package com.example.truyendex.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class AppDatabaseBookmarkTest {
    @Test
    public void upgrade_preservesAndDeduplicatesBookmarks_andBothActionsUpdateTheList() {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String prefsName = "bookmark-test-" + UUID.randomUUID();
        Context context = new ContextWrapper(target) {
            @Override
            public Context getApplicationContext() {
                return this;
            }

            @Override
            public SharedPreferences getSharedPreferences(String name, int mode) {
                return super.getSharedPreferences(prefsName, mode);
            }
        };
        SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        prefs.edit().putString("records", "[{\"novelId\":1,\"bookmarks\":[3,5]}]").apply();

        try (SQLiteDatabase sqlite = SQLiteDatabase.create(null)) {
            AppDatabase db = new AppDatabase(context) {
                @Override
                public SQLiteDatabase getReadableDatabase() {
                    return sqlite;
                }

                @Override
                public SQLiteDatabase getWritableDatabase() {
                    return sqlite;
                }
            };
            db.onOpen(sqlite);
            assertTrue(db.toggleBookmark(1, 3));

            db.onUpgrade(sqlite, 2, 3);
            assertEquals(Arrays.asList(3, 5), db.getBookmarksByNovel().get(1));

            assertFalse(db.toggleBookmark(1, 3));
            assertEquals(Collections.singletonList(5), db.getBookmarksByNovel().get(1));

            db.removeBookmark(1, 5);
            assertFalse(db.isBookmarked(1, 5));
            assertTrue(db.getBookmarksByNovel().isEmpty());
        } finally {
            target.deleteSharedPreferences(prefsName);
        }
    }
}
