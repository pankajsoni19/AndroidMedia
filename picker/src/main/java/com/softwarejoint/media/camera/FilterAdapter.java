package com.softwarejoint.media.camera;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.softwarejoint.media.R;
import com.softwarejoint.media.glutils.GL1977Filter;
import com.softwarejoint.media.glutils.GLArtFilter;
import com.softwarejoint.media.glutils.GLColorInvertFilter;
import com.softwarejoint.media.glutils.GLDrawer2D;
import com.softwarejoint.media.glutils.GLGrayscaleFilter;
import com.softwarejoint.media.glutils.GLPosterizeFilter;

/**
 * Created by Pankaj Soni <pankajsoni@softwarejoint.com> on 02/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
class FilterAdapter extends BaseAdapter {

    static int[] items = {
            R.drawable.effect_1,
            R.drawable.effect_1,
            R.drawable.effect_1,
            R.drawable.effect_1,
            R.drawable.effect_1,
            R.drawable.effect_1
    };

    private int selectedEffect = 0;

    @Override
    public int getCount() {
        return items.length;
    }

    @Override
    public GLDrawer2D getItem(int position) {
        switch (position) {
            case 1:
                return new GLPosterizeFilter();
            case 2:
                return new GLGrayscaleFilter();
            case 3:
                return new GLArtFilter();
            case 4:
                return new GL1977Filter();
            case 5:
                return new GLColorInvertFilter();
            case 0:
            default:
                return new GLDrawer2D();
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_video, parent, false);
            convertView.setTag(new ViewHolder(convertView));
        }

        ViewHolder holder = (ViewHolder) convertView.getTag();

        holder.iv_video.setImageResource(items[position]);

        if (position == selectedEffect) {
            holder.iv_select.setVisibility(View.VISIBLE);
        } else {
            holder.iv_select.setVisibility(View.INVISIBLE);
        }

        return convertView;
    }

    void markSelected(int position) {
        selectedEffect = position;
        notifyDataSetChanged();
    }

    private static class ViewHolder {

        private ImageView iv_video;
        private ImageView iv_select;

        ViewHolder(View convertView) {
            iv_video = convertView.findViewById(R.id.iv_video);
            iv_select = convertView.findViewById(R.id.iv_select);
        }
    }
}
