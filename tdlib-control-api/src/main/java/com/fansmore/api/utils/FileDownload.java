package com.fansmore.api.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Paths;

@Slf4j
public class FileDownload {
    public static File download(String downloadUrl) {
        String name = RandomUtils.getUUID();
        File file = Paths.get("/tmp/" + name).toFile();
        download(downloadUrl, file);
        return file;
    }

    public static void download(String downloadUrl, File file) {
        try {
            if (!file.exists()) {
                boolean newFile = file.createNewFile();
                if (!newFile) {
                    throw new RuntimeException("File download failed");
                }
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            URL url = new URL(downloadUrl);
            URLConnection connection = url.openConnection();
            InputStream inputStream = connection.getInputStream();
            int length;
            byte[] bytes = new byte[1024];
            while ((length = inputStream.read(bytes)) != -1) {
                fileOutputStream.write(bytes, 0, length);
            }
            fileOutputStream.close();
            inputStream.close();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
