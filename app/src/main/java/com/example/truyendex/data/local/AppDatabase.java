package com.example.truyendex.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.truyendex.data.model.Chapter;
import com.example.truyendex.data.model.Novel;
import com.example.truyendex.data.model.ReadingProgress;
import com.example.truyendex.data.repository.NovelRepository;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// quan ly sqlite luu truyen, chuong va tien do doc
public class AppDatabase extends SQLiteOpenHelper {
    private static final String DB_NAME = "truyendex.db";
    private static final int DB_VERSION = 5;
    private static final String READING_PROGRESS_COLUMNS =
            "SELECT novel_id, current_chapter, chapter_percent, finished FROM reading_progress";
    private static final String READING_PROGRESS_ORDER = " ORDER BY updated_at DESC, novel_id DESC";
    private final Context context;

    public AppDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
        importLegacyLibraryBookmarks(db);
        importLibraryState(db);
        upgradeReadingProgress(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        createTables(db);
        if (oldVersion < 3) {
            importLegacyLibraryBookmarks(db);
        }
        if (oldVersion < 4 && newVersion >= 4) {
            importLibraryState(db);
        }
        if (oldVersion < 5 && newVersion >= 5) {
            upgradeReadingProgress(db);
        }
    }

    private void upgradeReadingProgress(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE reading_progress ADD COLUMN chapter_percent INTEGER NOT NULL DEFAULT 0");
        db.execSQL("ALTER TABLE reading_progress ADD COLUMN finished INTEGER NOT NULL DEFAULT 0");
        db.execSQL("ALTER TABLE reading_progress ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0");
        db.execSQL("UPDATE reading_progress SET updated_at = COALESCE((" +
                "SELECT last_opened FROM reading_history " +
                "WHERE reading_history.novel_id = reading_progress.novel_id), 0)");
    }

    private JSONArray readLegacyLibraryRecords() {
        String records = context.getSharedPreferences("truyendex-library-v1", Context.MODE_PRIVATE)
                .getString("records", null);
        try {
            return records == null ? new JSONArray() : new JSONArray(records);
        } catch (JSONException e) {
            Log.w("AppDatabase", "Cannot read legacy library records", e);
            return new JSONArray();
        }
    }

    private void importLegacyLibraryBookmarks(SQLiteDatabase db) {
        JSONArray library = readLegacyLibraryRecords();
        for (int i = 0; i < library.length(); i++) {
            JSONObject record = library.optJSONObject(i);
            if (record == null) continue;
            int novelId = record.optInt("novelId");
            JSONArray bookmarks = record.optJSONArray("bookmarks");
            if (novelId <= 0 || bookmarks == null) continue;
            for (int j = 0; j < bookmarks.length(); j++) {
                int chapter = bookmarks.optInt(j);
                if (chapter > 0) insertBookmark(db, novelId, chapter);
            }
        }
    }

    private void importLibraryState(SQLiteDatabase db) {
        db.execSQL("INSERT OR IGNORE INTO reading_history (novel_id, last_opened) " +
                "SELECT novel_id, 0 FROM reading_progress");
        JSONArray library = readLegacyLibraryRecords();
        for (int i = 0; i < library.length(); i++) {
            JSONObject record = library.optJSONObject(i);
            if (record == null || record.optInt("novelId") <= 0) continue;
            int novelId = record.optInt("novelId");
            if (record.optBoolean("onShelf")) {
                db.execSQL("INSERT OR IGNORE INTO library_shelf (novel_id) VALUES (?)", new Object[]{novelId});
            }
            if (record.optBoolean("history")) {
                db.execSQL("INSERT OR IGNORE INTO reading_history (novel_id, last_opened) VALUES (?, 0)",
                        new Object[]{novelId});
            }
        }
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
        db.execSQL("CREATE TABLE IF NOT EXISTS library_shelf (novel_id INTEGER PRIMARY KEY)");
        db.execSQL("CREATE TABLE IF NOT EXISTS reading_history (" +
                "novel_id INTEGER PRIMARY KEY, last_opened INTEGER NOT NULL)");
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
            try {
                if (Arrays.asList(context.getAssets().list("")).contains("kiem_lai.txt")) {
                    importKiemLaiIfEmpty(context, novelId);
                    return;
                }
            } catch (java.io.IOException e) {
                android.util.Log.w("AppDatabase", "Cannot list bundled chapters", e);
            }
        }
        generateSampleChapters(novelId);
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

    public Cursor chapterList(int novelId) {
        return getReadableDatabase().rawQuery(
                "SELECT id AS _id, title, chapter_number, is_paid FROM chapters WHERE novel_id=? ORDER BY chapter_number",
                new String[]{String.valueOf(novelId)});
    }

    public boolean hasPaidBundle(int novelId) {
        try (Cursor rows = getReadableDatabase().rawQuery(
                "SELECT COUNT(DISTINCT chapter_number) FROM chapters WHERE novel_id=? AND chapter_number BETWEEN 16 AND 18 AND is_paid=1",
                new String[]{String.valueOf(novelId)})) {
            return rows.moveToFirst() && rows.getInt(0) == 3;
        }
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
        try (Cursor c = db.rawQuery("SELECT current_chapter FROM reading_progress WHERE novel_id = ?",
                new String[]{String.valueOf(novelId)})) {
            return c.moveToFirst() ? c.getInt(0) : 1;
        }
    }
    public void saveCurrentChapter(int novelId, int chapterNumber) {
        SQLiteDatabase db = getWritableDatabase();
        long now = System.currentTimeMillis();
        db.beginTransaction();
        try {
            db.execSQL("INSERT OR IGNORE INTO reading_progress (novel_id, current_chapter) VALUES (?, ?)",
                    new Object[]{novelId, chapterNumber});
            db.execSQL("UPDATE reading_progress SET " +
                            "chapter_percent = CASE WHEN current_chapter = ? THEN chapter_percent ELSE 0 END, " +
                            "current_chapter = ?, updated_at = ? WHERE novel_id = ?",
                    new Object[]{chapterNumber, chapterNumber, now, novelId});
            db.execSQL("INSERT OR REPLACE INTO reading_history (novel_id, last_opened) VALUES (?, ?)",
                    new Object[]{novelId, now});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void saveReadingScroll(int novelId, int chapterNumber, int percent, boolean lastChapter) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("UPDATE reading_progress SET chapter_percent = ?, updated_at = ?, " +
                        "finished = CASE WHEN ? THEN 1 ELSE finished END " +
                        "WHERE novel_id = ? AND current_chapter = ?",
                new Object[]{percent, System.currentTimeMillis(), lastChapter && percent == 100 ? 1 : 0,
                        novelId, chapterNumber});
    }

    public void markReadingFinished(List<Integer> novelIds) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (int novelId : novelIds) {
                db.execSQL("UPDATE reading_progress SET finished = 1 WHERE novel_id = ?", new Object[]{novelId});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public ReadingProgress getReadingProgress(int novelId) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                READING_PROGRESS_COLUMNS + " WHERE novel_id = ?", new String[]{String.valueOf(novelId)})) {
            return cursor.moveToFirst() ? readReadingProgress(cursor) : null;
        }
    }

    public Map<Integer, ReadingProgress> getReadingProgressByNovel() {
        Map<Integer, ReadingProgress> progressByNovel = new LinkedHashMap<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                READING_PROGRESS_COLUMNS + READING_PROGRESS_ORDER, null)) {
            while (cursor.moveToNext()) {
                ReadingProgress progress = readReadingProgress(cursor);
                progressByNovel.put(progress.novelId, progress);
            }
        }
        return progressByNovel;
    }

    private ReadingProgress readReadingProgress(Cursor cursor) {
        return new ReadingProgress(cursor.getInt(0), cursor.getInt(1), cursor.getInt(2), cursor.getInt(3) != 0);
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
    public boolean toggleBookmark(int novelId, int chapterNumber) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            boolean bookmarked = deleteBookmark(db, novelId, chapterNumber) == 0;
            if (bookmarked) {
                insertBookmark(db, novelId, chapterNumber);
            }
            db.setTransactionSuccessful();
            return bookmarked;
        } finally {
            db.endTransaction();
        }
    }

    public void removeBookmark(int novelId, int chapterNumber) {
        deleteBookmark(getWritableDatabase(), novelId, chapterNumber);
    }

    private int deleteBookmark(SQLiteDatabase db, int novelId, int chapterNumber) {
        return db.delete("bookmarks", "novel_id = ? AND chapter_number = ?",
                new String[]{String.valueOf(novelId), String.valueOf(chapterNumber)});
    }

    private void insertBookmark(SQLiteDatabase db, int novelId, int chapterNumber) {
        ContentValues values = new ContentValues();
        values.put("novel_id", novelId);
        values.put("chapter_number", chapterNumber);
        db.insertWithOnConflict("bookmarks", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public Map<Integer, List<Integer>> getBookmarksByNovel() {
        Map<Integer, List<Integer>> bookmarks = new LinkedHashMap<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT novel_id, chapter_number FROM bookmarks ORDER BY novel_id, chapter_number", null)) {
            while (cursor.moveToNext()) {
                bookmarks.computeIfAbsent(cursor.getInt(0), id -> new ArrayList<>()).add(cursor.getInt(1));
            }
        }
        return bookmarks;
    }

    public List<Integer> getReadHistoryNovelIds() {
        return queryNovelIds("SELECT novel_id FROM reading_history ORDER BY last_opened DESC, novel_id DESC");
    }

    public int getMostRecentlyReadNovelId(int fallbackNovelId) {
        ReadingProgress progress = getLatestUnfinishedReading();
        return progress == null ? fallbackNovelId : progress.novelId;
    }

    public ReadingProgress getLatestUnfinishedReading() {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                READING_PROGRESS_COLUMNS + " WHERE finished = 0 " +
                        "AND novel_id IN (SELECT novel_id FROM reading_history)" +
                        READING_PROGRESS_ORDER + " LIMIT 1", null)) {
            return cursor.moveToFirst() ? readReadingProgress(cursor) : null;
        }
    }

    public List<Integer> getShelfNovelIds() {
        return queryNovelIds("SELECT novel_id FROM library_shelf ORDER BY novel_id");
    }

    public boolean isOnShelf(int novelId) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT 1 FROM library_shelf WHERE novel_id = ?", new String[]{String.valueOf(novelId)})) {
            return cursor.moveToFirst();
        }
    }

    public void setOnShelf(int novelId, boolean onShelf) {
        SQLiteDatabase db = getWritableDatabase();
        if (onShelf) {
            db.execSQL("INSERT OR IGNORE INTO library_shelf (novel_id) VALUES (?)", new Object[]{novelId});
        } else {
            db.delete("library_shelf", "novel_id = ?", new String[]{String.valueOf(novelId)});
        }
    }

    public void removeFromHistory(int novelId) {
        getWritableDatabase().delete("reading_history", "novel_id = ?",
                new String[]{String.valueOf(novelId)});
    }

    public void clearReadingHistory() {
        getWritableDatabase().delete("reading_history", null, null);
    }

    private List<Integer> queryNovelIds(String sql) {
        List<Integer> list = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                list.add(cursor.getInt(0));
            }
        }
        return list;
    }

    public List<Novel> getAllNovelsFromDb() {
        List<Novel> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, name, author, genres, chapters, status, type, views FROM novels", null);
        if (c != null) {
            while (c.moveToNext()) {
                list.add(readNovel(c));
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
                novel = readNovel(c);
            }
            c.close();
        }
        return novel;
    }

    private Novel readNovel(Cursor cursor) {
        int id = cursor.getInt(0);
        String name = cursor.getString(1);
        String author = cursor.getString(2);
        List<String> genres = Arrays.asList(cursor.getString(3).split(","));
        int chapters = cursor.getInt(4);
        String status = cursor.getString(5);
        String type = cursor.getString(6);
        int views = cursor.getInt(7);
        int coverResId = NovelRepository.getInstance().getSampleCoverImageResId(name);
        return new Novel(id, name, author, "", genres, chapters, status, type,
                coverResId, 1, views, views, views);
    }

}
