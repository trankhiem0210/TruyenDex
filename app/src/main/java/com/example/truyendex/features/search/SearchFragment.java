package com.example.truyendex.features.search;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.truyendex.R;
import com.example.truyendex.data.model.Novel;
import com.example.truyendex.data.repository.NovelRepository;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.Collator;
import static com.example.truyendex.core.SearchText.normalize;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SearchFragment extends Fragment {

    private final NovelRepository repository = NovelRepository.getInstance();

    private EditText edtQuery;
    private HorizontalScrollView scrollGenres;
    private ChipGroup chipGroupGenres;
    private TextView tvSummary;
    private RecyclerView rvGrid;
    private View layoutEmpty;
    private MaterialButton btnLoadMore;
    private SearchAdapter adapter;

    private String query = "";
    private String authorFilter = "";
    private String artistFilter = "";
    private String statusFilter = "";
    private String typeFilter = "";
    private String sortBy = "name";
    private final List<String> selectedGenres = new ArrayList<>();
    private int visibleCount = 6;

    public SearchFragment() {
        super(R.layout.fragment_search);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        edtQuery = view.findViewById(R.id.edtExploreQuery);
        scrollGenres = view.findViewById(R.id.scrollExploreGenres);
        chipGroupGenres = view.findViewById(R.id.chipGroupExploreGenres);
        tvSummary = view.findViewById(R.id.tvExploreSummary);
        rvGrid = view.findViewById(R.id.rvExploreGrid);
        layoutEmpty = view.findViewById(R.id.layoutExploreEmpty);
        btnLoadMore = view.findViewById(R.id.btnExploreLoadMore);

        adapter = new SearchAdapter(novel -> {
            Bundle args = new Bundle();
            args.putInt("novel_id", novel.getId());
            Navigation.findNavController(view).navigate(R.id.open_novel_detail, args);
        });
        rvGrid.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rvGrid.setAdapter(adapter);

        edtQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                query = s != null ? s.toString().trim() : "";
                visibleCount = 6;
                renderExplore();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        View.OnClickListener openFilters = v -> showFilterSheet();
        view.findViewById(R.id.btnOpenFilterIcon).setOnClickListener(openFilters);
        view.findViewById(R.id.tvOpenFilterText).setOnClickListener(openFilters);

        view.findViewById(R.id.btnClearFilters).setOnClickListener(v -> {
            query = "";
            authorFilter = "";
            artistFilter = "";
            statusFilter = "";
            typeFilter = "";
            sortBy = "name";
            selectedGenres.clear();
            visibleCount = 6;
            edtQuery.setText("");
            renderExplore();
        });

        btnLoadMore.setOnClickListener(v -> {
            visibleCount += 6;
            renderExplore();
        });

        renderExplore();
    }

    private void renderGenreChips() {
        int savedScrollX = scrollGenres.getScrollX();
        chipGroupGenres.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        Chip allChip = (Chip) inflater.inflate(R.layout.item_home_genre, chipGroupGenres, false);
        allChip.setText("Tất cả");
        allChip.setChecked(selectedGenres.isEmpty());
        allChip.setOnClickListener(v -> {
            selectedGenres.clear();
            visibleCount = 6;
            renderExplore();
        });
        chipGroupGenres.addView(allChip);

        for (String genre : repository.getGenres()) {
            Chip chip = (Chip) inflater.inflate(R.layout.item_home_genre, chipGroupGenres, false);
            chip.setText(genre);
            chip.setChecked(selectedGenres.contains(genre));
            chip.setOnClickListener(v -> {
                if (selectedGenres.contains(genre)) {
                    selectedGenres.remove(genre);
                } else {
                    selectedGenres.add(genre);
                }
                visibleCount = 6;
                renderExplore();
            });
            chipGroupGenres.addView(chip);
        }

        scrollGenres.post(() -> scrollGenres.setScrollX(savedScrollX));
    }

    private void renderExplore() {
        List<Novel> all = repository.getAllNovels();
        List<Novel> matched = new ArrayList<>();

        String normQ = normalize(query);
        String normAuthor = normalize(authorFilter);
        String normArtist = normalize(artistFilter);

        for (Novel n : all) {
            boolean okQ = normQ.isEmpty() || normalize(n.getName() + " " + n.getAuthor() + " " + n.getGenresText()).contains(normQ);
            boolean okAuthor = normAuthor.isEmpty() || normalize(n.getAuthor()).contains(normAuthor);
            boolean okArtist = normArtist.isEmpty() || normalize(n.getArtist()).contains(normArtist);
            boolean okStatus = statusFilter.isEmpty() || statusFilter.equals(n.getStatus());
            boolean okType = typeFilter.isEmpty() || typeFilter.equals(n.getType());
            boolean okGenres = selectedGenres.isEmpty() || n.getGenres().containsAll(selectedGenres);

            if (okQ && okAuthor && okArtist && okStatus && okType && okGenres) {
                matched.add(n);
            }
        }

        Collator viCollator = Collator.getInstance(new Locale("vi", "VN"));
        Collections.sort(matched, (a, b) -> {
            if ("updated".equals(sortBy)) {
                return Integer.compare(b.getUpdated(), a.getUpdated());
            }
            if ("views".equals(sortBy)) {
                return Integer.compare(b.getAllViews(), a.getAllViews());
            }
            return viCollator.compare(a.getName(), b.getName());
        });

        int limit = Math.min(visibleCount, matched.size());
        adapter.submitList(new ArrayList<>(matched.subList(0, limit)));

        String genreText = selectedGenres.isEmpty() ? "" : " · " + TextUtils.join(", ", selectedGenres);
        tvSummary.setText(matched.size() + " truyện · Hiển thị " + limit + genreText);

        boolean isEmpty = matched.isEmpty();
        layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvGrid.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        btnLoadMore.setVisibility(visibleCount < matched.size() ? View.VISIBLE : View.GONE);

        renderGenreChips();
    }

    private void showFilterSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheet = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_search_filters, null);
        dialog.setContentView(sheet);

        EditText edtAuthor = sheet.findViewById(R.id.edtFilterAuthor);
        EditText edtArtist = sheet.findViewById(R.id.edtFilterArtist);
        Spinner spStatus = sheet.findViewById(R.id.spinnerFilterStatus);
        Spinner spSort = sheet.findViewById(R.id.spinnerFilterSort);
        RadioGroup rgType = sheet.findViewById(R.id.rgFilterType);
        ChipGroup cgSheet = sheet.findViewById(R.id.chipGroupSheetGenres);

        String[] statuses = {"Tất cả", "Đang tiến hành", "Hoàn thành"};
        spStatus.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, statuses));

        String[] sorts = {"Tên A–Z", "Mới cập nhật", "Xem nhiều"};
        spSort.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, sorts));

        edtAuthor.setText(authorFilter);
        edtArtist.setText(artistFilter);
        spStatus.setSelection("Đang tiến hành".equals(statusFilter) ? 1 : "Hoàn thành".equals(statusFilter) ? 2 : 0);
        spSort.setSelection("updated".equals(sortBy) ? 1 : "views".equals(sortBy) ? 2 : 0);

        if ("Truyện dịch".equals(typeFilter)) rgType.check(R.id.rbTypeTrans);
        else if ("AI dịch".equals(typeFilter)) rgType.check(R.id.rbTypeAi);
        else if ("Sáng tác".equals(typeFilter)) rgType.check(R.id.rbTypeOriginal);
        else rgType.check(R.id.rbTypeAll);

        List<String> draftGenres = new ArrayList<>(selectedGenres);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (String genre : repository.getGenres()) {
            Chip chip = (Chip) inflater.inflate(R.layout.item_home_genre, cgSheet, false);
            chip.setText(genre);
            chip.setChecked(draftGenres.contains(genre));
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (!draftGenres.contains(genre)) draftGenres.add(genre);
                } else {
                    draftGenres.remove(genre);
                }
            });
            cgSheet.addView(chip);
        }

        sheet.findViewById(R.id.btnCloseFilterSheet).setOnClickListener(v -> dialog.dismiss());

        sheet.findViewById(R.id.btnSheetReset).setOnClickListener(v -> {
            edtAuthor.setText("");
            edtArtist.setText("");
            spStatus.setSelection(0);
            spSort.setSelection(0);
            rgType.check(R.id.rbTypeAll);
            draftGenres.clear();
            for (int i = 0; i < cgSheet.getChildCount(); i++) {
                ((Chip) cgSheet.getChildAt(i)).setChecked(false);
            }
        });

        sheet.findViewById(R.id.btnSheetApply).setOnClickListener(v -> {
            authorFilter = edtAuthor.getText().toString().trim();
            artistFilter = edtArtist.getText().toString().trim();

            int stPos = spStatus.getSelectedItemPosition();
            statusFilter = stPos == 1 ? "Đang tiến hành" : stPos == 2 ? "Hoàn thành" : "";

            int sortPos = spSort.getSelectedItemPosition();
            sortBy = sortPos == 1 ? "updated" : sortPos == 2 ? "views" : "name";

            RadioButton rb = sheet.findViewById(rgType.getCheckedRadioButtonId());
            String typeText = rb != null ? rb.getText().toString() : "Tất cả";
            typeFilter = "Tất cả".equals(typeText) ? "" : typeText;

            selectedGenres.clear();
            selectedGenres.addAll(draftGenres);
            visibleCount = 6;
            renderExplore();
            dialog.dismiss();
        });

        dialog.show();
    }

}
