package com.serenegiant.audiovideosample;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.serenegiant.fileio.ImageLoader;
import com.serenegiant.mediaaudiotest.R;

import java.util.ArrayList;
import java.util.List;

import io.github.ypdieguez.cursorrecycleradapter.CursorRecyclerAdapter;

/**
 * Created by Pankaj Soni <pankajsoni@softwarejoint.com> on 02/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
public class GalleryAdapter extends CursorRecyclerAdapter<GalleryAdapter.ViewHolder> {

    private static final String TAG = "GalleryAdapter";

    private static final int MAX_SELECTION = 1;

    private int colIdIndex = 0;
    private int colDataIndex = 1;
    private int colthumbIndex = 2;
    //private Cursor cursor;
    private ContentResolver resolver;
    private int maxSelection = MAX_SELECTION;

    private List<String> selected = new ArrayList<>();

    GalleryAdapter(Context context, Cursor cursor) {
        super(cursor);
        colIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
        colDataIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        colthumbIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Thumbnails.DATA);
        resolver = context.getContentResolver();

    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView iv_image;
        ImageView iv_select;

        ViewHolder(View v) {
            super(v);
            iv_image = v.findViewById(R.id.iv_video);
            iv_select = v.findViewById(R.id.iv_select);
        }
    }

    @Override
    public GalleryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_video, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, Cursor cursor) {
        if (cursor == null || cursor.isClosed()) { return; }

        final long itemId = cursor.getLong(colIdIndex);
        final String mediaPath = cursor.getString(colDataIndex);

        ImageLoader.with(itemId).loadInto(holder.iv_image);

        GalleryClickListener clickListener = new GalleryClickListener(mediaPath, holder);

        if (selected.contains(mediaPath)) {
            holder.iv_select.setVisibility(View.VISIBLE);
        } else {
            holder.iv_select.setVisibility(View.GONE);
        }

        holder.iv_image.setOnClickListener(clickListener);
        holder.iv_select.setOnClickListener(clickListener);
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
            int position = viewHolder.getAdapterPosition();

            if (selected.contains(mediaPath)) {
                selected.remove(position);
                viewHolder.iv_select.setVisibility(View.GONE);
            } else if (selected.size() < maxSelection) {
                selected.add(mediaPath);

                if (selected.size() == maxSelection) {
                    onMaxSelection();
                } else {
                    viewHolder.iv_select.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void onMaxSelection() {
        Log.d(TAG, "onMaxSelection");
    }
}