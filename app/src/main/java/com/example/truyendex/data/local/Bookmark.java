package com.example.truyendex.data.local;

import androidx.room.Entity;

@Entity(tableName = "bookmarks", primaryKeys = {"novelId", "chapterNumber"})
public class Bookmark {
    public final int novelId;
    public final int chapterNumber;

    public Bookmark(int novelId, int chapterNumber) {
        this.novelId = novelId;
        this.chapterNumber = chapterNumber;
    }
}
