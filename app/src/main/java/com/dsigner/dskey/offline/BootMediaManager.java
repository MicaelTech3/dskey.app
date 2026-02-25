package com.dsigner.dskey.offline;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;

public class BootMediaManager {

    private static final String TAG       = "DSKEY_BOOT";
    private static final String DIR_NAME  = "bootVideo";
    private static final String JSON_NAME = "media.json";

    public static File getBaseDir(Context ctx) {
        File dir = new File(ctx.getExternalFilesDir(null), DIR_NAME);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static File getMediaFile(Context ctx) {
        return new File(getBaseDir(ctx), "media.file");
    }

    public static File getPlaylistFile(Context ctx, int index) {
        return new File(getBaseDir(ctx), "playlist_" + index + ".file");
    }

    public static File getJsonFile(Context ctx) {
        return new File(getBaseDir(ctx), JSON_NAME);
    }

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

    /** Salva mídia única (image/gif/video/mov) */
    public static void save(Context ctx, String tipo, String url, File mediaFile) {
        try {
            JSONObject o = new JSONObject();
            o.put("tipo", tipo);
            o.put("url", url);
            o.put("path", mediaFile.getAbsolutePath());
            o.put("timestamp", System.currentTimeMillis());
            writeJson(ctx, o);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao salvar JSON", e);
        }
    }

    /** Salva playlist com itens */
    public static void savePlaylist(Context ctx, JSONArray items) {
        try {
            JSONObject o = new JSONObject();
            o.put("tipo", "playlist");
            o.put("items", items);
            o.put("timestamp", System.currentTimeMillis());
            writeJson(ctx, o);
            Log.d(TAG, "Playlist salva: " + items.length() + " itens");
        } catch (Exception e) {
            Log.e(TAG, "Erro ao salvar playlist", e);
        }
    }

    /** Salva estado de playlist vazia (para mostrar mensagem correta offline) */
    public static void saveEmptyPlaylist(Context ctx) {
        try {
            JSONObject o = new JSONObject();
            o.put("tipo", "empty_playlist");
            o.put("timestamp", System.currentTimeMillis());
            writeJson(ctx, o);
            Log.d(TAG, "Estado: playlist vazia salvo");
        } catch (Exception e) {
            Log.e(TAG, "Erro ao salvar estado vazio", e);
        }
    }

    public static void clear(Context ctx) {
        File dir = getBaseDir(ctx);
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) f.delete();
        Log.d(TAG, "Pasta bootVideo limpa");
    }

    private static void writeJson(Context ctx, JSONObject o) throws Exception {
        FileWriter fw = new FileWriter(getJsonFile(ctx));
        fw.write(o.toString());
        fw.close();
    }
}