package com.example.truyendex.data.model;

import androidx.annotation.DrawableRes;

import java.util.List;

public class Novel {
    private int id;
    private String name;
    private String author;
    private String artist;
    private List<String> genres;
    private int chapters;
    private String status;       // "Đang tiến hành" / "Hoàn thành"
    private String type;         // "Sáng tác" / "Truyện dịch" / "AI dịch"
    @DrawableRes
    private int coverImageResId;
    private int updated;
    private int weekViews;
    private int monthViews;
    private int allViews;

    public Novel() {}

    public Novel(int id, String name, String author, String artist,
                 List<String> genres, int chapters, String status,
                 String type, @DrawableRes int coverImageResId, int updated,
                 int weekViews, int monthViews, int allViews) {
        this.id = id;
        this.name = name;
        this.author = author;
        this.artist = artist;
        this.genres = genres;
        this.chapters = chapters;
        this.status = status;
        this.type = type;
        this.coverImageResId = coverImageResId;
        this.updated = updated;
        this.weekViews = weekViews;
        this.monthViews = monthViews;
        this.allViews = allViews;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres; }

    public int getChapters() { return chapters; }
    public void setChapters(int chapters) { this.chapters = chapters; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    @DrawableRes
    public int getCoverImageResId() { return coverImageResId; }
    public void setCoverImageResId(@DrawableRes int coverImageResId) {
        this.coverImageResId = coverImageResId;
    }

    public int getUpdated() { return updated; }
    public void setUpdated(int updated) { this.updated = updated; }

    public int getWeekViews() { return weekViews; }
    public void setWeekViews(int weekViews) { this.weekViews = weekViews; }

    public int getMonthViews() { return monthViews; }
    public void setMonthViews(int monthViews) { this.monthViews = monthViews; }

    public int getAllViews() { return allViews; }
    public void setAllViews(int allViews) { this.allViews = allViews; }

    public String getGenresText() {
        if (genres == null || genres.isEmpty()) return "";
        return String.join(" · ", genres);
    }

    public String getFormattedViews(String period) {
        int views;
        switch (period) {
            case "week": views = weekViews; break;
            case "month": views = monthViews; break;
            default: views = allViews; break;
        }
        if (views >= 1000) {
            return String.format("%.1fK", views / 1000.0);
        }
        return String.valueOf(views);
    }
}
