package com.example.truyendex.features.reader;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.List;
import java.util.Locale;

final class TtsController {
    interface Listener {
        void onPlaybackChanged(boolean playing, int highlightedParagraph, String status);
    }

    private static final float[] RATES = {0.8f, 1f, 1.25f, 1.5f};
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<String> paragraphs;
    private final Listener listener;
    private TextToSpeech engine;
    private boolean ready;
    private boolean playing;
    private boolean initializing = true;
    private int pendingParagraph = -1;
    private int index;
    private int offset;
    private int rateIndex = 1;
    private long request;

    TtsController(Context context, List<String> paragraphs, Listener listener) {
        this.paragraphs = paragraphs;
        this.listener = listener;
        engine = new TextToSpeech(context.getApplicationContext(), status ->
                main.post(() -> initialize(status)));
    }

    private void initialize(int status) {
        if (engine == null) return;
        initializing = false;
        ready = status == TextToSpeech.SUCCESS && engine.setLanguage(Locale.forLanguageTag("vi-VN")) >= 0;
        if (!ready) {
            if (pendingParagraph >= 0) finish("Thiết bị chưa có giọng đọc tiếng Việt");
            pendingParagraph = -1;
            return;
        }
        engine.setSpeechRate(RATES[rateIndex]);
        engine.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String id) {}
            @Override public void onDone(String id) { main.post(() -> complete(id, true)); }
            @Override public void onError(String id) { main.post(() -> complete(id, false)); }
        });
        if (pendingParagraph >= 0) {
            int paragraph = pendingParagraph;
            pendingParagraph = -1;
            play(paragraph);
        }
    }

    private void complete(String id, boolean success) {
        if (!playing || !Long.toString(request).equals(id)) return;
        if (!success) {
            finish("Không phát được đoạn này");
        } else if (offset < paragraphs.get(index).length()) {
            speakChunk();
        } else if (index + 1 < paragraphs.size()) {
            play(index + 1);
        } else {
            finish("Đã đọc hết phần nội dung hiển thị");
        }
    }

    void play(int paragraph) {
        if (paragraph < 0 || paragraph >= paragraphs.size()) return;
        if (!ready) {
            if (initializing) pendingParagraph = paragraph;
            listener.onPlaybackChanged(false, -1, initializing
                    ? "Đang khởi tạo giọng đọc…" : "Thiết bị chưa có giọng đọc tiếng Việt");
            return;
        }
        index = paragraph;
        offset = 0;
        playing = true;
        listener.onPlaybackChanged(true, index, "Đang đọc đoạn " + (index + 1) + "/" + paragraphs.size());
        speakChunk();
    }

    private void speakChunk() {
        String text = paragraphs.get(index);
        int end = Math.min(text.length(), offset + TextToSpeech.getMaxSpeechInputLength());
        if (end < text.length() && Character.isLowSurrogate(text.charAt(end))) end--;
        String chunk = text.substring(offset, end);
        offset = end;
        if (engine.speak(chunk, TextToSpeech.QUEUE_FLUSH, null, Long.toString(++request)) == TextToSpeech.ERROR) {
            finish("Không phát được đoạn này");
        }
    }

    void togglePlayback() {
        if (!playing) {
            play(index);
            return;
        }
        playing = false;
        engine.stop();
        listener.onPlaybackChanged(false, index, "Tạm dừng ở đoạn " + (index + 1));
    }

    void skip(int delta) { play(index + delta); }

    float nextRate() {
        rateIndex = (rateIndex + 1) % RATES.length;
        if (ready) engine.setSpeechRate(RATES[rateIndex]);
        if (playing) play(index);
        return RATES[rateIndex];
    }

    private void finish(String status) {
        playing = false;
        index = 0;
        listener.onPlaybackChanged(false, -1, status);
    }

    void stop() {
        pendingParagraph = -1;
        if (ready) engine.stop();
        finish("Sẵn sàng đọc");
    }

    void close() {
        playing = false;
        ready = false;
        engine.stop();
        engine.shutdown();
        engine = null;
        main.removeCallbacksAndMessages(null);
    }
}
