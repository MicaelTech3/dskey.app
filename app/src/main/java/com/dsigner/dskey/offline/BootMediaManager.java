package com.dsigner.dskey.offline;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.*;

public class BootMediaManager {

    private static final String TAG = "DSKEY_BOOT";
    private static final String DIR_NAME = "bootVideo";
    private static final String MEDIA_NAME = "media.file";
    private static final String JSON_NAME = "media.json";

    // 📂 Pasta base
    public static File getBaseDir(Context ctx) {
        File dir = new File(ctx.getExternalFilesDir(null), DIR_NAME);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            Log.d(TAG, "Criando pasta bootVideo: " + created);
        }
        return dir;
    }

    // 🎥 Arquivo da mídia (único)
    public static File getMediaFile(Context ctx) {
        return new File(getBaseDir(ctx), MEDIA_NAME);
    }

    // 📄 JSON
    public static File getJsonFile(Context ctx) {
        return new File(getBaseDir(ctx), JSON_NAME);
    }

    // 📖 Ler JSON
    public static JSONObject read(Context ctx) {
        try {
            File json = getJsonFile(ctx);
            if (!json.exists()) return null;

            BufferedReader br = new BufferedReader(new FileReader(json));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            return new JSONObject(sb.toString());

        } catch (Exception e) {
            Log.e(TAG, "Erro ao ler JSON", e);
            return null;
        }
    }

    // 💾 Salvar JSON
    public static void save(Context ctx, String tipo, String url, File mediaFile) {
        try {
            JSONObject o = new JSONObject();
            o.put("tipo", tipo);
            o.put("url", url);
            o.put("path", mediaFile.getAbsolutePath());
            o.put("timestamp", System.currentTimeMillis());

            FileWriter fw = new FileWriter(getJsonFile(ctx));
            fw.write(o.toString());
            fw.close();

            Log.d(TAG, "JSON salvo com sucesso");

        } catch (Exception e) {
            Log.e(TAG, "Erro ao salvar JSON", e);
        }
    }

    // 🧹 Limpar pasta (garante 1 mídia só)
    public static void clear(Context ctx) {
        File dir = getBaseDir(ctx);
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : files) {
            f.delete();
        }

        Log.d(TAG, "Pasta bootVideo limpa");
    }
}
