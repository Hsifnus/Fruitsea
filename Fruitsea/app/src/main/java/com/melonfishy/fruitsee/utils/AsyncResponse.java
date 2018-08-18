package com.melonfishy.fruitsee.utils;

import android.graphics.Bitmap;

import java.util.ArrayList;

/**
 * Created by bakafish on 8/8/18.
 */

public interface AsyncResponse {
    void processFinish(Bitmap output);
    void processFinish(ArrayList<Integer> output);
}
