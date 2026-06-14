package net.kdt.pojavlaunch.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import java.util.Map;

public class SettingsSaveManager {
    public static final String DRAFT_PREFS_NAME = "pojav_preferences_draft";
    private static boolean sInitialized = false;

    public static SharedPreferences getDraftPrefs(Context context) {
        return context.getSharedPreferences(DRAFT_PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void initDraft(Context context) {
        if (sInitialized) return;
        SharedPreferences mainPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences draftPrefs = getDraftPrefs(context);
        
        SharedPreferences.Editor editor = draftPrefs.edit();
        editor.clear();
        for (Map.Entry<String, ?> entry : mainPrefs.getAll().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            putInEditor(editor, key, value);
        }
        // Ensure auto-save is on by default so switch toggles persist immediately
        if (!draftPrefs.contains("auto_save_fallback")) {
            editor.putBoolean("auto_save_fallback", true);
        }
        editor.commit();
        sInitialized = true;
    }

    public static void saveToMain(Context context, String key) {
        SharedPreferences mainPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences draftPrefs = getDraftPrefs(context);
        SharedPreferences.Editor editor = mainPrefs.edit();
        
        if (draftPrefs.contains(key)) {
            Object value = draftPrefs.getAll().get(key);
            putInEditor(editor, key, value);
        } else {
            editor.remove(key);
        }
        editor.commit();
    }

    public static void commitChanges(Context context) {
        SharedPreferences mainPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences draftPrefs = getDraftPrefs(context);
        
        SharedPreferences.Editor editor = mainPrefs.edit();
        editor.clear();
        for (Map.Entry<String, ?> entry : draftPrefs.getAll().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            putInEditor(editor, key, value);
        }
        editor.commit();
    }

    public static boolean hasUnsavedChanges(Context context) {
        SharedPreferences mainPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences draftPrefs = getDraftPrefs(context);
        
        Map<String, ?> mainMap = mainPrefs.getAll();
        Map<String, ?> draftMap = draftPrefs.getAll();
        
        for (Map.Entry<String, ?> entry : draftMap.entrySet()) {
            String key = entry.getKey();
            Object draftValue = entry.getValue();
            if (!mainMap.containsKey(key)) {
                return true;
            }
            Object mainValue = mainMap.get(key);
            if (draftValue == null) {
                if (mainValue != null) return true;
            } else if (!draftValue.equals(mainValue)) {
                return true;
            }
        }
        
        for (String key : mainMap.keySet()) {
            if (!draftMap.containsKey(key)) {
                return true;
            }
        }
        
        return false;
    }

    private static void putInEditor(SharedPreferences.Editor editor, String key, Object value) {
        if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof Long) {
            editor.putLong(key, (Long) value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float) value);
        } else if (value instanceof String) {
            editor.putString(key, (String) value);
        }
    }
}
