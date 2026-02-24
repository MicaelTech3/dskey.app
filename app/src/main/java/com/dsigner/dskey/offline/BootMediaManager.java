package com.dsigner.dskey.offline;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;

public class BootMediaManager {

    private static final String TAG       = "DSKEY_BOOT";
    private static final String DIR_NAME  = "bootVideo";
    private static final String MEDIA_NAME = "media.file";
    private static final String JSON_NAME  = "media.json";

    // ─── Pasta base ──────────────────────────────────────────────────────────
    public static File getBaseDir(Context ctx) {
        File dir = new File(ctx.getExternalFilesDir(null), DIR_NAME);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    // ─── Arquivo de mídia única ───────────────────────────────────────────────
    public static File getMediaFile(Context ctx) {
        return new File(getBaseDir(ctx), MEDIA_NAME);
    }

    // ─── Arquivo de item de playlist pelo índice ──────────────────────────────
    public static File getPlaylistFile(Context ctx, int index) {
        return new File(getBaseDir(ctx), "playlist_" + index + ".file");
    }

    // ─── JSON ─────────────────────────────────────────────────────────────────
    public static File getJsonFile(Context ctx) {
        return new File(getBaseDir(ctx), JSON_NAME);
    }

    // ─── Ler JSON salvo ───────────────────────────────────────────────────────
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

    // ─── Salvar mídia única ───────────────────────────────────────────────────
    public static void save(Context ctx, String tipo, String url, File mediaFile) {
        try {
            JSONObject o = new JSONObject();
            o.put("tipo", tipo);
            o.put("url", url);
            o.put("path", mediaFile.getAbsolutePath());
            o.put("timestamp", System.currentTimeMillis());

            writeJson(ctx, o.toString());
            Log.d(TAG, "JSON (única) salvo");
        } catch (Exception e) {
            Log.e(TAG, "Erro ao salvar JSON única", e);
        }
    }

    // ─── Salvar playlist ──────────────────────────────────────────────────────
    public static void savePlaylist(Context ctx, JSONArray items) {
        try {
            JSONObject o = new JSONObject();
            o.put("tipo", "playlist");
            o.put("items", items);
            o.put("timestamp", System.currentTimeMillis());

            writeJson(ctx, o.toString());
            Log.d(TAG, "JSON (playlist) salvo: " + items.length() + " itens");
        } catch (Exception e) {
            Log.e(TAG, "Erro ao salvar JSON playlist", e);
        }
    }

    // ─── Limpar toda a pasta ──────────────────────────────────────────────────
    public static void clearAll(Context ctx) {
        File dir = getBaseDir(ctx);
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) f.delete();
        Log.d(TAG, "Pasta bootVideo limpa");
    }

    /** @deprecated use clearAll */
    public static void clear(Context ctx) {
        clearAll(ctx);
    }

    // ─── Helper: escreve string no JSON ──────────────────────────────────────
    private static void writeJson(Context ctx, String content) throws IOException {
        FileWriter fw = new FileWriter(getJsonFile(ctx));
        fw.write(content);
        fw.close();
    }
}