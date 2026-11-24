package com.example.truyendex.features.reader;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.OneShotPreDrawListener;
import com.google.android.material.button.MaterialButton;

import com.example.truyendex.R;
import com.example.truyendex.core.ThemeManager;
import com.example.truyendex.data.repository.WalletRepository;
import com.example.truyendex.data.repository.BookmarkRepository;
import com.example.truyendex.features.wallet.WalletBottomSheet;
import com.example.truyendex.data.local.AppDatabase;
import com.example.truyendex.data.model.Chapter;
import com.example.truyendex.data.model.Novel;
import com.example.truyendex.data.model.ReadingProgress;
import com.example.truyendex.data.repository.NovelRepository;
import com.example.truyendex.features.novel.ChapterDropdown;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ReaderActivity extends AppCompatActivity {

    private int novelId = 1;
    private int currentChapter = 1;
    private int totalChapters = 18;
    private boolean toolsVisible = true;
    private boolean canTrackScroll;
    private int chapterPercent;
    private int savedChapterPercent;
    private OneShotPreDrawListener scrollRestore;

    private AppDatabase db;
    private BookmarkRepository bookmarks;
    private List<Integer> bookmarkedChapters = Collections.emptyList();
    private WalletRepository wallet;
    private SharedPreferences prefs;

    private ScrollView scrollReader;
    private LinearLayout readerContentBox, proseContainer, paywallBox, readerTopBar, readerBottomBar, ttsPlayerBar;
    private TextView tvChapterLabel, tvReaderTitle, tvReaderMeta, tvTopNovelName, tvTopChapter;
    private TextView tvPaywallBalance, tvEndChapterNote, tvProgressCaption, tvReadPercent, tvTtsStatus;
    private Button btnReaderWallet, btnShowTools, btnPrevChap, btnNextChap, btnEndNextChapter, btnBookmark, btnTtsPlayPause, btnTtsRate;

    private TtsController tts;
    private final List<String> paragraphs = new ArrayList<>();
    private final List<TextView> paragraphViews = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_reader);
        com.example.truyendex.core.WindowInsetsHelper.applySystemBarsPadding(findViewById(R.id.readerRoot));

        novelId = getIntent().getIntExtra("novel_id", 1);
        db = new AppDatabase(this);
        db.initChapterIfEmpty(this, novelId);
        wallet = new WalletRepository(this);
        prefs = getSharedPreferences("truyendex_prefs", MODE_PRIVATE);

        currentChapter = getIntent().getIntExtra("chapter_number", db.getCurrentChapter(novelId));

        int dbCount = db.getTotalChapters(novelId);
        if (dbCount > 0) {
            totalChapters = dbCount;
        } else {
            Novel novel = db.getNovelByIdFromDb(novelId);
            if (novel == null) {
                novel = NovelRepository.getInstance().getNovelById(novelId);
            }
            if (novel != null) totalChapters = novel.getChapters();
        }

        getSupportFragmentManager().setFragmentResultListener(WalletBottomSheet.UPDATED, this,
                (key, result) -> loadChapter(currentChapter));
        if (savedInstanceState != null) currentChapter = savedInstanceState.getInt("reader_chapter", currentChapter);
        bindViews();
        bookmarks = new BookmarkRepository(this);
        bookmarks.observeChapters(novelId).observe(this, chapters -> {
            bookmarkedChapters = chapters;
            updateBookmarkUI();
        });
        tts = new TtsController(this, paragraphs, (playing, index, status) -> {
            btnTtsPlayPause.setText(playing ? "Tạm dừng" : "Phát");
            ((MaterialButton) btnTtsPlayPause).setIconResource(playing
                    ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
            tvTtsStatus.setText(status);
            highlightParagraph(index);
        });
        setupEvents();
        loadChapter(currentChapter);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt("reader_chapter", currentChapter);
    }

    private void bindViews() {
        scrollReader = findViewById(R.id.scrollReader);
        readerContentBox = findViewById(R.id.readerContentBox);
        proseContainer = findViewById(R.id.proseContainer);
        paywallBox = findViewById(R.id.paywallBox);
        readerTopBar = findViewById(R.id.readerTopBar);
        readerBottomBar = findViewById(R.id.readerBottomBar);
        ttsPlayerBar = findViewById(R.id.ttsPlayerBar);

        tvChapterLabel = findViewById(R.id.tvChapterLabel);
        tvReaderTitle = findViewById(R.id.tvReaderTitle);
        tvReaderMeta = findViewById(R.id.tvReaderMeta);
        tvTopNovelName = findViewById(R.id.tvTopNovelName);
        tvTopChapter = findViewById(R.id.tvTopChapter);
        tvPaywallBalance = findViewById(R.id.tvPaywallBalance);
        tvEndChapterNote = findViewById(R.id.tvEndChapterNote);
        tvProgressCaption = findViewById(R.id.tvProgressCaption);
        tvReadPercent = findViewById(R.id.tvReadPercent);
        tvTtsStatus = findViewById(R.id.tvTtsStatus);

        btnReaderWallet = findViewById(R.id.btnReaderWallet);
        btnShowTools = findViewById(R.id.btnShowTools);
        btnPrevChap = findViewById(R.id.btnPrevChap);
        btnNextChap = findViewById(R.id.btnNextChap);
        btnEndNextChapter = findViewById(R.id.btnEndNextChapter);
        btnBookmark = findViewById(R.id.btnBookmark);
        btnTtsPlayPause = findViewById(R.id.btnTtsPlayPause);
        btnTtsRate = findViewById(R.id.btnTtsRate);
    }

    private void setupEvents() {
        findViewById(R.id.btnReaderBack).setOnClickListener(v -> finish());
        btnReaderWallet.setOnClickListener(v -> WalletBottomSheet.open(getSupportFragmentManager()));

        findViewById(R.id.btnHideTools).setOnClickListener(v -> setToolsVisible(false));
        btnShowTools.setOnClickListener(v -> setToolsVisible(true));
        readerContentBox.setOnClickListener(v -> setToolsVisible(!toolsVisible));

        scrollReader.setOnScrollChangeListener((view, x, y, oldX, oldY) -> updateReadingScroll(y));

        btnPrevChap.setOnClickListener(v -> {
            if (currentChapter > 1) loadChapter(currentChapter - 1);
        });
        btnNextChap.setOnClickListener(v -> {
            if (currentChapter < totalChapters) loadChapter(currentChapter + 1);
        });
        btnEndNextChapter.setOnClickListener(v -> {
            if (currentChapter < totalChapters) loadChapter(currentChapter + 1);
        });

        View.OnClickListener openChapters = v -> {
            setToolsVisible(true);
            ChapterDropdown dropdown = findViewById(R.id.chapterDropdown);
            dropdown.toggle(db, wallet, novelId, currentChapter, this::loadChapter);
        };
        findViewById(R.id.btnChapters).setOnClickListener(openChapters);
        findViewById(R.id.btnEndChapters).setOnClickListener(openChapters);

        findViewById(R.id.btnFontTheme).setOnClickListener(v -> showFontAndThemeDialog());

        btnBookmark.setOnClickListener(v -> {
            int chapter = currentChapter;
            bookmarks.toggle(novelId, chapter, saved -> {
                if (isFinishing() || isDestroyed()) return;
                if (chapter == currentChapter) renderBookmarkButton(saved);
                int message = saved ? R.string.bookmark_added : R.string.bookmark_removed;
                Toast.makeText(this, getString(message, chapter), Toast.LENGTH_SHORT).show();
            }, () -> {
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(this, R.string.bookmark_update_failed, Toast.LENGTH_SHORT).show();
                }
            });
        });

        findViewById(R.id.btnUnlockOne).setOnClickListener(v -> {
            if (wallet.unlockSingle(novelId, currentChapter)) {
                Toast.makeText(this, "Đã mở khóa chương!", Toast.LENGTH_SHORT).show();
                loadChapter(currentChapter);
            } else {
                Toast.makeText(this, "Không đủ xu!", Toast.LENGTH_SHORT).show();
                WalletBottomSheet.open(getSupportFragmentManager());
            }
        });

        findViewById(R.id.btnUnlockBundle).setOnClickListener(v -> {
            if (wallet.unlockBundle16To18(novelId)) {
                Toast.makeText(this, "Đã mở khóa cả 3 chương!", Toast.LENGTH_SHORT).show();
                loadChapter(currentChapter);
            } else {
                Toast.makeText(this, "Không đủ xu!", Toast.LENGTH_SHORT).show();
                WalletBottomSheet.open(getSupportFragmentManager());
            }
        });

        findViewById(R.id.btnListen).setOnClickListener(v -> {
            if (ttsPlayerBar.isShown()) closePlayer();
            else {
                ttsPlayerBar.setVisibility(View.VISIBLE);
                tts.play(0);
            }
        });

        btnTtsPlayPause.setOnClickListener(v -> tts.togglePlayback());
        findViewById(R.id.btnTtsPrev).setOnClickListener(v -> tts.skip(-1));
        findViewById(R.id.btnTtsNext).setOnClickListener(v -> tts.skip(1));
        btnTtsRate.setOnClickListener(v -> btnTtsRate.setText(tts.nextRate() + "x"));
        findViewById(R.id.btnTtsClose).setOnClickListener(v -> closePlayer());
    }

    private void setToolsVisible(boolean visible) {
        if (!visible) ((ChapterDropdown) findViewById(R.id.chapterDropdown)).collapse();
        toolsVisible = visible;
        readerTopBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        readerBottomBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        btnShowTools.setVisibility(visible ? View.GONE : View.VISIBLE);
    }

    // tai noi dung chuong va kiem tra mo khoa
    private void loadChapter(int chapterNumber) {
        Chapter chapter = db.getChapterByNumber(novelId, chapterNumber);
        if (chapter == null) {
            Toast.makeText(this, "Chưa có dữ liệu chương " + chapterNumber + " trong database", Toast.LENGTH_SHORT).show();
            return;
        }

        persistReadingScroll();
        canTrackScroll = false;
        if (scrollRestore != null) scrollRestore.removeListener();
        boolean changedChapter = chapterNumber != currentChapter;
        ((ChapterDropdown) findViewById(R.id.chapterDropdown)).collapse();
        tts.stop();
        currentChapter = chapterNumber;
        db.saveCurrentChapter(novelId, chapterNumber);

        boolean allowed = wallet.isUnlocked(novelId, chapterNumber, chapter.isPaid());
        ReadingProgress progress = db.getReadingProgress(novelId);
        savedChapterPercent = progress == null ? 0 : progress.chapterPercent;
        chapterPercent = savedChapterPercent;

        Novel novel = db.getNovelByIdFromDb(novelId);
        if (novel == null) {
            novel = NovelRepository.getInstance().getNovelById(novelId);
        }
        if (novel != null) {
            tvTopNovelName.setText(novel.getName());
            tvReaderMeta.setText(String.format(Locale.getDefault(), "%s · Khoảng 5 phút đọc", novel.getAuthor()));
        }

        tvChapterLabel.setText(String.format(Locale.getDefault(), "TẬP %02d · CHƯƠNG %d", chapter.getVolumeNumber(), chapterNumber));
        tvTopChapter.setText(String.format(Locale.getDefault(), "Chương %d · Tập %d", chapterNumber, chapter.getVolumeNumber()));
        tvReaderTitle.setText(chapter.getTitle());
        tvProgressCaption.setText(String.format(Locale.getDefault(), "Chương %d / %d", chapterNumber, totalChapters));
        btnReaderWallet.setText(String.format(Locale.getDefault(), "%d xu", wallet.getBalance()));

        btnPrevChap.setEnabled(chapterNumber > 1);
        btnNextChap.setEnabled(chapterNumber < totalChapters);
        btnEndNextChapter.setEnabled(chapterNumber < totalChapters);
        btnEndNextChapter.setText(chapterNumber == totalChapters ? "Bạn đã đọc đến chương mới nhất" : "Đọc chương tiếp theo →");
        tvEndChapterNote.setText(allowed ? "Bạn đã đọc hết chương này." : "Bạn đang xem trước chương này.");

        paragraphs.clear();
        paragraphViews.clear();
        proseContainer.removeAllViews();

        int fontSize = prefs.getInt("font_size", 18);
        int textColor = getAttrColor(R.attr.colorInk);

        if (chapter.getContent() != null) {
            String[] lines = chapter.getContent().split("\\r?\\n+");
            for (String line : lines) {
                String text = line.trim();
                if (!text.isEmpty()) paragraphs.add(text);
                if (!allowed && paragraphs.size() == 2) break;
            }
        }

        for (int i = 0; i < paragraphs.size(); i++) {
            TextView tv = new TextView(this);
            tv.setText(paragraphs.get(i));
            tv.setTypeface(Typeface.SERIF);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
            tv.setTextColor(textColor);
            tv.setLineSpacing(0, 1.6f);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 28);
            tv.setLayoutParams(lp);

            proseContainer.addView(tv);
            paragraphViews.add(tv);
        }

        if (allowed) {
            paywallBox.setVisibility(View.GONE);
        } else {
            paywallBox.setVisibility(View.VISIBLE);
            Button bundle = findViewById(R.id.btnUnlockBundle);
            bundle.setVisibility(chapterNumber >= 16 && chapterNumber <= 18 && db.hasPaidBundle(novelId)
                    ? View.VISIBLE : View.GONE);
            bundle.setText("Mở chương 16–18 · " + wallet.bundlePrice(novelId) + " xu");
            tvPaywallBalance.setText("Giá: 10 xu · Số dư: " + wallet.getBalance() + " xu");
        }

        updateBookmarkUI();
        scrollReader.scrollTo(0, 0);
        boolean readableContent = allowed && !paragraphs.isEmpty();
        tvReadPercent.setText((readableContent ? chapterPercent : 0) + "%");
        if (readableContent) {
            scrollRestore = OneShotPreDrawListener.add(scrollReader, () -> {
                scrollRestore = null;
                int range = getReadingScrollRange();
                scrollReader.scrollTo(0, Math.round(range * chapterPercent / 100f));
                canTrackScroll = true;
                if (range == 0) updateReadingScroll(0);
            });
        }
        if (changedChapter) {
            readerContentBox.animate().cancel();
            readerContentBox.setAlpha(0.55f);
            readerContentBox.animate().alpha(1f).setDuration(150).start();
        }
    }

    private int getReadingScrollRange() {
        int viewport = scrollReader.getHeight() - scrollReader.getPaddingTop() - scrollReader.getPaddingBottom();
        return Math.max(0, readerContentBox.getHeight() - viewport);
    }

    private void updateReadingScroll(int scrollY) {
        if (!canTrackScroll) return;
        int range = getReadingScrollRange();
        chapterPercent = range == 0 ? 100 : Math.max(0, Math.min(100, scrollY * 100 / range));
        tvReadPercent.setText(chapterPercent + "%");
        if (chapterPercent == 100) persistReadingScroll();
    }

    private void persistReadingScroll() {
        if (!canTrackScroll || chapterPercent == savedChapterPercent) return;
        boolean lastChapter = currentChapter == totalChapters;
        db.saveReadingScroll(novelId, currentChapter, chapterPercent, lastChapter);
        if (chapterPercent == 100) db.markChapterRead(novelId, currentChapter);
        savedChapterPercent = chapterPercent;
    }

    private void updateBookmarkUI() {
        renderBookmarkButton(bookmarkedChapters.contains(currentChapter));
    }

    private void renderBookmarkButton(boolean saved) {
        btnBookmark.setText(saved ? "Đã lưu" : "Lưu");
        ((MaterialButton) btnBookmark).setIconResource(saved
                ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
    }

    private void showFontAndThemeDialog() {
        int currentSize = prefs.getInt("font_size", 18);
        String[] items = {"Tăng cỡ chữ (+2sp)", "Giảm cỡ chữ (-2sp)", "Giao diện: Sáng", "Giao diện: Tối", "Giao diện: Sepia"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Tùy chỉnh trang đọc (" + currentSize + "sp)")
                .setItems(items, (d, which) -> {
                    if (which == 0 && currentSize < 28) {
                        prefs.edit().putInt("font_size", currentSize + 2).apply();
                        updateFontSize(currentSize + 2);
                    } else if (which == 1 && currentSize > 16) {
                        prefs.edit().putInt("font_size", currentSize - 2).apply();
                        updateFontSize(currentSize - 2);
                    } else if (which == 2) ThemeManager.setTheme(this, ThemeManager.LIGHT);
                    else if (which == 3) ThemeManager.setTheme(this, ThemeManager.DARK);
                    else if (which == 4) ThemeManager.setTheme(this, ThemeManager.SEPIA);
                })
                .show();
    }

    private void updateFontSize(int sizeSp) {
        for (TextView tv : paragraphViews) {
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        }
    }

    private int getAttrColor(int attr) {
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    private void closePlayer() {
        tts.stop();
        ttsPlayerBar.setVisibility(View.GONE);
    }

    private void highlightParagraph(int activeIndex) {
        int softColor = getAttrColor(R.attr.colorSoft);
        for (int i = 0; i < paragraphViews.size(); i++) {
            paragraphViews.get(i).setBackgroundColor(i == activeIndex ? softColor : Color.TRANSPARENT);
        }
    }

    @Override
    protected void onPause() {
        persistReadingScroll();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (scrollRestore != null) scrollRestore.removeListener();
        tts.close();
        db.close();
        super.onDestroy();
    }
}
