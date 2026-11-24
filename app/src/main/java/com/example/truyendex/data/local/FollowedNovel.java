package com.example.truyendex.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "followed_novels")
public class FollowedNovel {
    @PrimaryKey
    public final int novelId;

    public FollowedNovel(int novelId) {
        this.novelId = novelId;
    }
}
