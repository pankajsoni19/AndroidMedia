package com.serenegiant.audiovideosample;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
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

import java.util.List;

import io.github.ypdieguez.cursorrecycleradapter.CursorRecyclerAdapter;

/**
 * Created by Pankaj Soni <pankajsoni@softwarejoint.com> on 02/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
public class GalleryAdapter extends CursorRecyclerAdapter<GalleryAdapter.ViewHolder> {

    /**
     * Constructor.
     *
     * @param c The cursor from which to get the data.
     */
    private int colIdIndex = 0;
    private int colDataIndex = 1;
    private int thumbIndex = 2;
    private Cursor cursor;

    public GalleryAdapter(Cursor cursor) {
        super(cursor);
        colIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
        colDataIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        thumbIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Thumbnails.DATA);
        this.cursor = cursor;
    }

    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        ImageView iv_image;
        ImageView iv_select;

        ViewHolder(View v) {
            super(v);
            iv_image = v.findViewById(R.id.iv_video);
            iv_select = v.findViewById(R.id.iv_select);
            iv_image.setOnClickListener(this);
            iv_select.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            getAdapterPosition()
        }

        oncli
    }

    @Override
    public GalleryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_video, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, Cursor cursor) {



        String mediaPath = cursor.getString(colDataIndex);
        cursor.getPosition();
        Log.e("Column", absolutePathOfImage);
        Log.e("Folder", cursor.getString(column_index_folder_name));
        Log.e("column_id", cursor.getString(column_id));
        Log.e("thum", cursor.getString(thum));

        Model_Video obj_model = new Model_Video();
        obj_model.setBoolean_selected(false);
        obj_model.setStr_path(absolutePathOfImage);
        obj_model.setStr_thumb(cursor.getString(thum));

        al_video.add(obj_model);

        ImageLoader.with()


        Vholder.rl_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent_gallery = new Intent(context, Activity_galleryview.class);
                intent_gallery.putExtra("video", al_video.get(position).getStr_path());
                activity.startActivity(intent_gallery);

            }
        });

    }
}