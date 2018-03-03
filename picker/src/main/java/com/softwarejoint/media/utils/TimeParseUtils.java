package com.softwarejoint.media.utils;

import java.util.Locale;

/**
 * Created by Pankaj Soni <pankajsoni@softwarejoint.com> on 03/03/18.
 * Copyright (c) 2018 Software Joint. All rights reserved.
 */
public class TimeParseUtils {

    public static String getFormattedTimeHHMMSS(long milliseconds) {
        int hours = (int) (milliseconds / (1000 * 60 * 60) % 60);
        int minutes = (int) ((milliseconds / (1000 * 60)) % 60);
        int seconds = (int) (milliseconds / 1000) % 60;
        return String.format(Locale.ENGLISH, "%02d:%02d:%02d", hours, minutes, seconds);
    }

}
