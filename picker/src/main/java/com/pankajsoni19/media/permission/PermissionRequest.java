/*
 * Created By Pankaj Soni <pankajsoni19@live.com>
 * Copyright Wafer Inc. (c) 2017. All rights reserved
 *
 * Last Modified: 13/12/17 10:09 PM By Pankaj Soni <pankajsoni19@live.com>
 */

package com.pankajsoni19.media.permission;

import androidx.annotation.IntDef;

import static com.pankajsoni19.media.permission.PermissionRequest.REQUEST_CODE_PHOTO;
import static com.pankajsoni19.media.permission.PermissionRequest.REQUEST_CODE_VIDEO;

@IntDef({REQUEST_CODE_PHOTO, REQUEST_CODE_VIDEO})
public @interface PermissionRequest {
    int REQUEST_CODE_PHOTO = 103;
    int REQUEST_CODE_VIDEO = 104;
}
