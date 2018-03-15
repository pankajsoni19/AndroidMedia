/*
 * Created By Pankaj Soni <pankajsoni@softwarejoint.com>
 * Copyright Wafer Inc. (c) 2017. All rights reserved
 *
 * Last Modified: 13/12/17 10:09 PM By Pankaj Soni <pankajsoni@softwarejoint.com>
 */

package com.softwarejoint.media.permission;

import android.support.annotation.IntDef;

import static com.softwarejoint.media.permission.PermissionRequest.REQUEST_CODE_PHOTO;
import static com.softwarejoint.media.permission.PermissionRequest.REQUEST_CODE_VIDEO;

@IntDef({REQUEST_CODE_PHOTO, REQUEST_CODE_VIDEO})
public @interface PermissionRequest {
    int REQUEST_CODE_PHOTO = 103;
    int REQUEST_CODE_VIDEO = 104;
}
