package com.softwarejoint.media.multitouch;

import android.annotation.SuppressLint;
import android.graphics.PointF;

/**
 * Copied from https://github.com/thuytrinh/android-collage-views
 */
@SuppressLint("ParcelCreator")
class Vector2D extends PointF {

    Vector2D() {
        super();
    }

    static float getAngle(Vector2D vector1, Vector2D vector2) {
        vector1.normalize();
        vector2.normalize();
        double degrees = (180.0 / Math.PI) * (Math.atan2(vector2.y, vector2.x) - Math.atan2(vector1.y, vector1.x));
        return (float) degrees;
    }

    private void normalize() {
        float length = (float) Math.sqrt(x * x + y * y);
        x /= length;
        y /= length;
    }
}