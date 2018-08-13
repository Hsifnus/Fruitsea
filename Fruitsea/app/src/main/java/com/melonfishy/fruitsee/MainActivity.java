package com.melonfishy.fruitsee;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseIntArray;
import android.widget.TextView;

import org.tensorflow.contrib.android.*;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ArrayList<Integer> findMode(int[] arr) {
        SparseIntArray histogram = new SparseIntArray();
        for (int i : arr) {
            if (histogram.indexOfKey(i) == -1) {
                histogram.put(i, 1);
            } else {
                histogram.put(i, histogram.get(i) + 1);
            }
        }
        ArrayList<Integer> candidates = new ArrayList<>();
        int maxCount = -1;
        for (int i = 0; i < histogram.size(); i++) {
            if (histogram.valueAt(i) > maxCount) {
                maxCount = histogram.valueAt(i);
                candidates = new ArrayList<>();
                candidates.add(histogram.keyAt(i));
            } else if (histogram.valueAt(i) == maxCount) {
                candidates.add(histogram.keyAt(i));
            }
        }
        return candidates;
    }

    private boolean contains(ArrayList<Integer> ints, int item) {
        for (int i = 0; i < ints.size(); i++) {
            if (ints.get(i) == item) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String[] assets = {"Got nothing."};
        try {
            assets = getAssets().list("");
        } catch (IOException e) {
            Log.d("mainActivity", "Failed to get assets list.");
        }
        Log.d("mainActivity", assets[0]);
        TensorFlowInferenceInterface tensorflow =
                new TensorFlowInferenceInterface(getAssets(),
                        "frozen_graph.pb");

        ArrayList<Bitmap> testBitmaps = new ArrayList<>();

        Bitmap rawBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.starfruit_alt_2);
        testBitmaps.add(Bitmap.createScaledBitmap(rawBitmap, 100, 100, true));
        for (int i = 1; i < 30; i++) {
            Matrix rotated = new Matrix();
            rotated.postRotate(12*i);
            testBitmaps.add(Bitmap.createBitmap(testBitmaps.get(0), 0, 0, 100, 100, rotated, true));
        }

        long[] testInput = new long[900000];
        for (int h = 0; h < 30; h++) {
            for (int i = 0; i < 10000; i++) {
                int pixel = testBitmaps.get(h).getPixel(i / 100, i % 100);
                testInput[30000 * h + 3 * i] = (pixel & 0x00ff0000) >> 16;
                testInput[30000 * h + 3 * i + 1] = (pixel & 0x0000ff00) >> 8;
                testInput[30000 * h + 3 * i + 2] = pixel & 0x000000ff;
                // Log.d("mainActivity", "(" + testInput[3 * i] + ", " +
                        // testInput[3 * i + 1] + ", " + testInput[3 * i + 2] + ")");
            }
        }

        tensorflow.feed("Const", testInput, 30, 100, 100, 3);
        String[] outputNames = {"softmax_linear/softmax_linear"};
        tensorflow.run(outputNames);

        float[] results = new float[30 * 75];
        tensorflow.fetch("softmax_linear/softmax_linear", results);
        int[] maxIndices = new int[30];
        for (int h = 0; h < 30; h++) {
            float max = -1;
            maxIndices[h] = -1;
            for (int i = 0; i < 75; i++) {
                if (results[75 * h + i] > max) {
                    max = results[75 * h + i];
                    maxIndices[h] = i;
                }
            }
            Log.d("mainActivity", String.format("Subguess %d: %d", h, maxIndices[h]));
        }

        TextView guess = (TextView) findViewById(R.id.guess);
        ArrayList<Integer> modes = findMode(maxIndices);
        for (int i = 0; i < modes.size(); i++) {
            Log.d("mainActivity", String.format("Guess: %d", modes.get(i)));
        }

        if (contains(modes, 13)) {
            guess.setText(getString(R.string.guess_banana));
        } else if (contains(modes, 18)) {
            guess.setText(getString(R.string.guess_carambola));
        } else if (contains(modes, 55)) {
            guess.setText(getString(R.string.guess_pitahaya));
        } else {
            guess.setText(getString(R.string.guess_unknown));
        }

    }
}
