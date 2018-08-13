package com.melonfishy.fruitsee.utils;

import android.graphics.Bitmap;

/**
 * Created by bakafish on 8/8/18.
 */

public interface AsyncResponse {
    void processFinish(Bitmap output);
    void processFinish(Integer output);
}
