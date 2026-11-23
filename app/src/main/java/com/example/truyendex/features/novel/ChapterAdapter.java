package com.example.truyendex.features.novel;

import android.content.Context;
import android.database.Cursor;
import android.view.ContextThemeWrapper;
import android.widget.SimpleCursorAdapter;

import com.example.truyendex.R;
import com.example.truyendex.core.ThemeManager;
import com.example.truyendex.data.repository.WalletRepository;

final class ChapterAdapter extends SimpleCursorAdapter {
    ChapterAdapter(Context context, Cursor cursor, WalletRepository wallet, int novel) {
        super(new ContextThemeWrapper(context, ThemeManager.styleFor(context)), R.layout.item_chapter, cursor,
                new String[]{"title", "is_paid"}, new int[]{android.R.id.text1, android.R.id.text2}, 0);
        setViewBinder((view, row, index) -> {
            if (view.getId() != android.R.id.text2) return false;
            int chapter = row.getInt(row.getColumnIndexOrThrow("chapter_number"));
            boolean paid = row.getInt(index) != 0;
            String label = paid ? "Đã mở khóa" : "Miễn phí";
            if (!wallet.isUnlocked(novel, chapter, paid)) label = "Mở chương · 10 xu";
            ((android.widget.TextView) view).setText(label);
            return true;
        });
    }
}
