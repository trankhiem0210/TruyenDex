package com.example.truyendex.features.novel;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.truyendex.R;
import com.example.truyendex.data.repository.WalletRepository;
import com.example.truyendex.data.repository.BookmarkRepository;
import com.example.truyendex.data.repository.FollowRepository;
import com.example.truyendex.features.wallet.WalletBottomSheet;
import com.example.truyendex.data.local.AppDatabase;
import com.example.truyendex.data.model.Novel;
import com.example.truyendex.data.repository.NovelRepository;
import com.example.truyendex.features.reader.ReaderActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.Locale;
import java.util.Collections;
import java.util.List;

public class NovelDetailFragment extends Fragment {
    private int novelId = 1;
    private boolean expanded = false;
    private AppDatabase db;
    private List<Integer> bookmarkedChapters = Collections.emptyList();
    private WalletRepository wallet;

    private TextView tvResumeChapter;
    private Button btnDetailWallet, btnReadContinue;
    private MaterialButton btnBookmarkChapter;
    private MaterialButton btnToggleShelf;

    public NovelDetailFragment() {
        super(R.layout.fragment_novel_detail);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            novelId = getArguments().getInt("novel_id", 1);
        }

        db = new AppDatabase(requireContext());
        db.initChapterIfEmpty(requireContext(), novelId);
        wallet = new WalletRepository(requireContext());

        // uu tien lay thong tin truyen tu db, neu khong co moi lay tu repository
        Novel novel = db.getNovelByIdFromDb(novelId);
        if (novel == null) {
            novel = NovelRepository.getInstance().getNovelById(novelId);
        }
        int dbChapterCount = db.getTotalChapters(novelId);

        TextView tvTitle = view.findViewById(R.id.tvNovelTitle);
        ImageView novelCover = view.findViewById(R.id.imgNovelCover);
        TextView tvStatus = view.findViewById(R.id.tvNovelStatus);
        TextView tvAuthor = view.findViewById(R.id.tvNovelAuthor);
        TextView tvType = view.findViewById(R.id.tvNovelType);
        TextView tvStatChapters = view.findViewById(R.id.tvStatChapters);
        TextView tvStatViews = view.findViewById(R.id.tvStatViews);
        TextView tvChaptersSummary = view.findViewById(R.id.tvChaptersSummary);
        TextView tvAuthorAvatar = view.findViewById(R.id.tvAuthorAvatar);
        TextView tvAuthorCardName = view.findViewById(R.id.tvAuthorCardName);
        ChipGroup chipGroupGenres = view.findViewById(R.id.chipGroupGenres);
        TextView tvSynopsis = view.findViewById(R.id.tvSynopsis);
        TextView tvToggle = view.findViewById(R.id.tvToggleSynopsis);
        tvResumeChapter = view.findViewById(R.id.tvResumeChapter);
        btnDetailWallet = view.findViewById(R.id.btnDetailWallet);
        btnReadContinue = view.findViewById(R.id.btnReadContinue);
        btnBookmarkChapter = view.findViewById(R.id.btnBookmarkChapter);
        btnToggleShelf = view.findViewById(R.id.btnToggleShelf);
        MaterialButton btnFollow = view.findViewById(R.id.btnFollowNovel);
        View btnShare = view.findViewById(R.id.btnShareNovel);
        btnFollow.setEnabled(false);
        btnShare.setEnabled(novel != null);
        btnToggleShelf.setEnabled(novel != null);
        btnToggleShelf.setOnClickListener(v -> {
            boolean onShelf = !db.isOnShelf(novelId);
            db.setOnShelf(novelId, onShelf);
            updateShelfButton(onShelf);
            Toast.makeText(requireContext(), onShelf ? R.string.library_shelf_added
                    : R.string.library_shelf_removed, Toast.LENGTH_SHORT).show();
        });

        if (novel != null) {
            setupFollowAndShare(view, btnFollow, btnShare, novel);
            int totalCh = dbChapterCount > 0 ? dbChapterCount : novel.getChapters();
            int totalVol = ((totalCh - 1) / 10) + 1;

            tvTitle.setText(novel.getName());
            int coverResId = novel.getCoverImageResId();
            novelCover.setImageResource(coverResId == 0 ? R.drawable.cover_placeholder : coverResId);
            tvStatus.setText(novel.getStatus());
            tvAuthor.setText(String.format("Tác giả: %s", novel.getAuthor()));
            tvType.setText(String.format(Locale.getDefault(), "%s · %d tập", novel.getType(), totalVol));
            tvStatChapters.setText(String.format(Locale.getDefault(), "%d\nChương truyện", totalCh));
            tvStatViews.setText(String.format(Locale.getDefault(), "%.1fK\nLượt đọc", novel.getAllViews() / 1000.0));
            tvChaptersSummary.setText(String.format(Locale.getDefault(), "%d tập · %d chương", totalVol, totalCh));
            tvAuthorCardName.setText(novel.getAuthor());
            if (!novel.getAuthor().isEmpty()) {
                tvAuthorAvatar.setText(novel.getAuthor().substring(0, 1).toUpperCase(Locale.getDefault()));
            }
            if (novel.getGenres() != null) {
                for (String genre : novel.getGenres()) {
                    Chip chip = new Chip(requireContext());
                    chip.setText(genre);
                    chip.setClickable(false);
                    chipGroupGenres.addView(chip);
                }
            }
        }

        tvToggle.setOnClickListener(v -> {
            expanded = !expanded;
            tvSynopsis.setMaxLines(expanded ? Integer.MAX_VALUE : 4);
            tvToggle.setText(expanded ? "Thu gọn" : "Xem thêm");
        });

        view.findViewById(R.id.btnDetailBack).setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed()
        );
        getChildFragmentManager().setFragmentResultListener(WalletBottomSheet.UPDATED, getViewLifecycleOwner(),
                (key, result) -> refreshWallet());
        btnDetailWallet.setOnClickListener(v -> WalletBottomSheet.open(getChildFragmentManager()));
        View bundleOffer = (View) view.findViewById(R.id.tvSeeBundlePrice).getParent();
        bundleOffer.setVisibility(db.hasPaidBundle(novelId) ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.tvSeeBundlePrice).setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Mở khóa chương 16–18")
                        .setMessage("Mở lẻ: 10 xu / chương.\nMua trọn gói 3 chương: 25 xu.\nSố dư hiện tại: " + wallet.getBalance() + " xu.")
                        .setPositiveButton("Mở gói · " + wallet.bundlePrice(novelId) + " xu", (d, w) -> {
                            if (wallet.unlockBundle16To18(novelId)) {
                                Toast.makeText(requireContext(), "Đã mở khóa cả 3 chương!", Toast.LENGTH_SHORT).show();
                                refreshWallet();
                            } else {
                                Toast.makeText(requireContext(), "Không đủ xu!", Toast.LENGTH_SHORT).show();
                                WalletBottomSheet.open(getChildFragmentManager());
                            }
                        })
                        .setNegativeButton("Đóng", null)
                        .show()
        );

        ChapterDropdown dropdown = view.findViewById(R.id.chapterDropdown);
        View chevron = view.findViewById(R.id.chapterChevron);
        view.findViewById(R.id.cardChapters).setOnClickListener(v -> {
            dropdown.toggle(db, wallet, novelId, db.getCurrentChapter(novelId), chapter -> {
                chevron.setRotation(0f);
                startActivity(new Intent(requireContext(), ReaderActivity.class)
                        .putExtra("novel_id", novelId).putExtra("chapter_number", chapter));
            });
            chevron.setRotation(dropdown.isShown() ? 180f : 0f);
        });
        // mo trang doc o chuong dang doc do
        btnReadContinue.setOnClickListener(v -> {
            int ch = db.getCurrentChapter(novelId);
            Intent intent = new Intent(requireContext(), ReaderActivity.class);
            intent.putExtra("novel_id", novelId);
            intent.putExtra("chapter_number", ch);
            startActivity(intent);
        });
        BookmarkRepository bookmarks = new BookmarkRepository(requireContext());
        bookmarks.observeChapters(novelId).observe(getViewLifecycleOwner(), chapters -> {
            bookmarkedChapters = chapters;
            int chapter = db.getCurrentChapter(novelId);
            updateBookmarkButton(chapter, chapters.contains(chapter));
        });
        btnBookmarkChapter.setOnClickListener(v -> {
            int chapter = db.getCurrentChapter(novelId);
            bookmarks.toggle(novelId, chapter, bookmarked -> {
                if (getView() != view) return;
                if (chapter == db.getCurrentChapter(novelId)) updateBookmarkButton(chapter, bookmarked);
                int message = bookmarked ? R.string.bookmark_added : R.string.bookmark_removed;
                Toast.makeText(requireContext(), getString(message, chapter), Toast.LENGTH_SHORT).show();
            }, () -> {
                if (getView() == view) {
                    Toast.makeText(requireContext(), R.string.bookmark_update_failed, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        int ch = db.getCurrentChapter(novelId);
        tvResumeChapter.setText(String.format(Locale.getDefault(), "Bạn đang đọc chương %d", ch));
        btnReadContinue.setText(String.format(Locale.getDefault(), "Đọc tiếp · Chương %d", ch));
        updateBookmarkButton(ch, bookmarkedChapters.contains(ch));
        updateShelfButton(db.isOnShelf(novelId));
        refreshWallet();
    }

    private void refreshWallet() {
        btnDetailWallet.setText(String.format(Locale.getDefault(), "%d xu", wallet.getBalance()));
    }

    private void updateBookmarkButton(int chapter, boolean bookmarked) {
        btnBookmarkChapter.setIconResource(bookmarked ? R.drawable.ic_bookmark : R.drawable.ic_bookmark_outline);
        int action = bookmarked ? R.string.bookmark_remove : R.string.bookmark_add;
        btnBookmarkChapter.setContentDescription(getString(action, chapter));
    }

    private void updateShelfButton(boolean onShelf) {
        btnToggleShelf.setText(onShelf ? R.string.library_shelf_remove : R.string.library_shelf_add);
        btnToggleShelf.setIconResource(onShelf ? android.R.drawable.checkbox_on_background : R.drawable.ic_book);
    }

    private void setupFollowAndShare(View root, MaterialButton followButton, View shareButton, Novel novel) {
        FollowRepository follows = new FollowRepository(requireContext());
        follows.observeFollowed(novelId).observe(getViewLifecycleOwner(), followed -> {
            followButton.setText(Boolean.TRUE.equals(followed) ? R.string.novel_unfollow : R.string.novel_follow);
            followButton.setEnabled(true);
        });
        followButton.setOnClickListener(v -> follows.toggle(novelId, () -> {
            if (getView() == root) {
                Toast.makeText(requireContext(), R.string.novel_follow_failed, Toast.LENGTH_SHORT).show();
            }
        }));
        shareButton.setOnClickListener(v -> {
            String description = getString(R.string.novel_share_description, novel.getAuthor(),
                    novel.getType(), novel.getStatus());
            Intent share = new Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, novel.getName() + "\n" + description);
            startActivity(Intent.createChooser(share, getString(R.string.novel_share)));
        });
    }
}
