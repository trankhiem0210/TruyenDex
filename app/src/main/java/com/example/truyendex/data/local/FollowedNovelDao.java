package com.example.truyendex.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

@Dao
public abstract class FollowedNovelDao {
    @Query("SELECT EXISTS(SELECT 1 FROM followed_novels WHERE novelId = :novelId)")
    public abstract LiveData<Boolean> observeFollowed(int novelId);

    @Query("DELETE FROM followed_novels WHERE novelId = :novelId")
    abstract int delete(int novelId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract void insert(FollowedNovel novel);

    @Transaction
    public void toggle(int novelId) {
        if (delete(novelId) == 0) {
            insert(new FollowedNovel(novelId));
        }
    }
}
