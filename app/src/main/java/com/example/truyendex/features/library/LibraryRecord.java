package com.example.truyendex.features.library;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LibraryRecord {
    public final int novelId;
    public int chapter;
    public int progress;
    public int unread;
    public boolean onShelf;
    public boolean reading;
    public boolean history;
    public final List<Integer> bookmarks;

    public LibraryRecord(int novelId, int chapter, int progress, int unread,
                         boolean onShelf, boolean reading, boolean history, List<Integer> bookmarks) {
        this.novelId = novelId;
        this.chapter = chapter;
        this.progress = progress;
        this.unread = unread;
        this.onShelf = onShelf;
        this.reading = reading;
        this.history = history;
        this.bookmarks = new ArrayList<>(bookmarks);
    }

    private static final String PREFS_NAME = "truyendex-library-v1";
    private static final String KEY_DATA = "records";

    public static List<LibraryRecord> load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_DATA, null);
        if (raw == null) {
            List<LibraryRecord> defaults = getDefaultRecords();
            save(context, defaults);
            return defaults;
        }
        try {
            JSONArray array = new JSONArray(raw);
            List<LibraryRecord> list = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                JSONArray bmArr = o.optJSONArray("bookmarks");
                List<Integer> bms = new ArrayList<>();
                if (bmArr != null) {
                    for (int j = 0; j < bmArr.length(); j++) {
                        bms.add(bmArr.getInt(j));
                    }
                }
                list.add(new LibraryRecord(
                        o.getInt("novelId"),
                        o.getInt("chapter"),
                        o.getInt("progress"),
                        o.getInt("unread"),
                        o.getBoolean("onShelf"),
                        o.getBoolean("reading"),
                        o.getBoolean("history"),
                        bms
                ));
            }
            return list;
        } catch (JSONException e) {
            return getDefaultRecords();
        }
    }

    public static void save(Context context, List<LibraryRecord> records) {
        JSONArray array = new JSONArray();
        try {
            for (LibraryRecord r : records) {
                JSONObject o = new JSONObject();
                o.put("novelId", r.novelId);
                o.put("chapter", r.chapter);
                o.put("progress", r.progress);
                o.put("unread", r.unread);
                o.put("onShelf", r.onShelf);
                o.put("reading", r.reading);
                o.put("history", r.history);
                JSONArray bmArr = new JSONArray();
                for (int ch : r.bookmarks) {
                    bmArr.put(ch);
                }
                o.put("bookmarks", bmArr);
                array.put(o);
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_DATA, array.toString())
                    .apply();
        } catch (JSONException ignored) {
        }
    }

    private static List<LibraryRecord> getDefaultRecords() {
        List<LibraryRecord> list = new ArrayList<>();
        list.add(new LibraryRecord(1, 12, 62, 6, true, true, true, Arrays.asList(2, 12)));
        list.add(new LibraryRecord(2, 8, 35, 3, true, true, true, Collections.singletonList(5)));
        list.add(new LibraryRecord(3, 18, 100, 0, true, false, true, new ArrayList<>()));
        return list;
    }
}