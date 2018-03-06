package com.softwarejoint.media.camera;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.GridView;

import com.softwarejoint.media.R;
import com.softwarejoint.media.adapter.FilterAdapter;
import com.softwarejoint.media.anim.AnimFadeReveal;
import com.softwarejoint.media.glutils.GLDrawer2D;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;

/**
 * Created by Pankaj Soni <pankajsoni@softwarejoint.com> on 03/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
public class FilterPreviewDialogFragment extends DialogFragment {

    public final String TAG = "FilterPreviewDialog";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //noinspection ConstantConditions
        return new Dialog(getActivity(), getTheme()) {
            @Override
            public void onBackPressed() {
                dismissView();
            }
        };
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, R.style.AppTheme);
    }

    private int selectedIdx = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        //noinspection ConstantConditions
        Window activityWindow = getActivity().getWindow();

        if (activityWindow != null) {
            activityWindow.setLayout(MATCH_PARENT, MATCH_PARENT);
        }

        Window dialogWindow = getDialog().getWindow();

        if (dialogWindow != null) {
            dialogWindow.requestFeature(Window.FEATURE_NO_TITLE);
            dialogWindow.setBackgroundDrawableResource(android.R.color.transparent);
            dialogWindow.setFlags(FLAG_FULLSCREEN, FLAG_FULLSCREEN);
        }

        View rootView = inflater.inflate(R.layout.filter_preview, container, false);

        GridView grid_filters = rootView.findViewById(R.id.grid_filters);

        grid_filters.setAdapter(new FilterAdapter(selectedIdx));

        grid_filters.setOnItemClickListener((parent, view, position, id) -> {
            FilterAdapter adapter = (FilterAdapter) parent.getAdapter();
            selectedIdx = position;
            adapter.markSelected(position);
            //GLDrawer2D filter = adapter.getItem(position);
            //onFilterSelected(filter);
        });

        AnimFadeReveal.fadeIn(rootView);

        rootView.setOnClickListener(v -> dismissView());

        return rootView;
    }

//    private void onFilterSelected(GLDrawer2D filter) {
//        if (getTargetFragment() instanceof CameraFragment) {
//            ((CameraFragment) getTargetFragment()).onFilterSelected(filter);
//        }
//    }

    private void dismissView() {
        AnimFadeReveal.fadeOut(getView(), this::dismissDialog);
    }

    protected void dismissDialog() {
        if (isStateSaved()) dismissAllowingStateLoss();
        else dismiss();
    }
}
