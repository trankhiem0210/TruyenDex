package com.example.truyendex.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.truyendex.data.local.Bookmark;
import com.example.truyendex.data.local.BookmarkDao;
import com.example.truyendex.data.local.LibraryDatabase;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class BookmarkRepository {
    private static final ExecutorService WRITES = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private final BookmarkDao dao;

    public BookmarkRepository(Context context) {
        dao = LibraryDatabase.getInstance(context).bookmarks();
    }

    public LiveData<List<Bookmark>> observeAll() {
        return dao.observeAll();
    }

    public LiveData<List<Integer>> observeChapters(int novelId) {
        return dao.observeChapters(novelId);
    }

    public void toggle(int novelId, int chapter, Consumer<Boolean> onSuccess, Runnable onError) {
        write(() -> dao.toggle(novelId, chapter), onSuccess, onError);
    }

    public void remove(int novelId, int chapter, Runnable onSuccess, Runnable onError) {
        write(() -> dao.delete(novelId, chapter), deleted -> onSuccess.run(), onError);
    }

    private static <T> void write(Supplier<T> operation, Consumer<T> onSuccess, Runnable onError) {
        WRITES.execute(() -> {
            try {
                T result = operation.get();
                MAIN.post(() -> onSuccess.accept(result));
            } catch (RuntimeException e) {
                Log.e("BookmarkRepository", "Cannot update bookmark", e);
                MAIN.post(onError);
            }
        });
    }
}
