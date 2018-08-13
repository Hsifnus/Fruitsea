package com.melonfishy.fruitsee;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.ImageView;
import android.widget.TextView;

import com.melonfishy.fruitsee.utils.AsyncResponse;
import com.melonfishy.fruitsee.utils.TraceView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CropActivity extends AppCompatActivity implements AsyncResponse {

    private boolean locked, firstPass;
    private TextView modeText;
    private TraceView trace, trace2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);
        locked = false;
        firstPass = true;

        ImageView preview = (ImageView) findViewById(R.id.iv_preview);
        trace = (TraceView) findViewById(R.id.trv_preview);
        trace2 = (TraceView) findViewById(R.id.trv_previewCrop);

        String filename = "";
        if (getIntent() != null && getIntent().hasExtra(CameraActivity.CAMERA_FN_FLAG)) {

            filename = getIntent().getStringExtra(CameraActivity.CAMERA_FN_FLAG);
            File imgFile = new File(filename);
            imgFile.deleteOnExit();

            if (imgFile.exists()) {
                Bitmap capture = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                preview.setImageBitmap(capture);
            }
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int screenHeight = displayMetrics.heightPixels;
            int screenWidth = displayMetrics.widthPixels;

            trace.setDimensions(screenHeight, screenWidth);
            trace2.setDimensions(screenHeight, screenWidth);
            trace.initBitmap();
            trace2.initBitmap();
            trace.changeMode(TraceView.MODE_TRACE);
            trace2.changeMode(TraceView.MODE_NONE);
        }

        FloatingActionButton fabCrop = (FloatingActionButton) findViewById(R.id.fab_crop);
        FloatingActionButton fabTrace = (FloatingActionButton) findViewById(R.id.fab_trace);
        FloatingActionButton fabFill = (FloatingActionButton) findViewById(R.id.fab_fill);
        FloatingActionButton fabConfirm = (FloatingActionButton) findViewById(R.id.fab_confirm);
        FloatingActionButton fabClean = (FloatingActionButton) findViewById(R.id.fab_clean);
        modeText = (TextView) findViewById(R.id.tv_mode);

        fabCrop.setOnClickListener(v -> {
            if (locked) {
                return;
            }
            trace.changeMode(TraceView.MODE_NONE);
            trace2.changeMode(TraceView.MODE_CROP);
            trace2.bringToFront();
            trace2.invalidate();
            trace.invalidate();
            modeText.setText(R.string.mode_crop);
            modeText.setTextColor(ContextCompat.getColor(getApplicationContext(),
                    R.color.colorTraceOrangeOpaque));
        });

        fabTrace.setOnClickListener(v -> {
            if (locked) {
                return;
            }
            trace.changeMode(TraceView.MODE_TRACE);
            trace2.changeMode(TraceView.MODE_NONE);
            trace.bringToFront();
            trace2.invalidate();
            trace.invalidate();
            modeText.setText(R.string.mode_trace);
            modeText.setTextColor(ContextCompat.getColor(getApplicationContext(),
                    R.color.colorTraceBlueOpaque));
        });

        fabFill.setOnClickListener(v -> {
            if (locked) {
                return;
            }
            trace.changeMode(TraceView.MODE_FILL);
            trace2.changeMode(TraceView.MODE_CROP);
            trace.bringToFront();
            trace2.invalidate();
            trace.invalidate();
            modeText.setText(R.string.mode_fill);
            modeText.setTextColor(ContextCompat.getColor(getApplicationContext(),
                    R.color.colorTraceBlueOpaque));
        });

        fabClean.setOnClickListener(v -> {
            if (locked) {
                return;
            }
            trace.clear();
            trace2.clear();
        });

        final String confirmFilename = filename;
        fabConfirm.setOnClickListener(v -> {
            if (locked || confirmFilename.isEmpty()) {
                return;
            }
            confirm();
            Bitmap previewBitmap = ((BitmapDrawable)preview.getDrawable()).getBitmap();
            trace.transformBitmap(previewBitmap, TraceView.MODE_TRACE);
        });
    }

    public void waitForFill() {
        locked = true;
        modeText.setText(R.string.mode_fill_progress);
        modeText.setTextColor(ContextCompat.getColor(getApplicationContext(),
                R.color.colorPrimaryAlt2));
    }

    public void stopWaiting() {
        locked = false;
        modeText.setText(R.string.mode_fill);
        modeText.setTextColor(ContextCompat.getColor(getApplicationContext(),
                R.color.colorTraceBlueOpaque));
    }

    public void confirm() {
        locked = true;
        modeText.setText(R.string.mode_confirm);
        modeText.setTextColor(ContextCompat.getColor(getApplicationContext(),
                R.color.colorPrimaryAlt2));
    }

    @Override
    public void processFinish(Bitmap output) {
        if (firstPass) {
            firstPass = false;
            trace2.transformBitmap(output, TraceView.MODE_CROP);
        } else {
            stopWaiting();
            firstPass = true;

            Intent toClassify = new Intent(this, ClassifyActivity.class);

            File storageDirectory = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File galleryFolder = new File(storageDirectory, getResources()
                    .getString(R.string.app_name));

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                    Locale.getDefault()).format(new Date());
            String imageFileName = "image_" + timeStamp + "_";
            File result;

            try {
                result = File.createTempFile(imageFileName, ".jpg", galleryFolder);
                FileOutputStream out = new FileOutputStream(result.getAbsolutePath());
                output.compress(Bitmap.CompressFormat.PNG, 100, out);
                toClassify.putExtra(CameraActivity.CAMERA_FN_FLAG, result.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }

            startActivity(toClassify);
        }
    }

    @Override
    public void processFinish(Integer output) {
        throw new UnsupportedOperationException("CropActivity does not support processFinish()"
        + " for Integer arguments.");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
