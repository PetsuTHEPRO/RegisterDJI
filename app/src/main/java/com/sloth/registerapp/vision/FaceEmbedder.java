package com.sloth.registerapp.vision;

import android.content.Context;
import android.graphics.Bitmap;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class FaceEmbedder {

    private static final int INPUT_SIZE = 112; // mobilefacenet espera 112x112
    private static final int EMBEDDING_SIZE = 192;
    private Interpreter interpreter;

    public FaceEmbedder(Context context, String modelPath) throws IOException {
        interpreter = new Interpreter(loadModelFile(context, modelPath));
    }

    private MappedByteBuffer loadModelFile(Context context, String modelPath) throws IOException {
        FileInputStream inputStream = new FileInputStream(context.getAssets().openFd(modelPath).getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = context.getAssets().openFd(modelPath).getStartOffset();
        long declaredLength = context.getAssets().openFd(modelPath).getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public float[] getEmbedding(Bitmap faceBitmap) {
        // Preprocessa a imagem para ByteBuffer float normalizado
        ByteBuffer imgData = convertBitmapToBuffer(faceBitmap);

        float[][] embedding = new float[1][EMBEDDING_SIZE];
        interpreter.run(imgData, embedding);
        return embedding[0];
    }

    private ByteBuffer convertBitmapToBuffer(Bitmap bitmap) {
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);

        ByteBuffer buffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3);
        buffer.order(ByteOrder.nativeOrder());
        buffer.rewind();

        int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];
        scaled.getPixels(intValues, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE);

        for (int pixel : intValues) {
            // Extrai canais RGB e normaliza [-1, 1]
            float r = ((pixel >> 16) & 0xFF) / 255f;
            float g = ((pixel >> 8) & 0xFF) / 255f;
            float b = (pixel & 0xFF) / 255f;

            buffer.putFloat((r - 0.5f) * 2);
            buffer.putFloat((g - 0.5f) * 2);
            buffer.putFloat((b - 0.5f) * 2);
        }
        buffer.rewind();
        return buffer;
    }
}