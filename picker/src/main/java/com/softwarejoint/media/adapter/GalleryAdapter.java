package com.softwarejoint.media.adapter;

import android.database.Cursor;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.softwarejoint.media.R;
import com.softwarejoint.media.camera.CameraFragment;
import com.softwarejoint.media.fileio.ImageLoader;

import java.util.ArrayList;
import java.util.List;

import io.github.ypdieguez.cursorrecycleradapter.CursorRecyclerAdapter;

/**
 * Created by Pankaj Soni <pankajsoni@softwarejoint.com> on 02/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
public class GalleryAdapter extends CursorRecyclerAdapter<ViewHolder> {

    private static final String TAG = "GalleryAdapter";

    private int colIdIndex = 0;
    private int colDataIndex = 1;
    private final int maxSelection;
    private final CameraFragment cameraFragment;

    private List<String> selected = new ArrayList<>();

    public GalleryAdapter(Cursor cursor, int count, CameraFragment fragment) {
        super(cursor);
        colIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
        colDataIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        maxSelection = count;
        cameraFragment = fragment;
    }

    public void addSelected(String filePath) {
        selected.add(filePath);

        int count = 0;
        for (String file: selected) {
            Log.d(TAG, "count : " + (++count) +  " selected: " + file);
        }

        notifyDataSetChanged();
    }

    public int getSelectionCount() {
        return selected.size();
    }

    public void fill(ArrayList<String> items) {
        items.addAll(selected);
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_video, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, Cursor cursor) {
        if (cursor == null || cursor.isClosed()) { return; }

        final long itemId = cursor.getLong(colIdIndex);
        final String mediaPath = cursor.getString(colDataIndex);

        Log.d(TAG, "mediaPath: " + mediaPath);

        ImageLoader.with(itemId).loadInto(holder.iv_image);

        GalleryClickListener clickListener = new GalleryClickListener(mediaPath, holder);

        if (selected.contains(mediaPath)) {
            holder.iv_select.setVisibility(View.VISIBLE);
        } else {
            holder.iv_select.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(clickListener);
    }

    private class GalleryClickListener implements View.OnClickListener {

        private String mediaPath;
        private ViewHolder viewHolder;

        GalleryClickListener(String mediaPath, ViewHolder holder) {
            this.mediaPath = mediaPath;
            this.viewHolder = holder;
        }

        @Override
        public void onClick(View v) {
            if (selected.contains(mediaPath)) {
                selected.remove(mediaPath);
                viewHolder.iv_select.setVisibility(View.GONE);
            } else if (selected.size() < maxSelection) {
                viewHolder.iv_select.setVisibility(View.VISIBLE);
                selected.add(mediaPath);
            }

            cameraFragment.onMediaSelectionUpdated();
        }
    }
}