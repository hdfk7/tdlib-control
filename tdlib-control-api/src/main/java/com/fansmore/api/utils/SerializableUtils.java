package com.fansmore.api.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.*;

@Slf4j
public class SerializableUtils {

    public static File toFile(Object object) {
        String fileName;
        synchronized (SerializableUtils.class) {
            fileName = "wa_scan_api_c_t_" + System.currentTimeMillis();
        }
        try {
            final File file = new File("/tmp/" + fileName);
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(object);
            oos.flush();
            oos.close();
            return file;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T toObject(File file, Class<T> cls) {
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        try {
            fis = new FileInputStream(file);
            ois = new ObjectInputStream(fis);
            final Object o = ois.readObject();
            return (T) o;
        } catch (IOException | ClassNotFoundException e) {
            log.error(e.getMessage(), e);
        } finally {
            try {
                if (ois != null) {
                    ois.close();
                }
                if (fis != null) {
                    fis.close();
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return null;
    }
}
