package com.randeepbydesign.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import static com.randeepbydesign.util.Util.convertRowToList;
import static com.randeepbydesign.util.Util.hasData;
import static java.util.Optional.empty;
import static java.util.Optional.of;

@Slf4j
public class ExcelFileDataSource<T> extends DataSource<T> {

    List<Sheet> excelSheet;

    Function<List<Object[]>, Optional<T>> rowDataMapper;

    Function<Object[], Object> rowKeySupplier;

    /**
     * Takes a group of rows that are collected based on a key value. A row is defined as an Object array. In the event
     * the row group input is not valid an emptyOptional can be returned
     */
    public interface RowGroupMapperFunction<T> extends Function<List<Object[]>, Optional<T>> {

    }

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
            (List<Object[]> oa) -> oa.size() > 0 ? of(oa.get(0)) : empty();

    public static RowKeySupplier RowKeyIsCellAt(final int cellIndex) {
        return (Object[] oa) -> oa[cellIndex];
    }

    /**
     * @param rowDataMapper Maps a list of rows to a single data type
     * @param rowKeySupplier The way we identify how to group rows together
     */
    public ExcelFileDataSource(String filename, RowGroupMapperFunction<T> rowDataMapper,
            RowKeySupplier rowKeySupplier) throws Exception {
        this(filename, rowDataMapper, rowKeySupplier, w -> Collections.singletonList(w.getSheetAt(0)));
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
        if (true) {
            return deserializeSheetsList(classType);
        }
        return excelSheet.stream()
                .map(sheet -> deserializeList(sheet))
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }


    public List<T> deserializeSheetsList(Class<T> classType) {
        List<Object[]> allSheetsRows = excelSheet.stream()
                .map(sheet -> deserializeGenericList(sheet))
                .flatMap(List::stream)
                .collect(Collectors.toList());
        Collection<List<Object[]>> rowGroups = groupRows(allSheetsRows);
        return rowGroups.stream()
                .map(rowDataMapper)
                .filter(Optional::isPresent).map(Optional::get)
                .collect(Collectors.toList());
    }

    private List<Object[]> deserializeGenericList(Sheet sheet) {
        List<Object[]> rowsAsObject = new ArrayList<>();
        Iterator<Row> rowIterator = sheet.rowIterator();
        rowIterator.next();
        while (rowIterator.hasNext()) {
            rowsAsObject.add(convertRowToList(rowIterator.next()));
        }
        return rowsAsObject;
    }

    private List<T> deserializeList(Sheet sheet) {
        log.info("Processing sheet {}", sheet.getSheetName());
        List<Object[]> rowsAsObject = new ArrayList<>();
        Iterator<Row> rowIterator = sheet.rowIterator();
        rowIterator.next();
        while (rowIterator.hasNext()) {
            rowsAsObject.add(convertRowToList(rowIterator.next()));
        }
        Collection<List<Object[]>> rowGroups = groupRows(rowsAsObject);

        return rowGroups.stream()
                .map(rowDataMapper)
                .filter(Optional::isPresent).map(Optional::get)
                .collect(Collectors.toList());
    }

    private Collection<List<Object[]>> groupRows(List<Object[]> rowsAsObject) {
        return rowsAsObject.stream()
                .filter(Objects::nonNull)
                .filter(row -> hasData(rowKeySupplier.apply(row)))
                .collect(Collectors.groupingBy(rowKeySupplier))
                .values();
    }

    public void close() {
        try {
            excelSheet.get(0).getWorkbook().close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}

