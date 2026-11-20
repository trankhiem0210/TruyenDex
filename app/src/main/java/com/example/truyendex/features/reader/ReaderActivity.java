package com.example.truyendex.features.reader;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.truyendex.R;
import com.example.truyendex.core.ThemeManager;
import com.example.truyendex.core.WalletPref;
import com.example.truyendex.data.local.AppDatabase;
import com.example.truyendex.data.model.Chapter;
import com.example.truyendex.data.model.Novel;
import com.example.truyendex.data.repository.NovelRepository;
import com.example.truyendex.features.novel.ChaptersActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReaderActivity extends AppCompatActivity {

    private int novelId = 1;
    private int currentChapter = 1;
    private int totalChapters = 18;
    private boolean toolsVisible = true;

    private AppDatabase db;
    private WalletPref wallet;
    private SharedPreferences prefs;

    private ScrollView scrollReader;
    private LinearLayout readerContentBox, proseContainer, paywallBox, readerTopBar, readerBottomBar, ttsPlayerBar;
    private TextView tvChapterLabel, tvReaderTitle, tvReaderMeta, tvTopNovelName, tvTopChapter;
    private TextView tvPaywallBalance, tvEndChapterNote, tvProgressCaption, tvReadPercent, tvTtsStatus;
    private Button btnReaderWallet, btnShowTools, btnPrevChap, btnNextChap, btnEndNextChapter, btnBookmark, btnTtsPlayPause, btnTtsRate;

    // Text-To-Speech
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private boolean ttsPlaying = false;
    private int ttsIndex = 0;
    private final float[] rates = {0.8f, 1.0f, 1.25f, 1.5f};
    private int rateIdx = 1;
    private final List<String> paragraphs = new ArrayList<>();
    private final List<TextView> paragraphViews = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        novelId = getIntent().getIntExtra("novel_id", 1);
        db = new AppDatabase(this);
        db.initChapterIfEmpty(this, novelId);
        wallet = new WalletPref(this);
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

        bindViews();
        initTTS();
        setupEvents();
        loadChapter(currentChapter);
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
        btnReaderWallet.setOnClickListener(v -> wallet.showTopUpDialog(this, () -> loadChapter(currentChapter)));

        findViewById(R.id.btnHideTools).setOnClickListener(v -> setToolsVisible(false));
        btnShowTools.setOnClickListener(v -> setToolsVisible(true));
        readerContentBox.setOnClickListener(v -> setToolsVisible(!toolsVisible));

        scrollReader.getViewTreeObserver().addOnScrollChangedListener(() -> {
            View child = scrollReader.getChildAt(0);
            if (child == null) return;
            int max = child.getHeight() - scrollReader.getHeight();
            int percent = max > 0 ? Math.min(100, Math.max(0, (scrollReader.getScrollY() * 100) / max)) : 100;
            tvReadPercent.setText(percent + "%");
            if (percent >= 98) db.markChapterRead(novelId, currentChapter);
        });

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
            Intent intent = new Intent(this, ChaptersActivity.class);
            intent.putExtra("novel_id", novelId);
            startActivity(intent);
        };
        findViewById(R.id.btnChapters).setOnClickListener(openChapters);
        findViewById(R.id.btnEndChapters).setOnClickListener(openChapters);

        findViewById(R.id.btnFontTheme).setOnClickListener(v -> showFontAndThemeDialog());

        btnBookmark.setOnClickListener(v -> {
            db.toggleBookmark(novelId, currentChapter);
            updateBookmarkUI();
            Toast.makeText(this, db.isBookmarked(novelId, currentChapter)
                    ? "Đã đánh dấu chương " + currentChapter
                    : "Đã bỏ đánh dấu", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnUnlockOne).setOnClickListener(v -> {
            if (wallet.unlockSingle(novelId, currentChapter)) {
                Toast.makeText(this, "Đã mở khóa chương!", Toast.LENGTH_SHORT).show();
                loadChapter(currentChapter);
            } else {
                Toast.makeText(this, "Không đủ xu!", Toast.LENGTH_SHORT).show();
                wallet.showTopUpDialog(this, () -> loadChapter(currentChapter));
            }
        });

        findViewById(R.id.btnUnlockBundle).setOnClickListener(v -> {
            if (wallet.unlockBundle16To18(novelId)) {
                Toast.makeText(this, "Đã mở khóa cả 3 chương!", Toast.LENGTH_SHORT).show();
                loadChapter(currentChapter);
            } else {
                Toast.makeText(this, "Không đủ xu!", Toast.LENGTH_SHORT).show();
                wallet.showTopUpDialog(this, () -> loadChapter(currentChapter));
            }
        });

        findViewById(R.id.btnListen).setOnClickListener(v -> {
            if (ttsPlayerBar.getVisibility() == View.VISIBLE) stopTTS(true);
            else {
                ttsPlayerBar.setVisibility(View.VISIBLE);
                startSpeakingFrom(0);
            }
        });

        btnTtsPlayPause.setOnClickListener(v -> {
            if (ttsPlaying) pauseTTS();
            else startSpeakingFrom(ttsIndex);
        });

        findViewById(R.id.btnTtsPrev).setOnClickListener(v -> {
            if (ttsIndex > 0) startSpeakingFrom(ttsIndex - 1);
        });
        findViewById(R.id.btnTtsNext).setOnClickListener(v -> {
            if (ttsIndex < paragraphs.size() - 1) startSpeakingFrom(ttsIndex + 1);
        });
        btnTtsRate.setOnClickListener(v -> {
            rateIdx = (rateIdx + 1) % rates.length;
            btnTtsRate.setText(rates[rateIdx] + "x");
            if (ttsReady) tts.setSpeechRate(rates[rateIdx]);
            if (ttsPlaying) startSpeakingFrom(ttsIndex);
        });
        findViewById(R.id.btnTtsClose).setOnClickListener(v -> stopTTS(true));
    }

    private void setToolsVisible(boolean visible) {
        toolsVisible = visible;
        readerTopBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        readerBottomBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        btnShowTools.setVisibility(visible ? View.GONE : View.VISIBLE);
    }

    // tai noi dung chuong va kiem tra mo khoa
    private void loadChapter(int chapterNumber) {
        stopTTS(false);
        currentChapter = chapterNumber;
        db.saveCurrentChapter(novelId, chapterNumber);

        Chapter chapter = db.getChapterByNumber(novelId, chapterNumber);
        if (chapter == null) {
            Toast.makeText(this, "Chưa có dữ liệu chương " + chapterNumber + " trong database", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean allowed = wallet.isUnlocked(novelId, chapterNumber, chapter.isPaid());
        if (allowed) db.markChapterRead(novelId, chapterNumber);

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
            }
        }

        int limit = allowed ? paragraphs.size() : Math.min(2, paragraphs.size());
        for (int i = 0; i < limit; i++) {
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
            tvPaywallBalance.setText("Giá: 10 xu · Số dư: " + wallet.getBalance() + " xu");
        }

        updateBookmarkUI();
        scrollReader.scrollTo(0, 0);
        tvReadPercent.setText("0%");
    }

    private void updateBookmarkUI() {
        btnBookmark.setText(db.isBookmarked(novelId, currentChapter) ? "Đã lưu" : "Đánh dấu");
    }

    private void showFontAndThemeDialog() {
        int currentSize = prefs.getInt("font_size", 18);
        String[] items = {"Tăng cỡ chữ (+2sp)", "Giảm cỡ chữ (-2sp)", "Giao diện: Sáng", "Giao diện: Tối", "Giao diện: Sepia"};
        new AlertDialog.Builder(this)
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

    private void initTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int res = tts.setLanguage(new Locale.Builder().setLanguage("vi").setRegion("VN").build());
                ttsReady = (res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED);
                tts.setSpeechRate(rates[rateIdx]);
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String id) {}
                    @Override public void onDone(String id) {
                        runOnUiThread(() -> {
                            if (!ttsPlaying) return;
                            if (ttsIndex + 1 < paragraphViews.size()) startSpeakingFrom(ttsIndex + 1);
                            else {
                                stopTTS(false);
                                tvTtsStatus.setText("Đã đọc hết chương");
                            }
                        });
                    }
                    @Override public void onError(String id) {}
                });
            }
        });
    }

    private void startSpeakingFrom(int index) {
        if (!ttsReady) {
            Toast.makeText(this, "Thiết bị chưa hỗ trợ đọc tiếng Việt", Toast.LENGTH_SHORT).show();
            return;
        }
        if (index < 0 || index >= paragraphViews.size()) return;
        ttsIndex = index;
        ttsPlaying = true;
        btnTtsPlayPause.setText("Tạm dừng");
        tvTtsStatus.setText("Đang đọc đoạn " + (ttsIndex + 1) + "/" + paragraphViews.size());
        highlightParagraph(ttsIndex);
        tts.speak(paragraphs.get(ttsIndex), TextToSpeech.QUEUE_FLUSH, null, "utt_" + ttsIndex);
    }

    private void pauseTTS() {
        ttsPlaying = false;
        if (tts != null) tts.stop();
        btnTtsPlayPause.setText("Phát");
        tvTtsStatus.setText("Tạm dừng ở đoạn " + (ttsIndex + 1));
    }

    private void stopTTS(boolean hideBar) {
        ttsPlaying = false;
        ttsIndex = 0;
        if (tts != null) tts.stop();
        btnTtsPlayPause.setText("Phát");
        highlightParagraph(-1);
        if (hideBar) ttsPlayerBar.setVisibility(View.GONE);
    }

    private void highlightParagraph(int activeIndex) {
        int softColor = getAttrColor(R.attr.colorSoft);
        for (int i = 0; i < paragraphViews.size(); i++) {
            paragraphViews.get(i).setBackgroundColor(i == activeIndex ? softColor : Color.TRANSPARENT);
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
