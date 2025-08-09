package com.sloth.registerapp.vision;

import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;
import android.view.TextureView;

import androidx.annotation.NonNull;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.BiConsumer;

public class FaceAnalyzer {

    private static final String TAG = "FaceAnalyzer";
    private final FaceDetector detector;
    private final BiConsumer<List<Face>, Bitmap> callback;

    // Buffers pré-alocados para otimização de memória
    private byte[] nv21Buffer = null;
    private int[] argbBuffer = null;

    // Tamanho esperado do frame
    private int expectedWidth = 0;
    private int expectedHeight = 0;

    public FaceAnalyzer(BiConsumer<List<Face>, Bitmap> callback) {
        this.callback = callback;

        // 🔹 Criando o detector diretamente aqui
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .enableTracking()
                        .build();

        this.detector = FaceDetection.getClient(options);
    }

    public Bitmap otimizeBitmap(Bitmap bitmap) {
        return Bitmap.createScaledBitmap(
                bitmap,
                bitmap.getWidth() / 6,
                bitmap.getHeight() / 6,
                true
        );
    }

    public void analyzeOld(@NonNull Bitmap bitmap) {
        try {
            InputImage image = InputImage.fromBitmap(bitmap, 0);

            detector.process(image)
                    .addOnSuccessListener(faces -> {
                        Log.d(TAG, "Rostos detectados: " + faces.size());
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            callback.accept(faces, bitmap);
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Erro na detecção de rosto: " + e.getMessage()));
        } catch (Exception e) {
            Log.e(TAG, "Erro ao preparar imagem: " + e.getMessage());
        }
    }

    public void analyzeFast(TextureView viewer) {
        try {
            Bitmap bitmap = viewer.getBitmap();
            if (bitmap == null) {
                Log.w(TAG, "Bitmap do TextureView está nulo.");
                return;
            }

            int currentWidth = bitmap.getWidth();
            int currentHeight = bitmap.getHeight();

            if (nv21Buffer == null || currentWidth != expectedWidth || currentHeight != expectedHeight) {
                expectedWidth = currentWidth;
                expectedHeight = currentHeight;
                nv21Buffer = new byte[currentWidth * currentHeight * 3 / 2];
                argbBuffer = new int[currentWidth * currentHeight];
                Log.d(TAG, "Buffers NV21 e ARGB inicializados/redimensionados para " + currentWidth + "x" + currentHeight);
            }

            getNV21FromBitmap(bitmap, nv21Buffer, argbBuffer);

            ByteBuffer nv21ByteBuffer = ByteBuffer.wrap(nv21Buffer);

            InputImage image = InputImage.fromByteBuffer(
                    nv21ByteBuffer,
                    currentWidth,
                    currentHeight,
                    0, // ajuste se necessário
                    InputImage.IMAGE_FORMAT_NV21
            );

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                detector.process(image)
                        .addOnSuccessListener(faces -> callback.accept(faces, bitmap))
                        .addOnFailureListener(e -> Log.e(TAG, "Erro na detecção de rosto: " + e.getMessage(), e));
            }

        } catch (Exception e) {
            Log.e(TAG, "Erro ao analisar frame da câmera: " + e.getMessage(), e);
        }
    }

    private void getNV21FromBitmap(Bitmap bitmap, byte[] nv21Buffer, int[] argbBuffer) {
        int inputWidth = bitmap.getWidth();
        int inputHeight = bitmap.getHeight();

        bitmap.getPixels(argbBuffer, 0, inputWidth, 0, 0, inputWidth, inputHeight);
        encodeYUV420SP(nv21Buffer, argbBuffer, inputWidth, inputHeight);
    }

    private void encodeYUV420SP(byte[] yuv420sp, int[] argb, int width, int height) {
        final int frameSize = width * height;

        int yIndex = 0;
        int uvIndex = frameSize;

        int R, G, B, Y, U, V;

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                int argbPixel = argb[j * width + i];

                R = (argbPixel >> 16) & 0xff;
                G = (argbPixel >> 8) & 0xff;
                B = argbPixel & 0xff;

                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
                V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;

                yuv420sp[yIndex++] = (byte) (Y < 0 ? 0 : (Math.min(Y, 255)));

                if (j % 2 == 0 && i % 2 == 0) {
                    yuv420sp[uvIndex++] = (byte) (Math.min(Math.max(V, 0), 255));
                    yuv420sp[uvIndex++] = (byte) (Math.min(Math.max(U, 0), 255));
                }
            }
        }
    }
}
