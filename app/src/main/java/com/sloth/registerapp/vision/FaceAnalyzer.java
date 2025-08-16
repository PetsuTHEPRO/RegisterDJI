package com.sloth.registerapp.vision;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;
import android.view.TextureView;

import androidx.annotation.NonNull;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetector;

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

    // Tamanho esperado do frame (largura e altura)
    private int expectedWidth = 0;
    private int expectedHeight = 0;

    public FaceAnalyzer(BiConsumer<List<Face>, Bitmap> callback) {
        this.callback = callback;
        this.detector = FaceDetectorProvider.getFaceDetector();
    }

    public Bitmap otimizeBitmap(Bitmap bitmap)
    {
        Bitmap smallBitmap = Bitmap.createScaledBitmap(
                bitmap,
                bitmap.getWidth() / 6,  // metade da largura
                bitmap.getHeight() / 6, // metade da altura
                true // filtro bilinear para melhor qualidade
        );

        return smallBitmap;
    }

    public void analyzeOld(@NonNull Bitmap bitmap) {



        try {
            InputImage image = InputImage.fromBitmap(bitmap, 0);

            detector.process(image)
                    .addOnSuccessListener(faces -> {
                        //Bitmap bitmap2 = otimizeBitmap(bitmap);

                        Log.d(TAG, "Rostos detectados: " + faces.size());
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            callback.accept(faces, bitmap); // envia ambos: rostos + bitmap
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

            // Inicializa ou redimensiona os buffers se o tamanho do frame mudar
            if (nv21Buffer == null || currentWidth != expectedWidth || currentHeight != expectedHeight) {
                expectedWidth = currentWidth;
                expectedHeight = currentHeight;
                nv21Buffer = new byte[currentWidth * currentHeight * 3 / 2];
                argbBuffer = new int[currentWidth * currentHeight];
                Log.d(TAG, "Buffers NV21 e ARGB inicializados/redimensionados para " + currentWidth + "x" + currentHeight);
            }

            // Converta bitmap para NV21 byte[] usando buffers pré-alocados
            getNV21FromBitmap(bitmap, nv21Buffer, argbBuffer);

            // ML Kit agora pode aceitar ByteBuffer diretamente para melhor desempenho
            // do que converter byte[] para ByteBuffer internamente.
            // Wrap o array de bytes existente para evitar cópia.
            ByteBuffer nv21ByteBuffer = ByteBuffer.wrap(nv21Buffer);

            InputImage image = InputImage.fromByteBuffer(
                    nv21ByteBuffer,
                    currentWidth,
                    currentHeight,
                    0, // rotationDegrees (ajuste conforme necessidade, 0 é o padrão para TextureView)
                    InputImage.IMAGE_FORMAT_NV21
            );

            detector.process(image)
                    .addOnSuccessListener(faces -> {
                        // Log.d(TAG, "Rostos detectados: " + faces.size()); // Descomente para depuração
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            callback.accept(faces, bitmap); // Passa o bitmap original para desenhar, por exemplo
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Erro na detecção de rosto: " + e.getMessage(), e);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Erro ao analisar frame da câmera: " + e.getMessage(), e);
        }
    }

    /**
     * Converte um Bitmap ARGB para o formato NV21, reutilizando buffers pré-alocados.
     *
     * @param bitmap O Bitmap ARGB de entrada.
     * @param nv21Buffer Buffer de bytes pré-alocado para os dados NV21.
     * @param argbBuffer Buffer de inteiros pré-alocado para os dados ARGB.
     */
    private void getNV21FromBitmap(Bitmap bitmap, byte[] nv21Buffer, int[] argbBuffer) {
        int inputWidth = bitmap.getWidth();
        int inputHeight = bitmap.getHeight();

        // Copia os pixels do bitmap para o buffer ARGB.
        // Otimização: A alocação de 'argb' é movida para fora do loop de análise.
        bitmap.getPixels(argbBuffer, 0, inputWidth, 0, 0, inputWidth, inputHeight);

        // Otimização: A alocação de 'yuv' (nv21Buffer) é movida para fora do loop de análise.
        encodeYUV420SP(nv21Buffer, argbBuffer, inputWidth, inputHeight);
    }

    /**
     * Codifica um array de pixels ARGB para o formato YUV420SP (NV21).
     *
     * @param yuv420sp O buffer de bytes para o resultado YUV420SP.
     * @param argb Os pixels ARGB de entrada.
     * @param width A largura da imagem.
     * @param height A altura da imagem.
     */
    private void encodeYUV420SP(byte[] yuv420sp, int[] argb, int width, int height) {
        final int frameSize = width * height;

        int yIndex = 0;
        int uvIndex = frameSize;

        // Prefira variáveis locais para acesso mais rápido em loops intensivos.
        int R, G, B, Y, U, V;

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                int argbPixel = argb[j * width + i]; // Acesso direto ao pixel

                R = (argbPixel >> 16) & 0xff; // Mais eficiente que mascarar e depois deslocar
                G = (argbPixel >> 8) & 0xff;
                B = argbPixel & 0xff;

                // Conversão RGB para YUV (valores aproximados para 0-255)
                // Usando deslocamentos de bits para otimizar multiplicações e divisões
                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
                V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;

                // Clampar valores entre 0 e 255
                yuv420sp[yIndex++] = (byte) (Y < 0 ? 0 : (Y > 255 ? 255 : Y));

                // Amostragem de U e V (chroma) a cada 2x2 pixels
                if (j % 2 == 0 && i % 2 == 0) {
                    yuv420sp[uvIndex++] = (byte) (V < 0 ? 0 : (V > 255 ? 255 : V));
                    yuv420sp[uvIndex++] = (byte) (U < 0 ? 0 : (U > 255 ? 255 : U));
                }
            }
        }
    }

    public void analyzeFromBitmap(@NonNull Bitmap bitmap) {
        try {
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            detector.process(image)
                    .addOnSuccessListener(faces -> {
                        Log.d(TAG, "Rostos detectados: " + faces.size());
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            callback.accept(faces, bitmap);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Erro na detecção facial: " + e.getMessage(), e);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Erro ao preparar imagem: " + e.getMessage(), e);
        }
    }

}