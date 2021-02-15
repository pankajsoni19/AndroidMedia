package com.pankajsoni19.media.adapter;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

import com.pankajsoni19.media.R;

/**
 * Created by Pankaj Soni <pankajsoni19@live.com> on 03/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
class ViewHolder extends RecyclerView.ViewHolder {

    ImageView iv_image;
    ImageView iv_select;

    ViewHolder(View v) {
        super(v);
        iv_image = v.findViewById(R.id.iv_video);
        iv_select = v.findViewById(R.id.iv_select);
    }
}
