package com.example.truyendex.features.novel;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.truyendex.R;
import com.example.truyendex.core.ThemeManager;
import com.example.truyendex.core.WindowInsetsHelper;
import com.example.truyendex.data.local.AppDatabase;
import com.example.truyendex.data.repository.WalletRepository;
import com.example.truyendex.features.reader.ReaderActivity;

public final class ChaptersActivity extends AppCompatActivity {
    private AppDatabase db;
    private ChapterAdapter adapter;

    @Override
    protected void onCreate(Bundle state) {
        ThemeManager.applyTheme(this);
        super.onCreate(state);
        setContentView(R.layout.activity_chapters);
        WindowInsetsHelper.applySystemBarsPadding(findViewById(R.id.chaptersRoot));
        findViewById(R.id.chaptersBack).setOnClickListener(v -> finish());
        int novel = getIntent().getIntExtra("novel_id", 1);
        db = new AppDatabase(this);
        db.initChapterIfEmpty(this, novel);
        Cursor chapters = db.chapterList(novel);
        adapter = new ChapterAdapter(this, chapters, new WalletRepository(this), novel);
        ListView list = findViewById(R.id.chapterList);
        list.setAdapter(adapter);
        list.setEmptyView(findViewById(R.id.chaptersEmpty));
        list.setOnItemClickListener((parent, row, index, id) -> {
            Cursor item = (Cursor) adapter.getItem(index);
            int number = item.getInt(item.getColumnIndexOrThrow("chapter_number"));
            Intent selection = new Intent().putExtra("chapter_number", number);
            if (getIntent().getBooleanExtra("select_chapter", false)) {
                setResult(RESULT_OK, selection);
            } else {
                startActivity(new Intent(this, ReaderActivity.class)
                        .putExtra("novel_id", novel).putExtra("chapter_number", number));
            }
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        adapter.changeCursor(null);
        db.close();
        super.onDestroy();
    }
}
