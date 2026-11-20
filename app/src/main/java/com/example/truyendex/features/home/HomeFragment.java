package com.example.truyendex.features.home;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.truyendex.R;
import com.example.truyendex.core.ThemeManager;
import com.example.truyendex.data.local.AppDatabase;
import com.example.truyendex.data.model.Chapter;
import com.example.truyendex.data.model.Novel;
import com.example.truyendex.data.repository.NovelRepository;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private static final int CONTINUE_NOVEL_ID = 1;
    private static final String RANKING_BUTTON = "rankingButton";
    private static final String SELECTED_GENRE = "selectedGenre";
    private static final String SHOW_ALL_STORIES = "showAllStories";

    private final NovelRepository repository = NovelRepository.getInstance();
    private AppDatabase db;
    private int continueNovelId = CONTINUE_NOVEL_ID;
    private int rankingButton = R.id.homeRankWeek;
    private String selectedGenre;
    private boolean showAllStories;

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            rankingButton = savedInstanceState.getInt(RANKING_BUTTON, R.id.homeRankWeek);
            selectedGenre = savedInstanceState.getString(SELECTED_GENRE);
            showAllStories = savedInstanceState.getBoolean(SHOW_ALL_STORIES);
        }

        // khoi tao db va nap truyen kiem lai tu assets neu chua co
        db = new AppDatabase(requireContext());
        db.importKiemLaiIfEmpty(requireContext(), 1);
        repository.loadNovelsFromDb(db);

        setupThemeToggle(view);
        setupHeaderActions(view);
        loadContinueCard(view);
        setupFeatured(view);
        setupRanking(view);
        setupSearch(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null && db != null) {
            repository.loadNovelsFromDb(db);
            loadContinueCard(getView());
        }
    }

    // hien thi thong tin truyen dang doc do tren trang chu
    private void loadContinueCard(View view) {
        List<Integer> historyIds = db.getReadHistoryNovelIds();
        continueNovelId = historyIds.isEmpty() ? CONTINUE_NOVEL_ID : historyIds.get(0);

        Novel continueNovel = db.getNovelByIdFromDb(continueNovelId);
        if (continueNovel == null) {
            continueNovel = repository.getNovelById(continueNovelId);
        }
        if (continueNovel == null) return;

        HomeAdapter.bindCover(view.findViewById(R.id.continueCover), continueNovel.getCoverImageResId());

        TextView tvTitle = view.findViewById(R.id.continueTitle);
        TextView tvChapter = view.findViewById(R.id.continueChapter);
        ProgressBar progressBar = view.findViewById(R.id.continueProgress);
        TextView btnContinue = view.findViewById(R.id.btnContinueReading);

        tvTitle.setText(continueNovel.getName());

        int currentCh = db.getCurrentChapter(continueNovelId);
        int totalCh = db.getTotalChapters(continueNovelId);
        if (totalCh <= 0) totalCh = continueNovel.getChapters();

        Chapter chapter = db.getChapterByNumber(continueNovelId, currentCh);
        if (chapter != null && chapter.getTitle() != null) {
            tvChapter.setText(chapter.getTitle());
        } else {
            tvChapter.setText("Chương " + currentCh);
        }

        // tinh phan tram tien do doc
        int percent = totalCh > 0 ? (currentCh * 100) / totalCh : 0;
        if (percent < 1) percent = 1;
        progressBar.setProgress(percent);
        btnContinue.setText(getString(R.string.read_continue, percent));
    }

    private void setupThemeToggle(View view) {
        MaterialButtonToggleGroup toggle = view.findViewById(R.id.homeThemeToggle);
        String current = ThemeManager.getTheme(requireContext());
        if (ThemeManager.DARK.equals(current)) {
            toggle.check(R.id.homeThemeDark);
        } else if (ThemeManager.SEPIA.equals(current)) {
            toggle.check(R.id.homeThemeSepia);
        } else {
            toggle.check(R.id.homeThemeLight);
        }

        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            String theme = ThemeManager.LIGHT;
            if (checkedId == R.id.homeThemeDark) {
                theme = ThemeManager.DARK;
            } else if (checkedId == R.id.homeThemeSepia) {
                theme = ThemeManager.SEPIA;
            }
            ThemeManager.setTheme(requireActivity(), theme);
        });
    }

    private void setupHeaderActions(View view) {
        view.findViewById(R.id.btnNotifications).setOnClickListener(button -> {
            view.findViewById(R.id.notificationDot).setVisibility(View.GONE);
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.home_notifications)
                    .setMessage(R.string.home_no_notifications)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        });
        view.findViewById(R.id.btnToc).setOnClickListener(button -> {
            int currentCh = db != null ? db.getCurrentChapter(continueNovelId) : 1;
            Chapter ch = db != null ? db.getChapterByNumber(continueNovelId, currentCh) : null;
            String label = (ch != null && ch.getTitle() != null && !ch.getTitle().isEmpty())
                    ? ch.getTitle()
                    : getString(R.string.home_continue_chapter);
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.table_of_contents)
                    .setItems(new CharSequence[]{label},
                            (dialog, position) -> openNovelDetail(view, repository.getNovelById(continueNovelId)))
                    .show();
        });
        view.findViewById(R.id.btnContinueReading).setOnClickListener(button ->
                openNovelDetail(view, repository.getNovelById(continueNovelId)));
        view.findViewById(R.id.continueCover).setOnClickListener(button ->
                openNovelDetail(view, repository.getNovelById(continueNovelId)));
    }

    private void setupFeatured(View view) {
        RecyclerView featured = view.findViewById(R.id.homeFeatured);
        HomeAdapter adapter = new HomeAdapter(R.layout.item_story, novel -> openNovelDetail(view, novel));
        featured.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        featured.setAdapter(adapter);

        TextView discover = view.findViewById(R.id.btnDiscoverMore);
        discover.setText(showAllStories ? R.string.home_show_less : R.string.discover_more);
        discover.setOnClickListener(button -> {
            showAllStories = !showAllStories;
            discover.setText(showAllStories ? R.string.home_show_less : R.string.discover_more);
            if (!showAllStories) {
                ((ChipGroup) view.findViewById(R.id.homeGenres)).clearCheck();
            }
            updateFeatured(adapter);
        });

        ChipGroup genres = view.findViewById(R.id.homeGenres);
        LayoutInflater inflater = LayoutInflater.from(view.getContext());
        for (String genre : repository.getGenres()) {
            Chip chip = (Chip) inflater.inflate(R.layout.item_home_genre, genres, false);
            chip.setId(View.generateViewId());
            chip.setText(genre);
            genres.addView(chip);
            if (genre.equals(selectedGenre)) {
                genres.check(chip.getId());
            }
        }
        genres.setOnCheckedStateChangeListener((group, checkedIds) -> {
            selectedGenre = null;
            if (!checkedIds.isEmpty()) {
                Chip chip = group.findViewById(checkedIds.get(0));
                selectedGenre = chip.getText().toString();
            }
            updateFeatured(adapter);
        });
        updateFeatured(adapter);
    }

    private void updateFeatured(HomeAdapter adapter) {
        List<Novel> novels;
        if (selectedGenre != null) {
            novels = new ArrayList<>();
            for (Novel novel : repository.getAllNovels()) {
                if (novel.getGenres().contains(selectedGenre)) {
                    novels.add(novel);
                }
            }
        } else {
            novels = showAllStories ? repository.getAllNovels() : repository.getFeaturedNovels();
        }
        adapter.show(novels, null);
    }

    private void setupRanking(View view) {
        RecyclerView ranking = view.findViewById(R.id.homeRanking);
        HomeAdapter adapter = new HomeAdapter(R.layout.item_ranking, novel -> openNovelDetail(view, novel));
        ranking.setLayoutManager(new LinearLayoutManager(requireContext()));
        ranking.setAdapter(adapter);

        MaterialButtonToggleGroup tabs = view.findViewById(R.id.homeRankingTabs);
        tabs.check(rankingButton);
        updateRanking(tabs, adapter);
        tabs.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                rankingButton = checkedId;
                updateRanking(group, adapter);
            }
        });
    }

    private void updateRanking(MaterialButtonToggleGroup tabs, HomeAdapter adapter) {
        String period = "week";
        if (rankingButton == R.id.homeRankMonth) {
            period = "month";
        } else if (rankingButton == R.id.homeRankAll) {
            period = "all";
        }
        for (int index = 0; index < tabs.getChildCount(); index++) {
            TextView tab = (TextView) tabs.getChildAt(index);
            tab.setTypeface(tab.getTypeface(), tab.getId() == rankingButton ? Typeface.BOLD : Typeface.NORMAL);
        }
        adapter.show(repository.getRanking(period), period);
    }

    private void setupSearch(View view) {
        EditText search = view.findViewById(R.id.homeSearch);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                showSearchResults(view, text.toString().trim().toLowerCase(Locale.ROOT));
            }

            @Override
            public void afterTextChanged(Editable text) {}
        });
    }

    private void showSearchResults(View root, String query) {
        boolean searching = !query.isEmpty();
        root.findViewById(R.id.homeSections).setVisibility(searching ? View.GONE : View.VISIBLE);
        root.findViewById(R.id.homeSearchResults).setVisibility(searching ? View.VISIBLE : View.GONE);
        if (!searching) {
            return;
        }

        LinearLayout matches = root.findViewById(R.id.homeMatches);
        matches.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(root.getContext());
        for (Novel novel : repository.getAllNovels()) {
            String searchableText = novel.getName() + "\n" + novel.getAuthor() + "\n" + novel.getGenresText();
            if (searchableText.toLowerCase(Locale.ROOT).contains(query)) {
                View row = inflater.inflate(R.layout.item_ranking, matches, false);
                HomeAdapter.bindNovel(row, novel, novel.getAuthor());
                row.setOnClickListener(v -> openNovelDetail(v, novel));
                matches.addView(row);
            }
        }
        boolean hasMatches = matches.getChildCount() > 0;
        matches.setVisibility(hasMatches ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.homeNoResults).setVisibility(hasMatches ? View.GONE : View.VISIBLE);
    }

    private void openNovelDetail(View anchorView, Novel novel) {
        if (novel == null) return;
        Bundle args = new Bundle();
        args.putInt("novel_id", novel.getId());
        Navigation.findNavController(anchorView).navigate(R.id.nav_novel_detail, args);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(RANKING_BUTTON, rankingButton);
        outState.putString(SELECTED_GENRE, selectedGenre);
        outState.putBoolean(SHOW_ALL_STORIES, showAllStories);
    }
}
