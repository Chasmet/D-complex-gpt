package com.chasmet.dcomplexgpt;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppPrefs {
    private static final String FILE = "dcomplex_prefs";
    private static final String KEY_AUTO_UPDATE = "auto_update";

    private final SharedPreferences prefs;

    public AppPrefs(Context context) {
        prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public boolean isAutoUpdateEnabled() {
        return prefs.getBoolean(KEY_AUTO_UPDATE, true);
    }

    public void setAutoUpdateEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_UPDATE, enabled).apply();
    }
}
