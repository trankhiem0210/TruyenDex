package com.example.truyendex.features.library;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.truyendex.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LibraryFragment extends Fragment {

    private MaterialButtonToggleGroup toggleTabs;
    private View layoutShelfActions;
    private TextView btnMarkSelected;
    private RecyclerView rvList;
    private View layoutEmpty;
    private TextView tvEmptyTitle;
    private TextView tvEmptyDesc;

    private LibraryAdapter adapter;
    private List<LibraryRecord> allRecords = new ArrayList<>();
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
        rvList = view.findViewById(R.id.rvLibraryList);
        layoutEmpty = view.findViewById(R.id.layoutLibraryEmpty);
        tvEmptyTitle = view.findViewById(R.id.tvLibraryEmptyTitle);
        tvEmptyDesc = view.findViewById(R.id.tvLibraryEmptyDesc);

        allRecords = LibraryRecord.load(requireContext());

        adapter = new LibraryAdapter(new LibraryAdapter.OnLibraryActionListener() {
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
                for (LibraryRecord r : allRecords) {
                    if (r.novelId == novelId) {
                        r.bookmarks.remove(Integer.valueOf(chapter));
                        break;
                    }
                }
                LibraryRecord.save(requireContext(), allRecords);
                Toast.makeText(requireContext(), "Đã bỏ đánh dấu chương", Toast.LENGTH_SHORT).show();
                renderLibrary();
            }
        });

        rvList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvList.setAdapter(adapter);

        toggleTabs.check(R.id.tabLibReading);
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

        view.findViewById(R.id.btnEmptyDiscover).setOnClickListener(v -> {
            BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottomNav);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.nav_explore);
            }
        });

        renderLibrary();
    }

    private void markAsRead(List<Integer> novelIds) {
        for (LibraryRecord r : allRecords) {
            if (novelIds.contains(r.novelId)) {
                r.unread = 0;
                r.progress = 100;
                r.reading = false;
            }
        }
        LibraryRecord.save(requireContext(), allRecords);
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
        for (LibraryRecord r : allRecords) {
            if (LibraryAdapter.MODE_READING.equals(currentMode) && r.reading && r.progress < 100) {
                filtered.add(r);
            } else if (LibraryAdapter.MODE_SHELF.equals(currentMode) && r.onShelf) {
                filtered.add(r);
            } else if (LibraryAdapter.MODE_BOOKMARKS.equals(currentMode) && !r.bookmarks.isEmpty()) {
                filtered.add(r);
            } else if (LibraryAdapter.MODE_HISTORY.equals(currentMode) && r.history) {
                filtered.add(r);
            }
        }

        adapter.submitData(filtered, currentMode);

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
                    tvEmptyDesc.setText("Đánh dấu chương yêu thích trong trình đọc.");
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
