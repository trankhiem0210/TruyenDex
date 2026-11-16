package com.example.truyendex.features.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.truyendex.R;
import com.example.truyendex.data.model.Novel;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

final class HomeAdapter extends RecyclerView.Adapter<HomeAdapter.NovelHolder> {

    private final int layout;
    private List<Novel> novels = Collections.emptyList();
    private String rankingPeriod;

    HomeAdapter(int layout) {
        this.layout = layout;
    }

    void show(List<Novel> novels, String rankingPeriod) {
        this.novels = novels;
        this.rankingPeriod = rankingPeriod;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NovelHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NovelHolder(LayoutInflater.from(parent.getContext()).inflate(layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull NovelHolder holder, int position) {
        Novel novel = novels.get(position);
        if (layout == R.layout.item_story) {
            String info = holder.itemView.getContext().getString(
                    R.string.home_story_info, novel.getChapters(), novel.getStatus());
            bindNovel(holder.itemView, novel, info);
            TextView genres = holder.itemView.findViewById(R.id.homeNovelGenres);
            genres.setText(novel.getGenresText());
        } else {
            String info = holder.itemView.getContext().getString(
                    R.string.views_count, novel.getFormattedViews(rankingPeriod));
            bindNovel(holder.itemView, novel, info);
            TextView rank = holder.itemView.findViewById(R.id.homeNovelRank);
            rank.setText(String.format(Locale.getDefault(), "%d", position + 1));
            rank.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return novels.size();
    }

    static void bindNovel(View view, Novel novel, String info) {
        TextView title = view.findViewById(R.id.homeNovelTitle);
        TextView subtitle = view.findViewById(R.id.homeNovelInfo);
        title.setText(novel.getName());
        subtitle.setText(info);
        bindCover(view.findViewById(R.id.homeNovelCover), novel.getCoverImageResId());
    }

    static void bindCover(ImageView cover, int imageResId) {
        cover.setImageResource(imageResId == 0 ? R.drawable.cover_placeholder : imageResId);
    }

    static final class NovelHolder extends RecyclerView.ViewHolder {
        NovelHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
