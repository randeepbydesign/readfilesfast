package com.randeepbydesign.data;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import java.io.File;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonFileDataSource<T> extends DataSource<T> {

    private final File file;
    private ObjectMapper om;

    public JsonFileDataSource(String filePath) {
        try {
            file = new File(filePath);
        } catch (Exception e) {
            log.error("Couldn't load file", e);
            throw new RuntimeException(e);
        }
        om = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
    }


    @Override
    public List<T> deserializeList(Class<T> classType) {
        CollectionType type = om.getTypeFactory()
                .constructCollectionType(List.class, classType);
        try {
            return om.readValue(file, type);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

}
