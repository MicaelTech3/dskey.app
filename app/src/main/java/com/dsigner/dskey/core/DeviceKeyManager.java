package com.dsigner.dskey.core;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;
import java.util.Random;

public class DeviceKeyManager {

    private static final String PREF = "dskey_prefs";
    private static final String KEY = "activation_key";

    public static String getOrCreate(Context c) {
        SharedPreferences sp = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String key = sp.getString(KEY, null);

        if (key == null) {
            key = generate();
            sp.edit().putString(KEY, key).apply();
        }
        return key;
    }

    private static String generate() {
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random r = new Random();

        return String.format(Locale.US, "%c%c%c-%03d",
                letters.charAt(r.nextInt(26)),
                letters.charAt(r.nextInt(26)),
                letters.charAt(r.nextInt(26)),
                r.nextInt(1000)
        );
    }
}