package com.example.truyendex.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;

import com.example.truyendex.data.local.LibraryDatabase;

import java.util.concurrent.Executor;

public class FollowRepository {
    private final LibraryDatabase database;
    private final Executor main;

    public FollowRepository(Context context) {
        database = LibraryDatabase.getInstance(context);
        main = ContextCompat.getMainExecutor(context.getApplicationContext());
    }

    public LiveData<Boolean> observeFollowed(int novelId) {
        return database.followedNovels().observeFollowed(novelId);
    }

    public void toggle(int novelId, Runnable onError) {
        database.getTransactionExecutor().execute(() -> {
            try {
                database.followedNovels().toggle(novelId);
            } catch (RuntimeException e) {
                Log.e("FollowRepository", "Cannot update followed novel", e);
                main.execute(onError);
            }
        });
    }
}
