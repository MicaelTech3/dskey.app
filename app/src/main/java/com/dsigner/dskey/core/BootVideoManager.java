package com.dsigner.dskey.core;

import android.os.Environment;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class BootVideoManager {

    public static File getDir() {
        File dir = new File(
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                ),
                "bootVideo"
        );
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static File getVideo() {
        return new File(getDir(), "boot.mp4");
    }

    public static File getImage() {
        return new File(getDir(), "boot.jpg");
    }

    public static File getJson() {
        return new File(getDir(), "boot.json");
    }

    public static boolean hasVideo() {
        return getVideo().exists();
    }

    public static boolean hasImage() {
        return getImage().exists();
    }

    public static JSONObject getMeta() {
        try {
            if (!getJson().exists()) return null;
            FileInputStream fis = new FileInputStream(getJson());
            byte[] data = new byte[fis.available()];
            fis.read(data);
            fis.close();
            return new JSONObject(new String(data));
        } catch (Exception e) {
            return null;
        }
    }

    public static void saveMeta(String tipo, String url, String file) {
        try {
            JSONObject json = new JSONObject();
            json.put("tipo", tipo);
            json.put("url", url);
            json.put("file", file);
            json.put("timestamp", System.currentTimeMillis());

            FileOutputStream fos = new FileOutputStream(getJson());
            fos.write(json.toString(2).getBytes());
            fos.close();
        } catch (Exception ignored) {}
    }
}
