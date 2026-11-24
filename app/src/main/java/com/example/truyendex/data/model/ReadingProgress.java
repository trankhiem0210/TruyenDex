package com.example.truyendex.data.model;

public final class ReadingProgress {
    public final int novelId;
    public final int chapter;
    public final int chapterPercent;
    public final boolean finished;

    public ReadingProgress(int novelId, int chapter, int chapterPercent, boolean finished) {
        this.novelId = novelId;
        this.chapter = chapter;
        this.chapterPercent = chapterPercent;
        this.finished = finished;
    }

    public int novelPercent(int totalChapters) {
        if (finished) return 100;
        int total = Math.max(1, Math.max(chapter, totalChapters));
        long progress = ((chapter - 1L) * 100 + chapterPercent) / total;
        return (int) Math.max(0, Math.min(99, progress));
    }
}
