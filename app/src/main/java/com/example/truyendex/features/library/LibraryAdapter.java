package com.example.truyendex.features.library;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.truyendex.R;
import com.example.truyendex.data.model.Novel;
import com.example.truyendex.data.repository.NovelRepository;
import com.google.android.material.button.MaterialButton;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LibraryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final String MODE_READING = "reading";
    public static final String MODE_SHELF = "shelf";
    public static final String MODE_BOOKMARKS = "bookmarks";
    public static final String MODE_HISTORY = "history";

    public interface OnLibraryActionListener {
        void onOpenNovel(int novelId);
        void onOpenChapter(int novelId, int chapter);
        void onSelectionChanged(int selectedCount);
        void onMarkSingleRead(int novelId);
        void onRemoveBookmark(int novelId, int chapter);
        void onRemoveHistory(int novelId);
    }

    private final NovelRepository repository = NovelRepository.getInstance();
    private final OnLibraryActionListener listener;
    private List<LibraryRecord> items = Collections.emptyList();
    private String mode = MODE_READING;
    private final Set<Integer> selectedShelfIds = new HashSet<>();
    private final Set<Integer> expandedBookmarkIds = new HashSet<>();

    public LibraryAdapter(OnLibraryActionListener listener) {
        this.listener = listener;
    }

    public void submitData(List<LibraryRecord> list, String newMode) {
        this.items = list;
        this.mode = newMode;
        notifyDataSetChanged();
    }

    public Set<Integer> getSelectedShelfIds() {
        return new HashSet<>(selectedShelfIds);
    }

    public void clearSelection() {
        selectedShelfIds.clear();
        if (listener != null) listener.onSelectionChanged(0);
    }

    @Override
    public int getItemViewType(int position) {
        if (MODE_SHELF.equals(mode)) return 1;
        if (MODE_BOOKMARKS.equals(mode)) return 2;
        return 0;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == 1) {
            return new ShelfHolder(inflater.inflate(R.layout.item_library_shelf, parent, false));
        }
        if (viewType == 2) {
            return new BookmarkHolder(inflater.inflate(R.layout.item_library_bookmark, parent, false));
        }
        return new ReadingHolder(inflater.inflate(R.layout.item_library_reading, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        LibraryRecord record = items.get(position);
        Novel novel = repository.getNovelById(record.novelId);
        if (novel == null) return;

        int coverRes = novel.getCoverImageResId() == 0
                ? R.drawable.cover_placeholder : novel.getCoverImageResId();

        holder.itemView.setOnClickListener(v -> listener.onOpenNovel(record.novelId));
        if (holder instanceof ReadingHolder) {
            ReadingHolder h = (ReadingHolder) holder;
            h.cover.setImageResource(coverRes);
            h.title.setText(novel.getName());
            boolean isHistory = MODE_HISTORY.equals(mode);
            String statusLabel = isHistory ? "Đã mở gần đây" : "Đang đọc";
            h.sub.setText("Chương " + record.chapter + " · " + statusLabel);
            h.progress.setProgress(record.progress);
            h.action.setText("Đọc tiếp · " + record.progress + "% →");
            h.action.setOnClickListener(v -> listener.onOpenChapter(record.novelId, record.chapter));
            h.removeHistory.setVisibility(isHistory ? View.VISIBLE : View.GONE);
            h.removeHistory.setOnClickListener(isHistory ? v -> listener.onRemoveHistory(record.novelId) : null);

        } else if (holder instanceof ShelfHolder) {
            ShelfHolder h = (ShelfHolder) holder;
            h.cover.setImageResource(coverRes);
            h.title.setText(novel.getName());
            h.type.setText(novel.getType());
            h.unreadBadge.setText(String.valueOf(record.unread));

            h.checkBox.setOnCheckedChangeListener(null);
            h.checkBox.setChecked(selectedShelfIds.contains(record.novelId));
            h.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) selectedShelfIds.add(record.novelId);
                else selectedShelfIds.remove(record.novelId);
                if (listener != null) listener.onSelectionChanged(selectedShelfIds.size());
            });

            h.btnMarkRead.setOnClickListener(v -> {
                if (listener != null) listener.onMarkSingleRead(record.novelId);
            });

        } else if (holder instanceof BookmarkHolder) {
            BookmarkHolder h = (BookmarkHolder) holder;
            h.cover.setImageResource(coverRes);
            h.title.setText(novel.getName());
            h.count.setText(record.bookmarks.size() + " chương đánh dấu");

            boolean expanded = expandedBookmarkIds.contains(record.novelId);
            h.arrow.setRotation(expanded ? 90f : 0f);
            h.chaptersContainer.setVisibility(expanded ? View.VISIBLE : View.GONE);

            h.header.setOnClickListener(v -> listener.onOpenNovel(record.novelId));
            h.arrow.setContentDescription(expanded ? "Thu gọn chương đã lưu" : "Mở danh sách chương đã lưu");
            h.arrow.setOnClickListener(v -> {
                if (expandedBookmarkIds.contains(record.novelId)) {
                    expandedBookmarkIds.remove(record.novelId);
                } else {
                    expandedBookmarkIds.add(record.novelId);
                }
                notifyItemChanged(holder.getBindingAdapterPosition());
            });

            h.chaptersContainer.removeAllViews();
            if (expanded) {
                for (int chapterNum : record.bookmarks) {
                    LinearLayout row = new LinearLayout(h.itemView.getContext());
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setPadding(0, 12, 0, 0);

                    MaterialButton btnChapter = new MaterialButton(h.itemView.getContext(),
                            null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    btnChapter.setLayoutParams(lp);
                    btnChapter.setText("Chương " + chapterNum);
                    btnChapter.setAllCaps(false);
                    btnChapter.setOnClickListener(v -> listener.onOpenChapter(record.novelId, chapterNum));

                    ImageButton btnRemove = new ImageButton(h.itemView.getContext());
                    btnRemove.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                    btnRemove.setBackgroundResource(0);
                    btnRemove.setContentDescription("Bỏ đánh dấu chương " + chapterNum);
                    btnRemove.setOnClickListener(v -> {
                        if (listener != null) listener.onRemoveBookmark(record.novelId, chapterNum);
                    });

                    row.addView(btnChapter);
                    row.addView(btnRemove);
                    h.chaptersContainer.addView(row);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ReadingHolder extends RecyclerView.ViewHolder {
        final View removeHistory;
        final ImageView cover;
        final TextView title, sub, action;
        final ProgressBar progress;

        ReadingHolder(@NonNull View itemView) {
            super(itemView);
            removeHistory = itemView.findViewById(R.id.btnRemoveHistory);
            cover = itemView.findViewById(R.id.imgLibCover);
            title = itemView.findViewById(R.id.tvLibTitle);
            sub = itemView.findViewById(R.id.tvLibSub);
            progress = itemView.findViewById(R.id.progressLib);
            action = itemView.findViewById(R.id.tvLibContinue);
        }
    }

    static class ShelfHolder extends RecyclerView.ViewHolder {
        final CheckBox checkBox;
        final ImageView cover;
        final TextView title, type, unreadBadge;
        final MaterialButton btnMarkRead;

        ShelfHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.cbShelfSelect);
            cover = itemView.findViewById(R.id.imgShelfCover);
            title = itemView.findViewById(R.id.tvShelfTitle);
            type = itemView.findViewById(R.id.tvShelfType);
            unreadBadge = itemView.findViewById(R.id.tvShelfUnreadBadge);
            btnMarkRead = itemView.findViewById(R.id.btnShelfMarkRead);
        }
    }

    static class BookmarkHolder extends RecyclerView.ViewHolder {
        final View header;
        final ImageView cover, arrow;
        final TextView title, count;
        final LinearLayout chaptersContainer;

        BookmarkHolder(@NonNull View itemView) {
            super(itemView);
            header = itemView.findViewById(R.id.layoutBookmarkHeader);
            cover = itemView.findViewById(R.id.imgBookmarkCover);
            arrow = itemView.findViewById(R.id.imgBookmarkArrow);
            title = itemView.findViewById(R.id.tvBookmarkTitle);
            count = itemView.findViewById(R.id.tvBookmarkCount);
            chaptersContainer = itemView.findViewById(R.id.layoutBookmarkChapters);
        }
    }
}
