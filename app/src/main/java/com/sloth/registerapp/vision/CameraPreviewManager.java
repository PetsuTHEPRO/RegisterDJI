package com.sloth.registerapp.vision;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.TextureView;
import android.widget.FrameLayout;
import android.widget.Switch;

import com.sloth.registerapp.R;

import java.io.IOException;

@SuppressWarnings("deprecation")
public class CameraPreviewManager implements TextureView.SurfaceTextureListener {

    private final Context context;
    private final FrameLayout container;
    private final TextureView textureView;
    private Camera camera;
    private boolean isPreviewStarted = false;

    private int currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;

    public CameraPreviewManager(Activity activity) {
        this.context = activity;

        this.container = activity.findViewById(R.id.previewContainer);

        this.textureView = new TextureView(context);
        this.textureView.setSurfaceTextureListener(this);
        container.addView(textureView);


    }

    // Novo método que retorna um frame atual da TextureView
    public Bitmap getCurrentFrame() {
        if (textureView.isAvailable()) {
            return textureView.getBitmap();
        }
        return null;
    }


    private void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);

        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case 0: degrees = 0; break;
            case 1: degrees = 90; break;
            case 2: degrees = 180; break;
            case 3: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensação para câmera frontal (espelhada)
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }

        camera.setDisplayOrientation(result);
    }




    private void restartCamera() {
        stopCamera();
        if (textureView.isAvailable()) {
            startCamera(textureView.getSurfaceTexture());
        } else {
            textureView.setSurfaceTextureListener(this);
        }
    }

    private void startCamera(SurfaceTexture surface) {
        try {
            camera = Camera.open(currentCameraId);
            camera.setPreviewTexture(surface);
            setCameraDisplayOrientation((Activity) context, currentCameraId, camera);
            camera.startPreview();
            isPreviewStarted = true;
        } catch (IOException | RuntimeException e) {
            Log.e("CameraPreview", "Erro ao iniciar camera: " + e.getMessage());
        }
    }

    private void stopCamera() {
        if (camera != null) {
            try {
                camera.stopPreview();
                camera.release();
            } catch (Exception ignored) {}
            camera = null;
            isPreviewStarted = false;
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        startCamera(surface);
    }

    @Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}
    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        stopCamera();
        return true;
    }
    @Override public void onSurfaceTextureUpdated(SurfaceTexture surface) {}

    public TextureView getTextureView() {
        return textureView;
    }
}
