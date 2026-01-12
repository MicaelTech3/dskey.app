package com.dsigner.dskey.offline;

import android.content.Context;
import android.os.Environment;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;

public class BootMediaManager {

    private static final String JSON_NAME = "boot_media.json";
    private static final String MEDIA_NAME = "boot_media";

    // 📂 /Download/bootVideo
    public static File getBaseDir(Context c) {
        File dir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "bootVideo"
        );
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static File getMediaFile(Context c) {
        return new File(getBaseDir(c), MEDIA_NAME);
    }

    public static File getJsonFile(Context c) {
        return new File(c.getFilesDir(), JSON_NAME);
    }

    // 🔥 LÊ JSON
    public static JSONObject read(Context c) {
        try {
            File f = getJsonFile(c);
            if (!f.exists()) return null;

            FileInputStream fis = new FileInputStream(f);
            byte[] data = new byte[(int) f.length()];
            fis.read(data);
            fis.close();

            return new JSONObject(new String(data));
        } catch (Exception e) {
            return null;
        }
    }

    // 💾 SALVA JSON
    public static void save(Context c, String tipo, String url) {
        try {
            JSONObject o = new JSONObject();
            o.put("tipo", tipo);
            o.put("url", url);

            FileOutputStream fos = new FileOutputStream(getJsonFile(c));
            fos.write(o.toString().getBytes());
            fos.close();
        } catch (Exception ignored) {}
    }

    // 🧹 LIMPA MÍDIA ANTIGA
    public static void clearMedia(Context c) {
        File f = getMediaFile(c);
        if (f.exists()) f.delete();
    }
}
