package com.example.truyendex.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.truyendex.data.model.Chapter;
import com.example.truyendex.data.model.Novel;
import com.example.truyendex.data.repository.NovelRepository;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// quan ly sqlite luu truyen, chuong va tien do doc
public class AppDatabase extends SQLiteOpenHelper {
    private static final String DB_NAME = "truyendex.db";
    private static final int DB_VERSION = 2;

    public AppDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        createTables(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            createTables(db);
        }
    }

    // tao cac bang neu chua co
    private void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS chapters (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "novel_id INTEGER, " +
                "chapter_number INTEGER, " +
                "volume_number INTEGER, " +
                "title TEXT, " +
                "content TEXT, " +
                "is_paid INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS reading_progress (" +
                "novel_id INTEGER PRIMARY KEY, " +
                "current_chapter INTEGER DEFAULT 1, " +
                "is_saved INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS read_chapters (" +
                "novel_id INTEGER, " +
                "chapter_number INTEGER, " +
                "PRIMARY KEY (novel_id, chapter_number))");
        db.execSQL("CREATE TABLE IF NOT EXISTS bookmarks (" +
                "novel_id INTEGER, " +
                "chapter_number INTEGER, " +
                "PRIMARY KEY (novel_id, chapter_number))");
        db.execSQL("CREATE TABLE IF NOT EXISTS novels (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, " +
                "author TEXT, " +
                "genres TEXT, " +
                "chapters INTEGER, " +
                "status TEXT, " +
                "type TEXT, " +
                "views INTEGER DEFAULT 0)");
    }

    // kiem tra neu chua co chuong thi tao du lieu mau
    public void initChapterIfEmpty(Context context, int novelId) {
        SQLiteDatabase db = getWritableDatabase();

        Cursor check = db.rawQuery("SELECT COUNT(*) FROM chapters WHERE novel_id = ?",
                new String[]{String.valueOf(novelId)});
        if (check != null) {
            if (check.moveToFirst() && check.getInt(0) > 0) {
                check.close();
                return;
            }
            check.close();
        }

        if (novelId == 1) {
            importKiemLaiIfEmpty(context, novelId);
        } else {
            generateSampleChapters(novelId);
        }
    }

    // sinh chuong mau cho cac truyen khac
    private void generateSampleChapters(int novelID) {
        SQLiteDatabase db = getWritableDatabase();
        Novel novel = NovelRepository.getInstance().getNovelById(novelID);
        int totalChapters = novel != null ? novel.getChapters() : 20;

        db.beginTransaction();
        try {
            for (int i = 1; i <= totalChapters; i++) {
                int vol = ((i - 1) / 10) + 1;
                String title = "Chương " + i + ": " + (novel != null ? novel.getName() : "Truyện") + " - Khởi đầu phần " + i;
                String content = "Đây là nội dung chương " + i + " của bộ truyện " + (novel != null ? novel.getName() : "") + ".\n\n" +
                        "Mặt trời dần buông xuống phía xa, mang theo những tia nắng rực rỡ cuối cùng của một ngày dài. " +
                        "Trong không gian tĩnh lặng, từng bước chân vang lên đều đặn trên con đường nhỏ.\n\n" +
                        "Cuộc hành trình mới chỉ bắt đầu, mở ra những thử thách và cơ hội mới phía trước. " +
                        "Những bí mật thâm sâu dần được hé lộ qua từng bước đường dấn thân vào thế giới rộng lớn...";
                boolean isPaid = (i >= 16); // tu chuong 16 tro di tinh phi
                insertChapter(db, novelID, i, vol, title, content, isPaid);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    // doc file kiem_lai.txt trong assets va luu vao db
    public void importKiemLaiIfEmpty(Context context, int novelID) {
        SQLiteDatabase db = getWritableDatabase();

        Cursor check = db.rawQuery("SELECT COUNT(*) FROM chapters WHERE novel_id = ?",
                new String[]{String.valueOf(novelID)});
        int count = 0;
        if (check != null) {
            if (check.moveToFirst()) {
                count = check.getInt(0);
            }
            check.close();
        }

        // neu da co chuong roi thi chi can cap nhat lai thong tin truyen
        if (count > 0) {
            saveNovelToDb(db, novelID, "Kiếm Lai", "Phong Hỏa Hí Chư Hầu",
                    "Fantasy,Phiêu lưu,Hành động", count, "Đang tiến hành", "Truyện dịch", 58000);
            return;
        }

        db.beginTransaction();
        try {
            InputStream is = context.getAssets().open("kiem_lai.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

            String line;
            int chapterNum = 0;
            String chapterTitle = "";
            StringBuilder content = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                // tach chuong theo dong bat dau va ket thuc bang ---
                if (trimmed.startsWith("---") && trimmed.endsWith("---") && trimmed.contains("Chương")) {
                    if (chapterNum > 0) {
                        int vol = ((chapterNum - 1) / 10) + 1;
                        insertChapter(db, novelID, chapterNum, vol, chapterTitle, content.toString().trim(), false);
                    }
                    chapterNum++;
                    chapterTitle = trimmed.replace("---", "")
                            .replace("Kiếm Lai - ", "")
                            .replace("Kiếm lai - ", "")
                            .trim();
                    content.setLength(0);
                    continue;
                }

                if (chapterNum == 0) continue;
                if (trimmed.startsWith("(第") && trimmed.endsWith(")")) continue;
                if (trimmed.startsWith("Kiếm Lai - Chương ")) continue;
                content.append(line).append("\n");
            }
            // luu chuong cuoi cung
            if (chapterNum > 0 && content.length() > 0) {
                int vol = ((chapterNum - 1) / 10) + 1;
                insertChapter(db, novelID, chapterNum, vol, chapterTitle, content.toString().trim(), false);
            }

            if (chapterNum > 0) {
                saveNovelToDb(db, novelID, "Kiếm Lai", "Phong Hỏa Hí Chư Hầu",
                        "Fantasy,Phiêu lưu,Hành động", chapterNum, "Đang tiến hành", "Truyện dịch", 58000);
            }

            reader.close();
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    // luu thong tin truyen vao bang novels
    public void saveNovelToDb(SQLiteDatabase db, int id, String name, String author, String genres,
                              int chapters, String status, String type, int views) {
        ContentValues cv = new ContentValues();
        cv.put("id", id);
        cv.put("name", name);
        cv.put("author", author);
        cv.put("genres", genres);
        cv.put("chapters", chapters);
        cv.put("status", status);
        cv.put("type", type);
        cv.put("views", views);
        db.insertWithOnConflict("novels", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private void insertChapter(SQLiteDatabase db, int novelID, int chapterNum, int volumeNumber, String title, String content, boolean isPaid) {
        ContentValues cv = new ContentValues();
        cv.put("novel_id", novelID);
        cv.put("chapter_number", chapterNum);
        cv.put("volume_number", volumeNumber);
        cv.put("title", title);
        cv.put("content", content);
        cv.put("is_paid", isPaid ? 1 : 0);
        db.insert("chapters", null, cv);
    }

    public Chapter getChapterByNumber(int novelId, int chapterNumber) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, novel_id, chapter_number, volume_number, title, content, is_paid " +
                        "FROM chapters WHERE novel_id = ? AND chapter_number = ?",
                new String[]{String.valueOf(novelId), String.valueOf(chapterNumber)}
        );
        Chapter chapter = null;
        if (c != null && c.moveToFirst()) {
            chapter = new Chapter();
            chapter.setId(c.getInt(0));
            chapter.setNovelId(c.getInt(1));
            chapter.setChapterNumber(c.getInt(2));
            chapter.setVolumeNumber(c.getInt(3));
            chapter.setTitle(c.getString(4));
            chapter.setContent(c.getString(5));
            chapter.setPaid(c.getInt(6) == 1);
            c.close();
        }
        return chapter;
    }

    public List<Chapter> getAllChapters(int novelId) {
        List<Chapter> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery( "SELECT id, novel_id, chapter_number, volume_number, title, content, is_paid " +
                        "FROM chapters WHERE novel_id = ? ORDER BY chapter_number ASC",
                new String[]{String.valueOf(novelId)}
        );
        if (c != null) {
            while (c.moveToNext()) {
                Chapter ch = new Chapter();
                ch.setId(c.getInt(0));
                ch.setNovelId(c.getInt(1));
                ch.setChapterNumber(c.getInt(2));
                ch.setVolumeNumber(c.getInt(3));
                ch.setTitle(c.getString(4));
                ch.setContent(c.getString(5));
                ch.setPaid(c.getInt(6) == 1);
                list.add(ch);
            }
            c.close();
        }
        return list;
    }

    public int getTotalChapters(int novelId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM chapters WHERE novel_id = ?",
                new String[]{String.valueOf(novelId)});
        int count = 0;
        if (c != null && c.moveToFirst()) {
            count = c.getInt(0);
            c.close();
        }
        return count;
    }
    public int getCurrentChapter(int novelId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT current_chapter FROM reading_progress WHERE novel_id = ?",
                new String[]{String.valueOf(novelId)});
        int ch = 1;
        if (c != null && c.moveToFirst()) {
            ch = c.getInt(0);
            c.close();
        }
        return ch;
    }
    public void saveCurrentChapter(int novelId, int chapterNumber) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("novel_id", novelId);
        cv.put("current_chapter", chapterNumber);
        db.insertWithOnConflict("reading_progress", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }
    public boolean isChapterRead(int novelId, int chapterNumber) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT 1 FROM read_chapters WHERE novel_id = ? AND chapter_number = ?",
                new String[]{String.valueOf(novelId), String.valueOf(chapterNumber)});
        boolean exists = (c != null && c.moveToFirst());
        if (c != null) c.close();
        return exists;
    }
    public void markChapterRead(int novelId, int chapterNumber) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("novel_id", novelId);
        cv.put("chapter_number", chapterNumber);
        db.insertWithOnConflict("read_chapters", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }
    public boolean isBookmarked(int novelId, int chapterNumber) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT 1 FROM bookmarks WHERE novel_id = ? AND chapter_number = ?",
                new String[]{String.valueOf(novelId), String.valueOf(chapterNumber)});
        boolean exists = (c != null && c.moveToFirst());
        if (c != null) c.close();
        return exists;
    }
    public void toggleBookmark(int novelId, int chapterNumber) {
        SQLiteDatabase db = getWritableDatabase();
        if (isBookmarked(novelId, chapterNumber)) {
            db.delete("bookmarks", "novel_id = ? AND chapter_number = ?",
                    new String[]{String.valueOf(novelId), String.valueOf(chapterNumber)});
        } else {
            ContentValues cv = new ContentValues();
            cv.put("novel_id", novelId);
            cv.put("chapter_number", chapterNumber);
            db.insert("bookmarks", null, cv);
        }
    }
    public List<Integer> getReadHistoryNovelIds() {
        List<Integer> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT novel_id FROM reading_progress ORDER BY novel_id DESC", null);
        if (c != null) {
            while (c.moveToNext()) {
                list.add(c.getInt(0));
            }
            c.close();
        }
        return list;
    }

    public List<Novel> getAllNovelsFromDb() {
        List<Novel> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, name, author, genres, chapters, status, type, views FROM novels", null);
        if (c != null) {
            while (c.moveToNext()) {
                int id = c.getInt(0);
                String name = c.getString(1);
                String author = c.getString(2);
                String genresStr = c.getString(3);
                int chapters = c.getInt(4);
                String status = c.getString(5);
                String type = c.getString(6);
                int views = c.getInt(7);
                List<String> genreList = Arrays.asList(genresStr.split(","));
                list.add(new Novel(
                        id, name, author, "", genreList, chapters, status, type, 0, 1, views, views, views
                ));
            }
            c.close();
        }
        return list;
    }

    public Novel getNovelByIdFromDb(int novelId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, name, author, genres, chapters, status, type, views FROM novels WHERE id = ?",
                new String[]{String.valueOf(novelId)});
        Novel novel = null;
        if (c != null) {
            if (c.moveToFirst()) {
                int id = c.getInt(0);
                String name = c.getString(1);
                String author = c.getString(2);
                String genresStr = c.getString(3);
                int chapters = c.getInt(4);
                String status = c.getString(5);
                String type = c.getString(6);
                int views = c.getInt(7);
                List<String> genreList = Arrays.asList(genresStr.split(","));
                novel = new Novel(
                        id, name, author, "", genreList, chapters, status, type, 0, 1, views, views, views
                );
            }
            c.close();
        }
        return novel;
    }

}
