package net.opendasharchive.openarchive.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import net.opendasharchive.openarchive.R

class ThemeHelper {
    companion object {
        fun setTheme(context: Context, theme: String)  {
            val mode = when(theme) {
                context.getString(R.string.prefs_theme_val_light) -> AppCompatDelegate.MODE_NIGHT_NO;
                context.getString(R.string.prefs_theme_val_dark) -> AppCompatDelegate.MODE_NIGHT_YES;
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            }
            AppCompatDelegate.setDefaultNightMode(mode);
        };
    }
}