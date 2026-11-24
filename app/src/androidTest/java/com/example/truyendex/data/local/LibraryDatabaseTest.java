package com.example.truyendex.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class LibraryDatabaseTest {
    private final Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Test
    public void legacyBookmarks_importOnceAndToggleWithoutDuplicate() {
        String prefix = "bookmark-test-" + UUID.randomUUID() + "-";
        Context isolated = new ContextWrapper(target) {
            @Override
            public Context getApplicationContext() {
                return this;
            }

            @Override
            public File getDatabasePath(String name) {
                return super.getDatabasePath(prefix + name);
            }

            @Override
            public SharedPreferences getSharedPreferences(String name, int mode) {
                return super.getSharedPreferences(prefix + name, mode);
            }
        };
        String roomName = "library.db";
        try {
            try (AppDatabase legacy = new AppDatabase(isolated)) {
                assertTrue(legacy.toggleBookmark(4, 6));
                legacy.setOnShelf(4, true);
                legacy.saveCurrentChapter(4, 6);
                assertEquals(4, legacy.getMostRecentlyReadNovelId(1));
                legacy.removeFromHistory(4);
                assertTrue(legacy.getReadHistoryNovelIds().isEmpty());
                assertEquals(6, legacy.getCurrentChapter(4));
                assertTrue(legacy.isOnShelf(4));
            }
            LibraryDatabase room = LibraryDatabase.builder(isolated, roomName).build();
            try {
                SupportSQLiteDatabase sqlite = room.getOpenHelper().getWritableDatabase();
                assertEquals(1, count(sqlite, "SELECT COUNT(*) FROM bookmarks WHERE novelId = 4 AND chapterNumber = 6"));
                assertFalse(room.bookmarks().toggle(4, 6));
            } finally {
                room.close();
            }
            LibraryDatabase reopened = LibraryDatabase.builder(isolated, roomName).build();
            try {
                SupportSQLiteDatabase sqlite = reopened.getOpenHelper().getWritableDatabase();
                assertEquals(0, count(sqlite, "SELECT COUNT(*) FROM bookmarks"));
                assertTrue(reopened.bookmarks().toggle(4, 6));
                assertEquals(1, count(sqlite, "SELECT COUNT(*) FROM bookmarks"));
            } finally {
                reopened.close();
            }
        } finally {
            target.deleteDatabase(prefix + roomName);
            target.deleteDatabase(prefix + "truyendex.db");
        }
    }

    @Test
    public void migration_preservesBookmarksAndEnablesFollow() throws Exception {
        String name = "room-migration-" + UUID.randomUUID() + ".db";
        try {
            try (SQLiteDatabase versionOne = target.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)) {
                versionOne.execSQL("CREATE TABLE bookmarks (novelId INTEGER NOT NULL, " +
                        "chapterNumber INTEGER NOT NULL, PRIMARY KEY (novelId, chapterNumber))");
                versionOne.execSQL("INSERT INTO bookmarks (novelId, chapterNumber) VALUES (7, 11)");
                versionOne.setVersion(1);
            }
            LibraryDatabase room = LibraryDatabase.builder(target, name).build();
            try {
                SupportSQLiteDatabase sqlite = room.getOpenHelper().getWritableDatabase();
                assertEquals(2, sqlite.getVersion());
                assertEquals(1, count(sqlite, "SELECT COUNT(*) FROM bookmarks WHERE novelId = 7 AND chapterNumber = 11"));

                FutureTask<Void> follow = new FutureTask<>(() -> {
                    room.followedNovels().toggle(7);
                    return null;
                });
                room.getTransactionExecutor().execute(follow);
                follow.get(5, TimeUnit.SECONDS);
                assertEquals(1, count(sqlite, "SELECT COUNT(*) FROM followed_novels WHERE novelId = 7"));
                room.followedNovels().toggle(7);
                assertEquals(0, count(sqlite, "SELECT COUNT(*) FROM followed_novels"));
            } finally {
                room.close();
            }
        } finally {
            target.deleteDatabase(name);
        }
    }

    private int count(SupportSQLiteDatabase sqlite, String sql) {
        try (Cursor cursor = sqlite.query(sql)) {
            cursor.moveToFirst();
            return cursor.getInt(0);
        }
    }
}
