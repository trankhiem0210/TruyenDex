package com.example.truyendex.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Bookmark.class, FollowedNovel.class}, version = 2, exportSchema = false)
public abstract class LibraryDatabase extends RoomDatabase {
    private static LibraryDatabase instance;
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS followed_novels (novelId INTEGER NOT NULL PRIMARY KEY)");
        }
    };

    public abstract BookmarkDao bookmarks();
    public abstract FollowedNovelDao followedNovels();

    public static synchronized LibraryDatabase getInstance(Context context) {
        if (instance == null) {
            Context appContext = context.getApplicationContext();
            instance = builder(appContext, "truyendex-library.db").build();
        }
        return instance;
    }

    static Builder<LibraryDatabase> builder(Context context, String name) {
        return Room.databaseBuilder(context, LibraryDatabase.class, name)
                .addMigrations(MIGRATION_1_2)
                .addCallback(new Callback() {
                    @Override
                    public void onCreate(@NonNull SupportSQLiteDatabase db) {
                        importBookmarks(context, db);
                    }
                });
    }

    private static void importBookmarks(Context context, SupportSQLiteDatabase db) {
        try (AppDatabase legacyDb = new AppDatabase(context)) {
            legacyDb.getBookmarksByNovel().forEach((novelId, chapters) -> {
                for (int chapter : chapters) {
                    db.execSQL("INSERT OR IGNORE INTO bookmarks (novelId, chapterNumber) VALUES (?, ?)",
                            new Object[]{novelId, chapter});
                }
            });
        }
    }
}
