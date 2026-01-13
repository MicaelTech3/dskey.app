package com.dsigner.dskey.offline;

import android.util.Log;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class MediaDownloader {

    private static final String TAG = "DSKEY_DOWNLOADER";

    public static void download(String urlStr, File outFile) throws Exception {

        Log.d(TAG, "Download iniciado");
        Log.d(TAG, "URL: " + urlStr);
        Log.d(TAG, "Destino: " + outFile.getAbsolutePath());

        URL url = new URL(urlStr);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.connect();

        if (c.getResponseCode() != 200) {
            throw new IOException("HTTP " + c.getResponseCode());
        }

        InputStream in = c.getInputStream();
        FileOutputStream out = new FileOutputStream(outFile);

        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }

        out.flush();
        out.close();
        in.close();

        Log.d(TAG, "Download concluído com sucesso");
    }
}
