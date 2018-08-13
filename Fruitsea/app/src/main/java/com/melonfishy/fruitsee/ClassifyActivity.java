package com.melonfishy.fruitsee;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.melonfishy.fruitsee.utils.AsyncResponse;
import com.melonfishy.fruitsee.utils.ClassifyUtils;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.File;
import java.util.ArrayList;

public class ClassifyActivity extends AppCompatActivity implements AsyncResponse {

    private TensorFlowInferenceInterface tensorflow;

    private class ClassifyTask extends AsyncTask<Bitmap, Void, Integer> {
        AsyncResponse delegate = ClassifyActivity.this;

        @Override
        protected Integer doInBackground(Bitmap... params) {

            ArrayList<Bitmap> testBitmaps = new ArrayList<>();

            Bitmap rawBitmap = params[0];
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
            float[] maxWeights = new float[30];
            for (int h = 0; h < 30; h++) {
                maxWeights[h] = -1;
                maxIndices[h] = -1;
                for (int i = 0; i < 75; i++) {
                    if (results[75 * h + i] > maxWeights[h]) {
                        maxWeights[h] = results[75 * h + i];
                        maxIndices[h] = i;
                    }
                }
                Log.d("ClassifyActivity", String.format("Subguess %d: %d with weight %f", h,
                        maxIndices[h], maxWeights[h]));
            }

            ArrayList<Integer> modes = ClassifyUtils.findMode(maxIndices, maxWeights, 5);
            ArrayList<Float> frequencies = ClassifyUtils.getHistogram(maxIndices, maxWeights);
            for (int i = 0; i < modes.size(); i++) {
                Log.d("ClassifyActivity", String.format("Guess %d: %s with weight %f", i,
                        ClassifyUtils.typeToString(modes.get(i)),
                        frequencies.get(modes.get(i))));
            }
            return modes.get(0);
        }

        @Override
        protected void onPostExecute(Integer integer) {
            delegate.processFinish(integer);
            super.onPostExecute(integer);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_classify);

        ImageView test = (ImageView) findViewById(R.id.iv_test);
        if (getIntent() != null && getIntent().hasExtra(CameraActivity.CAMERA_FN_FLAG)) {
            String filename = getIntent().getStringExtra(CameraActivity.CAMERA_FN_FLAG);
            File imgFile = new File(filename);
            imgFile.deleteOnExit();
            if (imgFile.exists()) {
                Bitmap capture = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                test.setImageBitmap(capture);
                tensorflow = new TensorFlowInferenceInterface(getAssets(),
                        "frozen_graph.pb");
                new ClassifyTask().execute(capture);
            }
        }
    }

    @Override
    public void onBackPressed() {
        Intent toCamera = new Intent(this, CameraActivity.class);
        startActivity(toCamera);
    }

    @Override
    public void processFinish(Bitmap output) {
        throw new UnsupportedOperationException("ClassifyActivity does not support processFinish()"
                + " for Bitmap arguments.");
    }

    @Override
    public void processFinish(Integer output) {

    }
}
