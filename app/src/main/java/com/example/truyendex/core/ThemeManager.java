package com.example.truyendex.core;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.example.truyendex.R;

public final class ThemeManager {

    public static final String LIGHT = "light";
    public static final String DARK = "dark";
    public static final String SEPIA = "sepia";

    private static final String PREFS_NAME = "truyendex-settings";
    private static final String THEME_KEY = "truyendex-theme";

    private ThemeManager() {}

    public static String getTheme(Context context) {
        return normalize(preferences(context).getString(THEME_KEY, LIGHT));
    }

    public static void setTheme(Activity activity, String theme) {
        String next = normalize(theme);
        if (next.equals(getTheme(activity))) {
            return;
        }
        preferences(activity).edit().putString(THEME_KEY, next).apply();
        activity.recreate();
    }

    public static void applyTheme(Activity activity) {
        int style;
        switch (getTheme(activity)) {
            case DARK:
                style = R.style.Theme_TruyenDex_Dark;
                break;
            case SEPIA:
                style = R.style.Theme_TruyenDex_Sepia;
                break;
            default:
                style = R.style.Theme_TruyenDex_Light;
        }
        activity.setTheme(style);
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static String normalize(String theme) {
        return DARK.equals(theme) || SEPIA.equals(theme) ? theme : LIGHT;
    }
}
