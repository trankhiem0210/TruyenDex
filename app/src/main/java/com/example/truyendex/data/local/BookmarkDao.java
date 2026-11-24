package com.example.truyendex.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public abstract class BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY novelId, chapterNumber")
    public abstract LiveData<List<Bookmark>> observeAll();

    @Query("SELECT chapterNumber FROM bookmarks WHERE novelId = :novelId ORDER BY chapterNumber")
    public abstract LiveData<List<Integer>> observeChapters(int novelId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract void insert(Bookmark bookmark);

    @Query("DELETE FROM bookmarks WHERE novelId = :novelId AND chapterNumber = :chapter")
    public abstract int delete(int novelId, int chapter);

    @Transaction
    public boolean toggle(int novelId, int chapter) {
        if (delete(novelId, chapter) != 0) {
            return false;
        }
        insert(new Bookmark(novelId, chapter));
        return true;
    }
}
