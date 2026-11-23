package com.example.truyendex.features.novel;

import android.content.Context;
import android.database.Cursor;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.example.truyendex.data.local.AppDatabase;
import com.example.truyendex.data.repository.WalletRepository;

import java.util.function.IntConsumer;

public final class ChapterDropdown extends ListView {
    private ChapterAdapter chapters;

    public ChapterDropdown(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void toggle(AppDatabase db, WalletRepository wallet, int novelId,
                       int currentChapter, IntConsumer onSelect) {
        if (isShown()) {
            collapse();
            return;
        }
        Cursor cursor = db.chapterList(novelId);
        if (cursor.getCount() == 0) {
            cursor.close();
            Toast.makeText(getContext(), "Chưa có dữ liệu chương", Toast.LENGTH_SHORT).show();
            return;
        }
        chapters = new ChapterAdapter(getContext(), cursor, wallet, novelId);
        setAdapter(chapters);
        setVisibility(View.VISIBLE);
        int column = cursor.getColumnIndexOrThrow("chapter_number");
        while (cursor.moveToNext()) {
            if (cursor.getInt(column) == currentChapter) {
                setSelection(cursor.getPosition());
                break;
            }
        }
        setOnItemClickListener((parent, view, index, id) -> {
            Cursor item = (Cursor) parent.getItemAtPosition(index);
            int number = item.getInt(column);
            collapse();
            onSelect.accept(number);
        });
    }

    public void collapse() {
        setVisibility(View.GONE);
        if (chapters != null) {
            setAdapter(null);
            chapters.changeCursor(null);
            chapters = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        collapse();
        super.onDetachedFromWindow();
    }
}
