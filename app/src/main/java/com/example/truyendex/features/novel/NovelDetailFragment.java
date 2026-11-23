package com.example.truyendex.features.novel;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.truyendex.R;
import com.example.truyendex.data.repository.WalletRepository;
import com.example.truyendex.features.wallet.WalletBottomSheet;
import com.example.truyendex.data.local.AppDatabase;
import com.example.truyendex.data.model.Novel;
import com.example.truyendex.data.repository.NovelRepository;
import com.example.truyendex.features.reader.ReaderActivity;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.Locale;

public class NovelDetailFragment extends Fragment {
    private int novelId = 1;
    private boolean expanded = false;
    private AppDatabase db;
    private WalletRepository wallet;
    private SharedPreferences prefs;

    private TextView tvResumeChapter;
    private Button btnDetailWallet, btnReadContinue, btnSaveNovel;

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
        prefs = requireContext().getSharedPreferences("truyendex_prefs", Context.MODE_PRIVATE);

        // uu tien lay thong tin truyen tu db, neu khong co moi lay tu repository
        Novel novel = db.getNovelByIdFromDb(novelId);
        if (novel == null) {
            novel = NovelRepository.getInstance().getNovelById(novelId);
        }
        int dbChapterCount = db.getTotalChapters(novelId);

        TextView tvTitle = view.findViewById(R.id.tvNovelTitle);
        TextView tvCoverTitle = view.findViewById(R.id.tvCoverTitle);
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
        btnSaveNovel = view.findViewById(R.id.btnSaveNovel);

        if (novel != null) {
            int totalCh = dbChapterCount > 0 ? dbChapterCount : novel.getChapters();
            int totalVol = ((totalCh - 1) / 10) + 1;

            tvTitle.setText(novel.getName());
            tvCoverTitle.setText(novel.getName());
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
        // luu truyen vao thu vien bang sharedpreferences
        btnSaveNovel.setOnClickListener(v -> {
            boolean saved = !prefs.getBoolean("saved_" + novelId, false);
            prefs.edit().putBoolean("saved_" + novelId, saved).apply();
            updateSaveButton();
            Toast.makeText(requireContext(), saved ? "Đã lưu truyện vào thư viện" : "Đã bỏ lưu truyện", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        int ch = db.getCurrentChapter(novelId);
        tvResumeChapter.setText(String.format(Locale.getDefault(), "Bạn đang đọc chương %d", ch));
        btnReadContinue.setText(String.format(Locale.getDefault(), "Đọc tiếp · Chương %d", ch));
        updateSaveButton();
        refreshWallet();
    }

    private void refreshWallet() {
        btnDetailWallet.setText(String.format(Locale.getDefault(), "%d xu", wallet.getBalance()));
    }

    private void updateSaveButton() {
        boolean saved = prefs.getBoolean("saved_" + novelId, false);
        btnSaveNovel.setText(saved ? "Đã lưu" : "Lưu");
    }
}
