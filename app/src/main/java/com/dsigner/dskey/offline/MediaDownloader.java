package com.dsigner.dskey.offline;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.HttpURLConnection;

public class MediaDownloader {

    public static void download(String urlStr, File outFile) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.connect();

        InputStream is = c.getInputStream();
        FileOutputStream fos = new FileOutputStream(outFile);

        byte[] buffer = new byte[4096];
        int len;
        while ((len = is.read(buffer)) > 0) {
            fos.write(buffer, 0, len);
        }

        fos.close();
        is.close();
        c.disconnect();
    }
}
