package com.softwarejoint.media.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

import com.softwarejoint.media.R;

/**
 * Created by Pankaj Soni <pankajsoni@softwarejoint.com> on 03/03/18.
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
