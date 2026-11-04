package com.example.expensetracker.core.network.auth;

import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** In-memory SharedPreferences; only what AuthRepositoryImpl uses. */
class FakePrefs implements SharedPreferences {
    final Map<String, Object> map = new HashMap<>();

    @Override public Map<String, ?> getAll() { return map; }
    @Override public String getString(String k, String d) { return map.containsKey(k) ? (String) map.get(k) : d; }
    @Override public Set<String> getStringSet(String k, Set<String> d) { throw new UnsupportedOperationException(); }
    @Override public int getInt(String k, int d) { throw new UnsupportedOperationException(); }
    @Override public long getLong(String k, long d) { return map.containsKey(k) ? (Long) map.get(k) : d; }
    @Override public float getFloat(String k, float d) { throw new UnsupportedOperationException(); }
    @Override public boolean getBoolean(String k, boolean d) { throw new UnsupportedOperationException(); }
    @Override public boolean contains(String k) { return map.containsKey(k); }
    @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) { }
    @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) { }

    @Override
    public Editor edit() {
        return new Editor() {
            final Map<String, Object> pending = new HashMap<>();
            boolean clear;

            @Override public Editor putString(String k, String v) { pending.put(k, v); return this; }
            @Override public Editor putStringSet(String k, Set<String> v) { throw new UnsupportedOperationException(); }
            @Override public Editor putInt(String k, int v) { throw new UnsupportedOperationException(); }
            @Override public Editor putLong(String k, long v) { pending.put(k, v); return this; }
            @Override public Editor putFloat(String k, float v) { throw new UnsupportedOperationException(); }
            @Override public Editor putBoolean(String k, boolean v) { throw new UnsupportedOperationException(); }
            @Override public Editor remove(String k) { pending.put(k, null); return this; }
            @Override public Editor clear() { clear = true; return this; }
            @Override public boolean commit() { apply(); return true; }

            @Override
            public void apply() {
                if (clear) map.clear();
                for (Map.Entry<String, Object> e : pending.entrySet()) {
                    if (e.getValue() == null) map.remove(e.getKey()); else map.put(e.getKey(), e.getValue());
                }
            }
        };
    }
}
