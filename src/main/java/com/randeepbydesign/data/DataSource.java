package com.randeepbydesign.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
/**
 *
 */
public abstract class DataSource<T> {

    public static <T> void serializeFile(String filePath, T objectInstance) {
        try {
            FileOutputStream fos = new FileOutputStream(new File(filePath));
            ObjectOutputStream o = new ObjectOutputStream(fos);
            o.writeObject(objectInstance);
            o.close();
            fos.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public abstract List<T> deserializeList(Class<T> classType);

    public static <T> T deserializeFile(File inFile) {
        try {
            FileInputStream fin = new FileInputStream(inFile);
            ObjectInputStream ois = new ObjectInputStream(fin);
            T retVal = (T) ois.readObject();

            log.debug("Object has been serialized");
            ois.close();
            fin.close();
            return retVal;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
