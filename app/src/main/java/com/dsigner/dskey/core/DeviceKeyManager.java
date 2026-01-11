package com.dsigner.dskey.core;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Random;

public class DeviceKeyManager {

    private static final String PREF = "dskey_prefs";
    private static final String KEY = "device_key";
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    public static String getOrCreate(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String key = sp.getString(KEY, null);
        if (key != null) return key;

        key = generate();
        sp.edit().putString(KEY, key).apply();
        return key;
    }

    private static String generate() {
        Random r = new Random();
        return "" + CHARS.charAt(r.nextInt(CHARS.length())) +
                CHARS.charAt(r.nextInt(CHARS.length())) +
                CHARS.charAt(r.nextInt(CHARS.length())) +
                "-" +
                CHARS.charAt(r.nextInt(CHARS.length())) +
                CHARS.charAt(r.nextInt(CHARS.length())) +
                CHARS.charAt(r.nextInt(CHARS.length()));
    }
}
