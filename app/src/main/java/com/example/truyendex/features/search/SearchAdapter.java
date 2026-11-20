package com.example.truyendex.features.search;

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

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {

    public interface OnNovelClickListener {
        void onNovelClick(Novel novel);
    }

    private List<Novel> novels = Collections.emptyList();
    private final OnNovelClickListener listener;

    public SearchAdapter(OnNovelClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Novel> list) {
        this.novels = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_story, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Novel novel = novels.get(position);
        holder.title.setText(novel.getName());
        holder.genres.setText(novel.getGenresText());
        holder.info.setText(holder.itemView.getContext().getString(
                R.string.home_story_info, novel.getChapters(), novel.getStatus()));

        int resId = novel.getCoverImageResId();
        holder.cover.setImageResource(resId == 0 ? R.drawable.cover_placeholder : resId);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNovelClick(novel);
            }
        });
    }

    @Override
    public int getItemCount() {
        return novels.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView cover;
        final TextView title;
        final TextView genres;
        final TextView info;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.homeNovelCover);
            title = itemView.findViewById(R.id.homeNovelTitle);
            genres = itemView.findViewById(R.id.homeNovelGenres);
            info = itemView.findViewById(R.id.homeNovelInfo);
        }
    }
}
