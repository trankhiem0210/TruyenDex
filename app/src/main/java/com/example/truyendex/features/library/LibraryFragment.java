package com.example.truyendex.features.library;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.truyendex.features.reader.ReaderActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.truyendex.R;
import com.example.truyendex.data.local.AppDatabase;
import com.example.truyendex.data.local.Bookmark;
import com.example.truyendex.data.model.Novel;
import com.example.truyendex.data.model.ReadingProgress;
import com.example.truyendex.data.repository.BookmarkRepository;
import com.example.truyendex.data.repository.NovelRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LibraryFragment extends Fragment {

    private MaterialButtonToggleGroup toggleTabs;
    private View layoutShelfActions;
    private TextView btnMarkSelected;
    private View btnClearHistory;
    private RecyclerView rvList;
    private View layoutEmpty;
    private TextView tvEmptyTitle;
    private TextView tvEmptyDesc;

    private LibraryAdapter adapter;
    private AppDatabase db;
    private List<LibraryRecord> allRecords = new ArrayList<>();
    private List<Bookmark> savedBookmarks = Collections.emptyList();
    private List<LibraryRecord> readingRecords = Collections.emptyList();
    private List<LibraryRecord> historyRecords = Collections.emptyList();
    private String currentMode = LibraryAdapter.MODE_READING;

    public LibraryFragment() {
        super(R.layout.fragment_library);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        toggleTabs = view.findViewById(R.id.toggleLibraryTabs);
        layoutShelfActions = view.findViewById(R.id.layoutShelfActions);
        btnMarkSelected = view.findViewById(R.id.btnMarkSelected);
        btnClearHistory = view.findViewById(R.id.btnClearHistory);
        rvList = view.findViewById(R.id.rvLibraryList);
        layoutEmpty = view.findViewById(R.id.layoutLibraryEmpty);
        tvEmptyTitle = view.findViewById(R.id.tvLibraryEmptyTitle);
        tvEmptyDesc = view.findViewById(R.id.tvLibraryEmptyDesc);

        db = new AppDatabase(requireContext());
        // Import legacy bookmarks before saving any library records.
        db.getWritableDatabase();
        allRecords = LibraryRecord.load(requireContext());
        BookmarkRepository bookmarks = new BookmarkRepository(requireContext());

        adapter = new LibraryAdapter(new LibraryAdapter.OnLibraryActionListener() {
            @Override
            public void onOpenNovel(int novelId) {
                Bundle args = new Bundle();
                args.putInt("novel_id", novelId);
                Navigation.findNavController(view).navigate(R.id.open_novel_detail, args);
            }

            @Override
            public void onOpenChapter(int novelId, int chapter) {
                startActivity(new Intent(requireContext(), ReaderActivity.class)
                        .putExtra("novel_id", novelId).putExtra("chapter_number", chapter));
            }

            @Override
            public void onSelectionChanged(int selectedCount) {
                updateMarkSelectedState(selectedCount > 0);
            }

            @Override
            public void onMarkSingleRead(int novelId) {
                List<Integer> ids = new ArrayList<>();
                ids.add(novelId);
                markAsRead(ids);
            }

            @Override
            public void onRemoveBookmark(int novelId, int chapter) {
                bookmarks.remove(novelId, chapter, () -> {
                    if (getView() != view) return;
                    Toast.makeText(requireContext(), getString(R.string.bookmark_removed, chapter),
                            Toast.LENGTH_SHORT).show();
                }, () -> {
                    if (getView() == view) {
                        Toast.makeText(requireContext(), R.string.bookmark_update_failed, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onRemoveHistory(int novelId) {
                db.removeFromHistory(novelId);
                reloadRecords();
                renderLibrary();
                Toast.makeText(requireContext(), R.string.library_history_removed, Toast.LENGTH_SHORT).show();
            }
        });

        rvList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvList.setAdapter(adapter);

        int tabId = R.id.tabLibReading;
        if (LibraryAdapter.MODE_SHELF.equals(currentMode)) tabId = R.id.tabLibShelf;
        else if (LibraryAdapter.MODE_BOOKMARKS.equals(currentMode)) tabId = R.id.tabLibBookmarks;
        else if (LibraryAdapter.MODE_HISTORY.equals(currentMode)) tabId = R.id.tabLibHistory;
        toggleTabs.check(tabId);
        toggleTabs.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.tabLibShelf) currentMode = LibraryAdapter.MODE_SHELF;
            else if (checkedId == R.id.tabLibBookmarks) currentMode = LibraryAdapter.MODE_BOOKMARKS;
            else if (checkedId == R.id.tabLibHistory) currentMode = LibraryAdapter.MODE_HISTORY;
            else currentMode = LibraryAdapter.MODE_READING;
            renderLibrary();
        });

        btnMarkSelected.setOnClickListener(v -> {
            Set<Integer> selected = adapter.getSelectedShelfIds();
            if (!selected.isEmpty()) {
                markAsRead(new ArrayList<>(selected));
                adapter.clearSelection();
            }
        });

        view.findViewById(R.id.btnMarkAll).setOnClickListener(v -> {
            List<Integer> shelfIds = new ArrayList<>();
            for (LibraryRecord r : allRecords) {
                if (r.onShelf) shelfIds.add(r.novelId);
            }
            markAsRead(shelfIds);
            adapter.clearSelection();
        });

        btnClearHistory.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.library_clear_history_title)
                .setMessage(R.string.library_clear_history_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.library_clear_history, (dialog, which) -> {
                    db.clearReadingHistory();
                    reloadRecords();
                    renderLibrary();
                    Toast.makeText(requireContext(), R.string.library_history_cleared,
                            Toast.LENGTH_SHORT).show();
                })
                .show());

        view.findViewById(R.id.btnEmptyDiscover).setOnClickListener(v -> {
            BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottomNav);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.nav_explore);
            }
        });

        bookmarks.observeAll().observe(getViewLifecycleOwner(), entries -> {
            savedBookmarks = entries;
            if (LibraryAdapter.MODE_BOOKMARKS.equals(currentMode)) renderLibrary();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadRecords();
        renderLibrary();
    }

    @Override
    public void onDestroyView() {
        db.close();
        super.onDestroyView();
    }

    private void reloadRecords() {
        NovelRepository repository = NovelRepository.getInstance();
        repository.loadNovelsFromDb(db);
        Set<Integer> shelfNovelIds = new HashSet<>(db.getShelfNovelIds());
        List<Integer> historyNovelIds = db.getReadHistoryNovelIds();
        Set<Integer> recentNovelIds = new HashSet<>(historyNovelIds);
        Map<Integer, ReadingProgress> progressByNovel = db.getReadingProgressByNovel();
        Map<Integer, LibraryRecord> records = new LinkedHashMap<>();
        for (LibraryRecord record : LibraryRecord.load(requireContext())) {
            records.put(record.novelId, record);
        }

        Set<Integer> activeNovelIds = new HashSet<>(shelfNovelIds);
        activeNovelIds.addAll(recentNovelIds);
        activeNovelIds.addAll(progressByNovel.keySet());
        for (int novelId : activeNovelIds) {
            if (records.containsKey(novelId)) continue;
            Novel novel = repository.getNovelById(novelId);
            if (novel == null) continue;
            int chapter = db.getCurrentChapter(novelId);
            int unread = Math.max(0, novel.getChapters() - chapter);
            records.put(novelId, new LibraryRecord(novelId, chapter, 0, unread,
                    false, false, false, Collections.emptyList()));
        }
        for (LibraryRecord record : records.values()) {
            record.onShelf = shelfNovelIds.contains(record.novelId);
            record.history = recentNovelIds.contains(record.novelId);
            ReadingProgress progress = progressByNovel.get(record.novelId);
            record.reading = record.history && progress != null && !progress.finished;
            if (progress != null) {
                int total = db.getTotalChapters(record.novelId);
                Novel novel = repository.getNovelById(record.novelId);
                if (total == 0 && novel != null) total = novel.getChapters();
                record.chapter = progress.chapter;
                record.progress = progress.novelPercent(total);
                if (progress.finished) record.unread = 0;
            }
        }
        readingRecords = new ArrayList<>();
        for (int novelId : progressByNovel.keySet()) {
            LibraryRecord record = records.get(novelId);
            if (record != null && record.reading) readingRecords.add(record);
        }
        historyRecords = new ArrayList<>();
        for (int novelId : historyNovelIds) {
            LibraryRecord record = records.get(novelId);
            if (record != null) historyRecords.add(record);
        }
        allRecords = new ArrayList<>(records.values());
    }

    private void markAsRead(List<Integer> novelIds) {
        db.markReadingFinished(novelIds);
        for (LibraryRecord r : allRecords) {
            if (novelIds.contains(r.novelId)) {
                r.unread = 0;
                r.progress = 100;
                r.reading = false;
            }
        }
        LibraryRecord.save(requireContext(), allRecords);
        reloadRecords();
        Toast.makeText(requireContext(), "Đã cập nhật trạng thái đã đọc", Toast.LENGTH_SHORT).show();
        renderLibrary();
    }

    private void updateMarkSelectedState(boolean enabled) {
        btnMarkSelected.setEnabled(enabled);
        TypedValue tv = new TypedValue();
        int attr = enabled ? R.attr.colorBrand : R.attr.colorMuted;
        requireContext().getTheme().resolveAttribute(attr, tv, true);
        btnMarkSelected.setTextColor(tv.data);
    }

    private void renderLibrary() {
        int activeId = toggleTabs.getCheckedButtonId();
        for (int i = 0; i < toggleTabs.getChildCount(); i++) {
            TextView tab = (TextView) toggleTabs.getChildAt(i);
            tab.setTypeface(null, tab.getId() == activeId ? Typeface.BOLD : Typeface.NORMAL);
        }

        boolean isShelf = LibraryAdapter.MODE_SHELF.equals(currentMode);
        layoutShelfActions.setVisibility(isShelf ? View.VISIBLE : View.GONE);
        updateMarkSelectedState(!adapter.getSelectedShelfIds().isEmpty());

        List<LibraryRecord> filtered = new ArrayList<>();
        if (LibraryAdapter.MODE_READING.equals(currentMode)) {
            filtered.addAll(readingRecords);
        } else if (isShelf) {
            for (LibraryRecord record : allRecords) {
                if (record.onShelf) filtered.add(record);
            }
        } else if (LibraryAdapter.MODE_HISTORY.equals(currentMode)) {
            filtered.addAll(historyRecords);
        }

        if (LibraryAdapter.MODE_BOOKMARKS.equals(currentMode)) {
            Map<Integer, List<Integer>> chaptersByNovel = new LinkedHashMap<>();
            for (Bookmark bookmark : savedBookmarks) {
                chaptersByNovel.computeIfAbsent(bookmark.novelId, id -> new ArrayList<>())
                        .add(bookmark.chapterNumber);
            }
            chaptersByNovel.forEach((novelId, chapters) ->
                    filtered.add(new LibraryRecord(novelId, chapters.get(0), 0, 0,
                            false, false, false, chapters)));
        }

        adapter.submitData(filtered, currentMode);
        btnClearHistory.setVisibility(LibraryAdapter.MODE_HISTORY.equals(currentMode) && !filtered.isEmpty()
                ? View.VISIBLE : View.GONE);

        if (filtered.isEmpty()) {
            rvList.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
            switch (currentMode) {
                case LibraryAdapter.MODE_SHELF:
                    tvEmptyTitle.setText("Kệ sách đang trống");
                    tvEmptyDesc.setText("Những truyện đã lưu sẽ nằm ở đây.");
                    break;
                case LibraryAdapter.MODE_BOOKMARKS:
                    tvEmptyTitle.setText("Chưa có bookmark");
                    tvEmptyDesc.setText(R.string.bookmarks_empty_description);
                    break;
                case LibraryAdapter.MODE_HISTORY:
                    tvEmptyTitle.setText("Chưa có lịch sử đọc");
                    tvEmptyDesc.setText("Các truyện bạn đã mở sẽ xuất hiện tại đây.");
                    break;
                default:
                    tvEmptyTitle.setText("Bạn chưa có truyện đang đọc");
                    tvEmptyDesc.setText("Khám phá một câu chuyện để bắt đầu.");
                    break;
            }
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvList.setVisibility(View.VISIBLE);
        }
    }
}
