package com.example.truyendex.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.truyendex.data.model.ReadingProgress;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class AppDatabaseReadingProgressTest {
    @Test
    public void upgradeAndRead_keepsProgressIndependentOfHistoryAndExcludesFinishedNovel() {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String databaseName = "reading-test-" + UUID.randomUUID() + ".db";
        Context isolated = new ContextWrapper(target) {
            @Override
            public File getDatabasePath(String name) {
                return super.getDatabasePath(databaseName);
            }
        };
        try {
            try (SQLiteDatabase oldDb = target.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)) {
                oldDb.execSQL("CREATE TABLE reading_progress (novel_id INTEGER PRIMARY KEY, " +
                        "current_chapter INTEGER DEFAULT 1, is_saved INTEGER DEFAULT 0)");
                oldDb.execSQL("CREATE TABLE reading_history (novel_id INTEGER PRIMARY KEY, last_opened INTEGER NOT NULL)");
                oldDb.execSQL("INSERT INTO reading_progress (novel_id, current_chapter) VALUES (8, 3), (9, 2)");
                oldDb.execSQL("INSERT INTO reading_history VALUES (8, 2000), (9, 1000)");
                oldDb.setVersion(4);
            }
            try (AppDatabase db = new AppDatabase(isolated)) {
                assertEquals(8, db.getLatestUnfinishedReading().novelId);
                assertEquals(3, db.getLatestUnfinishedReading().chapter);
                assertEquals(0, db.getLatestUnfinishedReading().chapterPercent);

                db.saveCurrentChapter(4, 1);
                db.saveReadingScroll(4, 1, 100, false);
                db.saveCurrentChapter(4, 2);
                db.saveReadingScroll(4, 2, 40, true);
                ReadingProgress active = db.getLatestUnfinishedReading();
                assertEquals(4, active.novelId);
                assertEquals(2, active.chapter);
                assertEquals(70, active.novelPercent(2));
                assertEquals(40, active.chapterPercent);
                assertFalse(active.finished);
                assertEquals(Arrays.asList(4, 8, 9), db.getReadHistoryNovelIds());
            }

            try (AppDatabase reopened = new AppDatabase(isolated)) {
                reopened.setOnShelf(4, true);
                reopened.removeFromHistory(4);
                assertEquals(Arrays.asList(8, 9), reopened.getReadHistoryNovelIds());
                assertEquals(4, reopened.getLatestUnfinishedReading().novelId);
                assertEquals(40, reopened.getLatestUnfinishedReading().chapterPercent);
                assertTrue(reopened.isOnShelf(4));

                reopened.saveCurrentChapter(4, 2);
                assertEquals(40, reopened.getReadingProgress(4).chapterPercent);
                reopened.saveReadingScroll(4, 2, 100, true);
                assertTrue(reopened.getReadingProgress(4).finished);
                assertEquals(8, reopened.getLatestUnfinishedReading().novelId);
                assertEquals(Integer.valueOf(4), reopened.getReadHistoryNovelIds().get(0));

                reopened.markReadingFinished(Arrays.asList(8, 9));
                reopened.clearReadingHistory();
                assertEquals(Collections.emptyList(), reopened.getReadHistoryNovelIds());
                assertEquals(2, reopened.getCurrentChapter(4));
            }
        } finally {
            target.deleteDatabase(databaseName);
        }
    }
}
