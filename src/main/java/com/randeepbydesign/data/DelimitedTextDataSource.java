package com.randeepbydesign.data;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

@Slf4j
/**
 * A tab or comma separated text source
 */
public class DelimitedTextDataSource<T> extends DataSource<T> {

    private final File file;

    public enum DelimiterType {
        COMMA,
        TAB
    }

    private DelimiterType delimiterType = DelimiterType.TAB;

    private Function<String[], T> classMapper;

    private boolean skipHeader = false;

    public DelimitedTextDataSource(String filePath, DelimiterType delimiterType, Function<String[], T> classMapper) {
        this(filePath, delimiterType, classMapper, false);
    }

    public DelimitedTextDataSource(String filePath, DelimiterType delimiterType, Function<String[], T> classMapper,
            boolean skipHeader) {
        this.delimiterType = delimiterType;
        this.classMapper = classMapper;
        this.file = new File(filePath);
        this.skipHeader = skipHeader;
    }

    @Override
    public List<T> deserializeList(Class<T> classType) {
        List<T> returnList = new ArrayList<>();

        try {
            FileReader fileReader = new FileReader(file);
            CSVReaderBuilder csvReaderBuilder = new CSVReaderBuilder(fileReader).withCSVParser(
                    new CSVParserBuilder()
                            .withSeparator(getSeparator(delimiterType))
                            .build());
            CSVReader csvReader = csvReaderBuilder.build();
            if(skipHeader) {
                csvReader.readNext();
            }
            String[] lineRead = null;
            do {
                lineRead = csvReader.readNext();
                if (lineRead == null) {
                    break;
                }
                returnList.add(classMapper.apply(lineRead));
            } while (lineRead != null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        log.info("Returning {} converted objects", returnList.size());
        return returnList;
    }

    private char getSeparator(DelimiterType delimiterType) {
        switch (delimiterType) {

            case COMMA:
                return ',';
            case TAB:
                return '\t';
        }
        throw new RuntimeException("Missing delimiter");
    }

}
