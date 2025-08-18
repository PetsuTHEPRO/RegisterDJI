package com.sloth.registerapp.vision;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Switch;
import android.widget.FrameLayout;

import com.sloth.registerapp.R;

import java.io.IOException;
import android.os.Handler;
import java.util.logging.LogRecord;

@SuppressWarnings("deprecation")
public class CameraPreviewManager implements TextureView.SurfaceTextureListener {

    // Adicione uma TAG para filtrar os logs consistentemente
    private static final String TAG = "CameraDebug";

    private final Context context;
    private final FrameLayout container;
    private final TextureView textureView;
    private Camera camera;
    private boolean isPreviewStarted = false;
    private int currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public CameraPreviewManager(Activity activity) {
        this.context = activity;

        this.container = activity.findViewById(R.id.previewContainer);

        this.textureView = new TextureView(context);
        this.textureView.setSurfaceTextureListener(this);
        container.addView(textureView);
        // Log para saber quando o manager é criado
        Log.d(TAG, "CameraPreviewManager: Construtor chamado e TextureView adicionada.");
    }

    // O resto da sua classe continua aqui
    public Bitmap getCurrentFrame() {
        if (textureView.isAvailable()) { return textureView.getBitmap(); }
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


    public void switchCamera() {
        Log.i(TAG, "switchCamera: Requisição para trocar de câmera recebida.");

        if (Camera.getNumberOfCameras() < 2) {
            Log.w(TAG, "switchCamera: Dispositivo não possui mais de uma câmera.");
            return;
        }

        // 1. Para a câmera atual.
        stopCamera();

        // 2. Inverte o ID da câmera.
        currentCameraId = (currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK)
                ? Camera.CameraInfo.CAMERA_FACING_FRONT
                : Camera.CameraInfo.CAMERA_FACING_BACK;

        Log.d(TAG, "switchCamera: Câmera parada. Agendando reinício para a nova câmera ID: " + currentCameraId);

        // 3. AGENDA o reinício da câmera para daqui a um instante.
        mainHandler.postDelayed(() -> {
            Log.d(TAG, "Handler: Executando o reinício da câmera agendado.");
            restartCamera();
        }, 150);
    }

    public void restartCamera() {
        Log.d(TAG, "restartCamera: Reiniciando a câmera.");
        stopCamera(); // A parada será logada dentro do método stopCamera

        if (textureView.isAvailable()) {
            Log.d(TAG, "restartCamera: TextureView está disponível. Iniciando a câmera imediatamente.");
            startCamera(textureView.getSurfaceTexture());
        } else {
            // Este cenário pode acontecer se a troca for muito rápida.
            Log.w(TAG, "restartCamera: TextureView NÃO está disponível. Aguardando onSurfaceTextureAvailable.");
            textureView.setSurfaceTextureListener(this);
        }
    }

    public void startCamera(SurfaceTexture surface) {
        Log.d(TAG, "startCamera: Tentando iniciar a câmera com ID: " + currentCameraId);
        if (camera != null) {
            Log.w(TAG, "startCamera: Câmera já estava iniciada. Parando antes de continuar.");
            stopCamera();
        }
        try {
            camera = Camera.open(currentCameraId);
            camera.setPreviewTexture(surface);
            setCameraDisplayOrientation((Activity) context, currentCameraId, camera);
            camera.startPreview();
            isPreviewStarted = true;
            Log.i(TAG, "startCamera: Câmera iniciada com SUCESSO.");
        } catch (IOException | RuntimeException e) {
            // Log de ERRO crucial para diagnosticar falhas
            Log.e(TAG, "startCamera: ERRO CRÍTICO ao iniciar câmera. Causa: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stopCamera() {
        if (camera != null) {
            Log.d(TAG, "stopCamera: Parando e liberando a câmera.");
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
        Log.d(TAG, "onSurfaceTextureAvailable: Superfície pronta. Iniciando a câmera.");
        startCamera(surface);
    }

    @Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}
    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d(TAG, "onSurfaceTextureDestroyed: Superfície destruída. Parando a câmera.");
        stopCamera();
        return true;
    }
    @Override public void onSurfaceTextureUpdated(SurfaceTexture surface) {}

    public TextureView getTextureView() {
        return textureView;
    }
}