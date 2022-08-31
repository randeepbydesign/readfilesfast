package com.randeepbydesign.data.excel;

import com.randeepbydesign.data.DataSource;
import com.randeepbydesign.data.RowGroupMapperFunction;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import static com.randeepbydesign.util.Util.asString;
import static com.randeepbydesign.util.Util.convertRowToList;
import static com.randeepbydesign.util.Util.hasData;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;

@Slf4j
public class ExcelFileDataSource<T> extends DataSource<T> {

    private static final RowGroupMapperFunction<Map<String, String>> mapTransformer = (RowGroup singleRowGroup) -> {
        if (!singleRowGroup.getHeaderRow().isPresent()) {
            throw new RuntimeException("Headers required to render Map");
        }
        Object[] headers = singleRowGroup.getHeaderRow().get();
        return Optional.of(IntStream.rangeClosed(0, headers.length).boxed()
                .collect(Collectors.toMap(
                        i -> asString(headers[i]),
                        i -> asString(singleRowGroup.getGroupedRows().get(0)[i])))
        );
    };

    List<Sheet> excelSheet;

    RowGroupMapperFunction<T> rowDataMapper;

    Function<Object[], Object> rowKeySupplier;

    /**
     * A transformation to extract a grouping key from a Row. This can be used to map excel rows based on a common
     * value.
     */
    public interface RowKeySupplier extends Function<Object[], Object> {

    }

    public static final RowKeySupplier FirstCell = (Object[] oa) -> oa[0];

    private final static AtomicInteger counter = new AtomicInteger(0);

    public static final RowKeySupplier EveryRowUnique = oa -> counter.getAndIncrement();

    /**
     * Treats every row individually (so if a group is submitted just return 1st entry) otherwise empty
     */
    public static final RowGroupMapperFunction<Object[]> identity =
            oa -> oa.getGroupedRows().size() > 0 ? of(oa.getGroupedRows().get(0)) : empty();

    public static RowKeySupplier RowKeyIsCellAt(final int cellIndex) {
        return (Object[] oa) -> oa[cellIndex];
    }

    public ExcelFileDataSource(String filename) throws Exception {
        this(filename, mapTransformer, ExcelFileDataSource.EveryRowUnique);
    }

    /**
     * @param rowDataMapper Maps a list of rows to a single data type
     * @param rowKeySupplier The way we identify how to group rows together
     */
    public ExcelFileDataSource(String filename, RowGroupMapperFunction<T> rowDataMapper,
            RowKeySupplier rowKeySupplier) throws Exception {
        this(filename, rowDataMapper, rowKeySupplier, w -> singletonList(w.getSheetAt(0)));
    }

    public ExcelFileDataSource(String filename, RowGroupMapperFunction<T> rowDataMapper,
            RowKeySupplier rowKeySupplier, int... sheetIndices) throws Exception {
        this(filename, rowDataMapper, rowKeySupplier, w -> Arrays.stream(sheetIndices)
                .mapToObj(w::getSheetAt)
                .collect(Collectors.toList()));
    }

    public ExcelFileDataSource(String filename, RowGroupMapperFunction rowDataMapper,
            RowKeySupplier rowKeySupplier, String... sheetNames) throws Exception {
        this(filename, rowDataMapper, rowKeySupplier,
                workbook -> Arrays.stream(sheetNames)
                        .map(workbook::getSheet)
                        .collect(Collectors.toList()));
    }

    private ExcelFileDataSource(String filename, RowGroupMapperFunction<T> rowDataMapper,
            Function<Object[], Object> rowKeySupplier, Function<Workbook, List<Sheet>> sheetMapper) throws Exception {

        File file = new File(filename);
        FileInputStream fis = new FileInputStream(file);
        Workbook workbook = new XSSFWorkbook(fis);

        log.info("Workbook has {} sheets", workbook.getNumberOfSheets());
        Iterator<Sheet> sheetIterator = workbook.sheetIterator();
        while (sheetIterator.hasNext()) {
            Sheet sheet = sheetIterator.next();
            log.info("'{}' sheet with {} rows.First cell: {}", sheet.getSheetName(), sheet.getLastRowNum(),
                    sheet.getRow(0).getCell(0).getStringCellValue());
        }
        excelSheet = sheetMapper.apply(workbook);
        log.info("Recorded these sheets for processing: {}", excelSheet.stream()
                .map(Sheet::getSheetName)
                .collect(Collectors.joining(", ")));
        this.rowDataMapper = rowDataMapper;
        this.rowKeySupplier = rowKeySupplier;
        fis.close();
    }

    @Override
    public List<T> deserializeList(Class<T> classType) {
        return deserializeSheetsList(classType);
    }


    public List<T> deserializeSheetsList(Class<T> classType) {
        List<RowGroup> allSheetsRows = excelSheet.stream()
                .map(sheet -> deserializeGenericList(sheet))
                .collect(Collectors.toList());
        List<RowGroup> rowGroups = groupRows(allSheetsRows);
        return rowGroups.stream()
                .map(rowDataMapper::apply)
                .filter(Optional::isPresent).map(Optional::get)
                .collect(Collectors.toList());
    }

    /**
     * @return all the rows of the sheet grouped together
     */
    private RowGroup deserializeGenericList(Sheet sheet) {
        List<Object[]> rowsAsObject = new ArrayList<>();
        Iterator<Row> rowIterator = sheet.rowIterator();
        Object[] headerRow = convertRowToList(rowIterator.next());
        while (rowIterator.hasNext()) {
            rowsAsObject.add(convertRowToList(rowIterator.next()));
        }
        return new RowGroup(headerRow, rowsAsObject);
    }

    protected List<RowGroup> groupRows(List<RowGroup> sheets) {
        if (rowKeySupplier == null || sheets.isEmpty()) {
            return sheets;
        }
        Optional<Object[]> header = sheets.get(0).getHeaderRow();
        return sheets.stream()
                .map(RowGroup::getGroupedRows).flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .filter(row -> {
                    try {
                        return hasData(rowKeySupplier.apply(row));
                    } catch (Exception e) {
                        log.error("Unable to process row", e);
                        return false;
                    }
                })
                .collect(Collectors.groupingBy(rowKeySupplier))
                .values().stream()
                .map(la -> new RowGroup(header.orElse(null), la))
                .collect(Collectors.toList());
    }

    public void close() {
        try {
            excelSheet.get(0).getWorkbook().close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}

