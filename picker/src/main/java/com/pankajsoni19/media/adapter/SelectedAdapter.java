package com.pankajsoni19.media.adapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pankajsoni19.media.R;
import com.pankajsoni19.media.camera.CameraFragment;
import com.pankajsoni19.media.enums.MediaType;
import com.pankajsoni19.media.fileio.ImageLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Pankaj Soni <pankajsoni19@live.com> on 03/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
public class SelectedAdapter extends RecyclerView.Adapter<ViewHolder> {

    private static final String TAG = "SelectedAdapter";

    private List<String> selected = new ArrayList<>();
    private @MediaType
    int mediaType;
    private CameraFragment cameraFragment;

    public SelectedAdapter(@MediaType int mediaType, CameraFragment fragment) {
        this.mediaType = mediaType;
        cameraFragment = fragment;
    }

    private String getItem(int position) {
        return selected.get(position);
    }

    @Override
    public int getItemCount() {
        return selected.size();
    }

    public void addSelected(String filePath) {
        selected.add(filePath);

        int count = 0;
        for (String file : selected) {
            Log.d(TAG, "count : " + (++count) + " selected: " + file);
        }

        notifyDataSetChanged();
    }

    public int getSelectionCount() {
        return selected.size();
    }

    public void fill(ArrayList<String> items) {
        items.addAll(selected);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_video, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final String mediaPath = getItem(position);
        Log.d(TAG, "mediaPath: " + mediaPath);

        ImageLoader.load(mediaPath).withMediaHint(mediaType).into(holder.iv_image);

        ClickListener clickListener = new ClickListener(mediaPath);

        holder.iv_select.setVisibility(View.VISIBLE);

        holder.itemView.setOnClickListener(clickListener);
    }

    public void clearSelection() {
        selected.clear();
    }

    private class ClickListener implements View.OnClickListener {

        private final String mediaPath;

        ClickListener(String mediaPath) {
            this.mediaPath = mediaPath;
        }

        @Override
        public void onClick(View v) {
            selected.remove(mediaPath);
            notifyDataSetChanged();

            cameraFragment.checkIfMediaSelectionCompleted();
        }
    }
}