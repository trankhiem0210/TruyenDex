package com.example.truyendex.data.model;

public class Chapter {
    private int id;
    private int novelId;
    private int chapterNumber;
    private int volumeNumber;
    private String title;
    private String content;
    private boolean isPaid;

    public Chapter() {

    }
    public Chapter(boolean isPaid, String content, String title, int chapterNumber, int volumeNumber, int novelId, int id) {
        this.isPaid = isPaid;
        this.content = content;
        this.title = title;
        this.chapterNumber = chapterNumber;
        this.volumeNumber = volumeNumber;
        this.novelId = novelId;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getNovelId() {
        return novelId;
    }

    public void setNovelId(int novelId) {
        this.novelId = novelId;
    }

    public int getChapterNumber() {
        return chapterNumber;
    }

    public void setChapterNumber(int chapterNumber) {
        this.chapterNumber = chapterNumber;
    }

    public int getVolumeNumber() {
        return volumeNumber;
    }

    public void setVolumeNumber(int volumeNumber) {
        this.volumeNumber = volumeNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isPaid() {
        return isPaid;
    }

    public void setPaid(boolean paid) {
        isPaid = paid;
    }
}
