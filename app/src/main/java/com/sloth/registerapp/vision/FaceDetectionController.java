package com.sloth.registerapp.vision;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.sloth.registerapp.R;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FaceDetectionController {

    private final Activity activity;
    private final FrameLayout container;
    private final FaceEmbedder faceEmbedder;
    private final FaceDatabase faceDatabase;
    private final FaceDetector detector;
    private final Handler handler = new Handler();

    // Cache de embeddings por trackingId
    private final Map<Integer, float[]> embeddingCache = new HashMap<>();
    private final Map<Integer, View> overlayPool = new HashMap<>();
    private final Map<Integer, TextView> labelPool = new HashMap<>();

    private int frameCounter = 0;
    private static final int FRAME_SKIP = 2; // processa 1 de cada 2 frames

    public FaceDetectionController(Activity activity) {
        this.activity = activity;
        this.container = activity.findViewById(R.id.previewContainer);

        try {
            this.faceEmbedder = new FaceEmbedder(activity, "mobile_face_net.tflite");
        } catch (IOException e) {
            throw new RuntimeException("Erro ao carregar modelo .tflite", e);
        }
        this.faceDatabase = new FaceDatabase(activity);

        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .enableTracking()
                        .build();

        this.detector = FaceDetection.getClient(options);
    }

    /** 🔹 Chamado pelo VideoFeedActivity a cada frame */
    public void processFrame(Bitmap frameBitmap) {
        if (++frameCounter % FRAME_SKIP != 0) return;

        if (frameBitmap == null || frameBitmap.isRecycled()) return;

        InputImage image = InputImage.fromBitmap(frameBitmap, 0);

        detector.process(image)
                .addOnSuccessListener(faces -> drawCirclesAndRecognizeFaces(faces, frameBitmap))
                .addOnFailureListener(e -> e.printStackTrace());
    }

    private void drawCirclesAndRecognizeFaces(List<Face> faces, final Bitmap currentFrameBitmap) {
        clearPreviousOverlays();

        for (Face face : faces) {
            Rect box = face.getBoundingBox();
            if (box.width() < 60 || box.height() < 60) continue;

            int left = Math.max(0, box.left);
            int top = Math.max(0, box.top);
            int right = Math.min(currentFrameBitmap.getWidth(), box.right);
            int bottom = Math.min(currentFrameBitmap.getHeight(), box.bottom);
            int width = right - left;
            int height = bottom - top;

            if (width <= 0 || height <= 0) continue;

            Bitmap faceBitmap = Bitmap.createBitmap(currentFrameBitmap, left, top, width, height);

            float[] embedding;
            Integer trackingId = face.getTrackingId();
            if (trackingId != null && embeddingCache.containsKey(trackingId)) {
                embedding = embeddingCache.get(trackingId);
            } else {
                embedding = faceEmbedder.getEmbedding(faceBitmap);
                if (trackingId != null) embeddingCache.put(trackingId, embedding);
            }

            String name = recognize(embedding);

            // Desenha overlay circular
            View circle = overlayPool.get(trackingId);
            if (circle == null) {
                circle = new View(activity);
                if ("Desconhecido".equals(name)) {
                    circle.setBackgroundResource(R.drawable.circle_overlay_white);
                } else {
                    circle.setBackgroundResource(R.drawable.circle_overlay_green);
                }
                overlayPool.put(trackingId != null ? trackingId : box.hashCode(), circle);
                container.addView(circle);
            }

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(box.width(), box.width());
            params.leftMargin = box.centerX() - box.width() / 2;
            params.topMargin = box.centerY() - box.width() / 2;
            circle.setLayoutParams(params);
            circle.setVisibility(View.VISIBLE);

            // Label do nome
            TextView label = labelPool.get(trackingId);
            if (label == null) {
                label = new TextView(activity);
                label.setTextColor(Color.WHITE);
                label.setBackgroundColor(Color.BLACK);
                label.setPadding(8, 4, 8, 4);
                label.setTextSize(12);
                labelPool.put(trackingId != null ? trackingId : box.hashCode(), label);
                container.addView(label);
            }
            label.setText(name);
            FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            textParams.leftMargin = params.leftMargin;
            textParams.topMargin = params.topMargin - 40;
            label.setLayoutParams(textParams);
            label.setVisibility(View.VISIBLE);

            // Clique para cadastrar
            final Rect clickedFaceBox = new Rect(left, top, right, bottom);
            final Bitmap clickedFrameBitmap = currentFrameBitmap.copy(currentFrameBitmap.getConfig(), false);
            circle.setOnClickListener(v -> showNameInputDialog(clickedFaceBox, clickedFrameBitmap));
        }
    }

    private void clearPreviousOverlays() {
        for (View v : overlayPool.values()) v.setVisibility(View.GONE);
        for (TextView t : labelPool.values()) t.setVisibility(View.GONE);
    }

    private void showNameInputDialog(Rect box, final Bitmap frameBitmap) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Cadastrar rosto");

        EditText input = new EditText(activity);
        input.setHint("Digite o nome");
        builder.setView(input);

        builder.setPositiveButton("Salvar", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                saveFace(name, box, frameBitmap);
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void saveFace(String name, Rect box, Bitmap frameBitmap) {
        if (frameBitmap == null) {
            Toast.makeText(activity, "Erro: Bitmap não disponível.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Rect safeBox = new Rect(
                    Math.max(0, box.left),
                    Math.max(0, box.top),
                    Math.min(frameBitmap.getWidth(), box.right),
                    Math.min(frameBitmap.getHeight(), box.bottom)
            );

            int width = safeBox.width();
            int height = safeBox.height();
            if (width <= 0 || height <= 0) return;

            Bitmap faceBitmap = Bitmap.createBitmap(
                    frameBitmap,
                    safeBox.left,
                    safeBox.top,
                    width,
                    height
            );

            FaceSaver.saveFaceImage(activity, name, faceBitmap);
            float[] embedding = faceEmbedder.getEmbedding(faceBitmap);
            faceDatabase.addEmbedding(name, embedding);

            Toast.makeText(activity, "Rosto cadastrado: " + name, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(activity, "Erro ao salvar rosto: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            if (frameBitmap != null && !frameBitmap.isRecycled()) {
                frameBitmap.recycle();
            }
        }
    }

    private String recognize(float[] currentEmbedding) {
        Map<String, float[]> allEmbeddings = faceDatabase.getAllEmbeddings();
        String bestMatch = "Desconhecido";
        float bestDistance = Float.MAX_VALUE;

        for (Map.Entry<String, float[]> entry : allEmbeddings.entrySet()) {
            float distance = calculateEuclideanDistance(currentEmbedding, entry.getValue());
            if (distance < bestDistance) {
                bestDistance = distance;
                bestMatch = entry.getKey();
            }
        }

        return (bestDistance > 1.0f) ? "Desconhecido" : bestMatch;
    }

    private float calculateEuclideanDistance(float[] emb1, float[] emb2) {
        float sum = 0f;
        for (int i = 0; i < emb1.length; i++) {
            float diff = emb1[i] - emb2[i];
            sum += diff * diff;
        }
        return (float) Math.sqrt(sum);
    }
}
