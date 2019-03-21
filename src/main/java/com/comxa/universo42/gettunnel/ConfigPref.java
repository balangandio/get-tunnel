package com.comxa.universo42.gettunnel;

import android.content.SharedPreferences;
import android.util.Log;

import com.comxa.universo42.embaralhador.Embaralhador;

public class ConfigPref {
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public ConfigPref(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public String getString(String key, String defaultValue) {
        String strValue = this.prefs.getString(key, defaultValue);

        if (strValue.equals(defaultValue)) {
            return defaultValue;
        }

        return decode(strValue);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.valueOf(getString(key, new Boolean(defaultValue).toString()));
    }

    public int getInt(String key, int defaultValue) {
        return Integer.valueOf(getString(key, String.valueOf(defaultValue)));
    }

    public void putString(String key, String value) {
        if (value != null) {
            value = encode(value);
        }
        getEditor().putString(key, value);
    }

    public void putBoolean(String key, boolean value) {
        putString(key, new Boolean(value).toString());
    }

    public void putInt(String key, int value) {
        putString(key, String.valueOf(value));
    }

    public void save() {
        getEditor().commit();
    }

    private SharedPreferences.Editor getEditor() {
        if (editor == null) {
            editor = this.prefs.edit();
        }
        return editor;
    }

    private String encode(String value) {
        return new String(Embaralhador.embaralhar(value.getBytes()));
    }

    private String decode(String value) {
        if (value.length() <= 7) {
            return "";
        } else {
            return new String(Embaralhador.desembaralhar(value.getBytes()));
        }
    }
}
