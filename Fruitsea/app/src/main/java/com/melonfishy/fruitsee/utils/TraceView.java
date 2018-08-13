package com.melonfishy.fruitsee.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.melonfishy.fruitsee.CropActivity;
import com.melonfishy.fruitsee.R;

import java.lang.Math;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by bakafish on 8/6/18. >v<
 */

public class TraceView extends AppCompatImageView {

    private int height, width, strokeSize, lastX, lastY, cropX1, cropX2, cropY1, cropY2, corner, sb;
    private Bitmap image, output;
    private BitmapDrawable bitmapDrawable;
    private boolean inflated, locked;
    private enum Mode {CROP, TRACE, FILL, NONE};
    private Mode currentMode;
    private static final String DEBUG_TAG = "TraceView";
    private int traceBlue, traceOrange;
    private boolean transformMode;
    private CropActivity parent;

    public static final int MODE_CROP = 0;
    public static final int MODE_TRACE = 1;
    public static final int MODE_FILL = 2;
    public static final int MODE_NONE = 3;

    private class FloodFillTask extends AsyncTask<Integer, Void, Void> {
        @Override
        protected void onPreExecute() {
            parent.waitForFill();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Integer... params) {
            int x = params[0];
            int y = params[1];
            fill(x, y);
            return null;
        }

        private long loc(int x, int y) {
            return x << 16 | y;
        }

        private void fill(int x, int y) {

            image.setPixel(x, y, traceBlue);
            ArrayDeque<ArrayList<Integer>> buffer = new ArrayDeque<>();
            ArrayList<Integer> first = new ArrayList<>();
            HashSet<Long> queued = new HashSet<>();
            first.add(x);
            first.add(y);
            queued.add(loc(x, y));
            buffer.push(first);

            while(!buffer.isEmpty()) {

                ArrayList<Integer> target = buffer.pop();
                int tx = target.get(0);
                int ty = target.get(1);
                image.setPixel(tx, ty, traceBlue);
                queued.remove(loc(x, y));
                ArrayList<Integer> neighbor;

                if (ty > 0 && image.getPixel(tx, ty - 1) == 0 && !queued.contains(loc(tx, ty - 1)))
                {
                    neighbor = new ArrayList<>();
                    neighbor.add(tx);
                    neighbor.add(ty - 1);
                    buffer.push(neighbor);
                    queued.add(loc(tx, ty - 1));
                }

                if (ty < height - 1 && image.getPixel(tx, ty + 1) == 0
                        && !queued.contains(loc(tx, ty + 1))) {
                    neighbor = new ArrayList<>();
                    neighbor.add(tx);
                    neighbor.add(ty + 1);
                    buffer.push(neighbor);
                    queued.add(loc(tx, ty + 1));
                }

                if (tx > 0 && image.getPixel(tx - 1, ty) == 0 && !queued.contains(loc(tx - 1, ty)))
                {
                    neighbor = new ArrayList<>();
                    neighbor.add(tx - 1);
                    neighbor.add(ty);
                    buffer.push(neighbor);
                    queued.add(loc(tx - 1, ty));
                }

                if (tx < width - 1 && image.getPixel(tx + 1, ty) == 0
                        && !queued.contains(loc(tx + 1, ty))) {
                    neighbor = new ArrayList<>();
                    neighbor.add(tx + 1);
                    neighbor.add(ty);
                    buffer.push(neighbor);
                    queued.add(loc(tx + 1, ty));
                }

            }

        }

        @Override
        protected void onPostExecute(Void aVoid) {
            locked = false;
            parent.stopWaiting();
            super.onPostExecute(aVoid);
        }
    }

    private class TransformTask extends AsyncTask<Bitmap, Void, Bitmap> {
        private AsyncResponse delegate = parent;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Bitmap doInBackground(Bitmap... params) {
            Bitmap input = params[0];
            input = input.copy(Bitmap.Config.ARGB_8888, true);
            if (transformMode) {
                boolean lessThanX = cropX1 <= cropX2;
                boolean lessThanY = cropY1 <= cropY2;
                int x1 = lessThanX ? cropX1 : cropX2;
                int x2 = lessThanX ? cropX2 : cropX1;
                x1 = Math.max(0, x1);
                x2 = Math.min(input.getWidth(), x2);
                int y1 = lessThanY ? cropY1 : cropY2;
                int y2 = lessThanY ? cropY2 : cropY1;
                Log.d("traceView", x1 + ", " + y1 + " : " + x2 + ", " + y2 + " : "
                        + input.getWidth() + ", " + input.getHeight());
                int ceil = Math.max(sb, y1);
                return Bitmap.createBitmap(input, x1, ceil - sb, x2 - x1, y2 - ceil);
            } else {
                for (int i = 0; i < input.getWidth(); i++) {
                    for (int j = 0; j < input.getHeight(); j++) {
                        if (image.getPixel(i, j + sb / 2) == 0) {
                            input.setPixel(i, j, 0xFFFFFF);
                        }
                    }
                }
                return input;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            delegate.processFinish(bitmap);
            super.onPostExecute(bitmap);
        }
    }

    public TraceView(Context context) {
        super(context);
        height = width = 0;
        lastX = lastY = -1;
        cropX1 = cropX2 = cropY1 = cropY2 = -1;
        strokeSize = 10;
        corner = 0;
        currentMode = Mode.TRACE;
        inflated = locked = false;
        traceBlue = ContextCompat.getColor(getContext(), R.color.colorTraceBlue);
        traceOrange = ContextCompat.getColor(getContext(), R.color.colorTraceOrange);
        parent = (CropActivity) getContext();
    }

    public TraceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        height = width = 0;
        lastX = lastY = -1;
        cropX1 = cropX2 = cropY1 = cropY2 = -1;
        strokeSize = 10;
        corner = 0;
        currentMode = Mode.CROP;
        inflated = locked = false;
        traceBlue = ContextCompat.getColor(getContext(), R.color.colorTraceBlue);
        traceOrange = ContextCompat.getColor(getContext(), R.color.colorTraceOrange);
        parent = (CropActivity) getContext();
    }

    public void setDimensions(int h, int w) {
        height = h;
        width = w;
    }

    public void initBitmap() {
        if (!inflated && (height & width) != 0) {
            inflated = true;
            image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            image.eraseColor(0);
        }
        cropX1 = cropY1 = 0;
        cropX2 = width;
        cropY2 = height;
    }

    public void clearBitmap() {
        if (inflated) {
            image.eraseColor(0);
        }
    }

    private void resetParams() {
        lastX = lastY = -1;
        corner = 0;
    }

    public void clear() {
        clearBitmap();
        lastX = lastY = -1;
        cropX1 = cropY1 = 0;
        cropX2 = width;
        cropY2 = height;
        corner = 0;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int action = MotionEventCompat.getActionMasked(event);
        int x = Math.round(event.getX());
        int y = Math.round(event.getY());

        if (!inflated) {
            return true;
        }
        switch(action) {
            case (MotionEvent.ACTION_DOWN) :
                // Log.d(DEBUG_TAG,"Action was DOWN");
                draw(x, y, true);
                bitmapDrawable = new BitmapDrawable(getContext().getResources(), image);
                return true;
            case (MotionEvent.ACTION_MOVE) :
                // Log.d(DEBUG_TAG,"Action was MOVE");
                draw(x, y, false);
                bitmapDrawable = new BitmapDrawable(getContext().getResources(), image);
                return true;
            case (MotionEvent.ACTION_UP) :
                // Log.d(DEBUG_TAG,"Action was UP");
                // reset last location data
                resetParams();
                return true;
            case (MotionEvent.ACTION_CANCEL) :
                // Log.d(DEBUG_TAG,"Action was CANCEL");
                // reset last location data
                resetParams();
                return true;
            case (MotionEvent.ACTION_OUTSIDE) :
                // Log.d(DEBUG_TAG,"Movement occurred outside bounds " +
                //         "of current screen element");
                resetParams();
                return true;
            default:
                resetParams();
                return super.onTouchEvent(event);
        }
    }

    private void draw(int x, int y, boolean isDown) {
        switch(currentMode) {
            case CROP:
                setCropping(x, y, isDown);
                break;
            case TRACE:
                drawLine(x, y);
                break;
            case FILL:
                if (image.getPixel(x, y) == 0) {
                    floodFill(x, y);
                }
                break;
        }
    }

    private void drawLine(int x, int y) {

        if (!locked) {

            Canvas canvas = new Canvas(image);
            if (lastX > -1 && lastY > -1) {
                Paint paint = new Paint();
                paint.setColor(traceBlue);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(strokeSize);
                canvas.drawLine(lastX, lastY, x, y, paint);
            }
            // update previous location information
            lastX = x;
            lastY = y;
        }
    }

    private void floodFill(int x, int y) {

        if (!locked) {
            locked = true;
            new FloodFillTask().execute(x, y);
        }

    }

    private void setCropping(int x, int y, boolean actionIsDown) {

        if (locked || cropX1 == -1 || cropX2 == -1 || cropY1 == -1 || cropY2 == -1) {
            return;
        }

        if (actionIsDown) {
            double dist1 = Math.pow(x - cropX1, 2) + Math.pow(y - cropY1, 2);
            double dist2 = Math.pow(x - cropX2, 2) + Math.pow(y - cropY2, 2);
            corner = dist1 <= dist2 ? 1 : 2;
        }

        if (corner == 1) {
            cropX1 = x;
            cropY1 = y;
        } else if (corner == 2) {
            cropX2 = x;
            cropY2 = y;
        }

        clearBitmap();
        Canvas canvas = new Canvas(image);
        Paint paint = new Paint();

        paint.setColor(traceOrange);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeSize);

        canvas.drawLine(cropX1, cropY1, cropX2, cropY1, paint);
        canvas.drawLine(cropX2, cropY1, cropX2, cropY2, paint);
        canvas.drawLine(cropX2, cropY2, cropX1, cropY2, paint);
        canvas.drawLine(cropX1, cropY2, cropX1, cropY1, paint);

        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawCircle(cropX1, cropY1, 30, paint);
        canvas.drawCircle(cropX2, cropY2, 30, paint);


    }

    @Override
    protected void onDraw(Canvas canvas) {
        setBackground(bitmapDrawable);
        super.onDraw(canvas);
        invalidate();
    }

    public void changeMode(int code) {
        switch (code) {
            case MODE_CROP:
                currentMode = Mode.CROP;
                break;
            case MODE_TRACE:
                currentMode = Mode.TRACE;
                break;
            case MODE_FILL:
                currentMode = Mode.FILL;
                break;
            case MODE_NONE:
                currentMode = Mode.NONE;
                break;
        }
    }

    public void transformBitmap(Bitmap input, int code) {
        sb = getStatusBarHeight();
        transformMode = code == MODE_CROP;
        new TransformTask().execute(input);
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}
