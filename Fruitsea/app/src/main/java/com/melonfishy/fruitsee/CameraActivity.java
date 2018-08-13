package com.melonfishy.fruitsee;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CameraActivity extends AppCompatActivity {

    private final int MY_PERMISSIONS_REQUEST_CAMERA = 100;
    private CameraManager cameraManager;
    private final int cameraFacing = CameraMetadata.LENS_FACING_BACK;
    private String cameraId;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private CameraDevice.StateCallback stateCallback;
    private CameraDevice cameraDevice;
    private TextureView textureView;
    private TextureView.SurfaceTextureListener surfaceTextureListener;
    private Size previewSize;
    private CaptureRequest.Builder captureRequestBuilder;
    private CaptureRequest captureRequest;
    private CameraCaptureSession cameraCaptureSession;
    private File galleryFolder;

    public static final String CAMERA_FN_FLAG = "CAMERA_FN_FLAG";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        createImageGallery();
        textureView = (TextureView) findViewById(R.id.texture_view);
        cameraId = "";
        cameraManager =
                (CameraManager) getApplicationContext().getSystemService(Context.CAMERA_SERVICE);

        surfaceTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                if (checkCameraHardware(getApplicationContext())) {
                    if (ContextCompat.checkSelfPermission(getApplicationContext(),
                            "android.permission.CAMERA") == PackageManager.PERMISSION_DENIED) {
                        ActivityCompat.requestPermissions(CameraActivity.this,
                                new String[]{"android.permission.CAMERA"},
                                MY_PERMISSIONS_REQUEST_CAMERA);
                    }
                    if (ContextCompat.checkSelfPermission(getApplicationContext(),
                            "android.permission.WRITE_EXTERNAL_STORAGE") ==
                            PackageManager.PERMISSION_DENIED) {
                        ActivityCompat.requestPermissions(CameraActivity.this,
                                new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"},
                                MY_PERMISSIONS_REQUEST_CAMERA);
                    }
                    if (setUpCamera()) {
                        openCamera();
                    }
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

            }
        };

        stateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice cameraDevice) {
                CameraActivity.this.cameraDevice = cameraDevice;
                createPreviewSession();
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                cameraDevice.close();
                CameraActivity.this.cameraDevice = null;
            }

            @Override
            public void onError(@NonNull CameraDevice cameraDevice, int error) {
                cameraDevice.close();
                CameraActivity.this.cameraDevice = null;
            }
        };

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_take_photo);
        fab.setOnClickListener(v -> {
            lock();
            FileOutputStream outputPhoto = null;
            String cameraFileName = "";
            try {
                File newFile = createImageFile(galleryFolder);
                outputPhoto = new FileOutputStream(newFile);
                textureView.getBitmap()
                        .compress(Bitmap.CompressFormat.PNG, 100, outputPhoto);
                cameraFileName = newFile.getAbsolutePath();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                unlock();
                try {
                    if (outputPhoto != null && !cameraFileName.isEmpty()) {
                        outputPhoto.close();
                        Intent toCrop = new Intent(CameraActivity.this, CropActivity.class);
                        toCrop.putExtra(CAMERA_FN_FLAG, cameraFileName);
                        startActivity(toCrop);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio=(double)h / w;

        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Size size : sizes) {
            double ratio = (double) size.getWidth() / size.getHeight();
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.getHeight() - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.getHeight() - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.getHeight() - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.getHeight() - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    private boolean setUpCamera() {
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics =
                        cameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                        cameraFacing) {
                    StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(
                            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    ArrayList<Size> previewSizes = new ArrayList<>();
                    for (Size s : streamConfigurationMap.getOutputSizes(SurfaceTexture.class)) {
                        previewSizes.add(s);
                    }

                    DisplayMetrics displayMetrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                    int screenHeight = displayMetrics.heightPixels - getStatusBarHeight();
                    int screenWidth = displayMetrics.widthPixels;

                    previewSize = getOptimalPreviewSize(previewSizes, screenWidth, screenHeight);
                    this.cameraId = cameraId;
                }
            }
        } catch (CameraAccessException | NullPointerException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void openCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(cameraId, stateCallback, backgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openBackgroundThread() {
        backgroundThread = new HandlerThread("camera_background_thread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        openBackgroundThread();
        if (textureView.isAvailable()) {
            setUpCamera();
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        closeCamera();
        closeBackgroundThread();
    }

    private void closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    private void closeBackgroundThread() {
        if (backgroundHandler != null) {
            backgroundThread.quitSafely();
            backgroundThread = null;
            backgroundHandler = null;
        }
    }

    private void createPreviewSession() {
        try {
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(previewSurface);

            cameraDevice.createCaptureSession(Collections.singletonList(previewSurface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            if (cameraDevice == null) {
                                return;
                            }

                            try {
                                captureRequest = captureRequestBuilder.build();
                                CameraActivity.this.cameraCaptureSession = cameraCaptureSession;
                                CameraActivity.this.cameraCaptureSession.setRepeatingRequest(captureRequest,
                                        null, backgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {

                        }
                    }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void createImageGallery() {
        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        galleryFolder = new File(storageDirectory, getResources().getString(R.string.app_name));
        if (!galleryFolder.exists()) {
            boolean wasCreated = galleryFolder.mkdir();
            if (!wasCreated) {
                Log.e("CapturedImages", "Failed to create directory");
            }
        }
    }

    private File createImageFile(File galleryFolder) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "image_" + timeStamp + "_";
        return File.createTempFile(imageFileName, ".jpg", galleryFolder);
    }


    private void lock() {
        try {
            cameraCaptureSession.capture(captureRequestBuilder.build(),
                    null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void unlock() {
        try {
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(),
                    null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {}

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}
