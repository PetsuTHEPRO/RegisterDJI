package com.sloth.registerapp.vision;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.google.mlkit.vision.face.Face;
import com.sloth.registerapp.R;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FaceDetectionController {

    private final Activity activity;
    private final FaceAnalyzer faceAnalyzer;
    private final CameraPreviewManager previewManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final FrameLayout container;

    private final FaceEmbedder faceEmbedder;
    private final FaceDatabase faceDatabase;

    private int frameCounter = 0;
    private static final int FRAME_SKIP = 2;
    private final Map<Integer, float[]> embeddingCache = new HashMap<>();
    private final Map<Integer, View> overlayPool = new HashMap<>();
    private final Map<Integer, TextView> labelPool = new HashMap<>();

    public FaceDetectionController(Activity activity) {
        this.activity = activity;
        this.container = activity.findViewById(R.id.previewContainer);

        try {
            this.faceEmbedder = new FaceEmbedder(activity, "mobile_face_net.tflite");
        } catch (IOException e) {
            throw new RuntimeException("Erro ao carregar modelo .tflite", e);
        }

        this.faceDatabase = new FaceDatabase(activity);
        this.faceAnalyzer = new FaceAnalyzer(this::drawCirclesAndRecognizeFaces);

        // Esta classe agora espera um construtor que só precise da Activity
        this.previewManager = new CameraPreviewManager(activity);

        // ALTERAÇÃO: A inicialização do loop foi removida daqui para ser controlada pela Activity.
    }

    // ALTERAÇÃO: Adicionado método para a Activity iniciar a detecção.
    public void start() {
        handler.post(detectionLoop);
    }

    // ALTERAÇÃO: Adicionado método para a Activity parar a detecção.
    public void stop() {
        handler.removeCallbacks(detectionLoop);
    }

    private final Runnable detectionLoop = new Runnable() {
        @Override
        public void run() {
            TextureView textureView = previewManager.getTextureView();
            if (textureView != null && textureView.isAvailable()) {
                // Usa a versão antiga do analyzeFast, sem o parâmetro de rotação.
                faceAnalyzer.analyzeFast(textureView);
            }
            handler.postDelayed(this, 500);
        }
    };

    // NENHUMA MUDANÇA DAQUI PARA BAIXO. TODA A SUA LÓGICA ORIGINAL FOI MANTIDA.
    private void drawCirclesAndRecognizeFaces(List<Face> faces, final Bitmap currentFrameBitmap) {
        if (++frameCounter % FRAME_SKIP != 0) return;

        activity.runOnUiThread(() -> {
            clearPreviousOverlays();

            for (Face face : faces) {
                Rect box = face.getBoundingBox();
                if (box.width() < 60 || box.height() < 60) continue;

                int x = box.centerX();
                int y = box.centerY();
                int size = box.width();

                int left = Math.max(0, box.left);
                int top = Math.max(0, box.top);
                int width = Math.min(box.width(), currentFrameBitmap.getWidth() - left);
                int height = Math.min(box.height(), currentFrameBitmap.getHeight() - top);
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

                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
                params.leftMargin = x - size / 2;
                params.topMargin = y - size / 2;
                circle.setLayoutParams(params);
                circle.setVisibility(View.VISIBLE);

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

                circle.setOnClickListener(v -> showNameInputDialog(box, currentFrameBitmap.copy(currentFrameBitmap.getConfig(), false)));
            }
        });
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
            } else if (frameBitmap != null && !frameBitmap.isRecycled()) {
                frameBitmap.recycle();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> {
            dialog.cancel();
            if (frameBitmap != null && !frameBitmap.isRecycled()) {
                frameBitmap.recycle();
            }
        });
        builder.show();
    }

    private void saveFace(String name, Rect box, Bitmap frameBitmap) {
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

            Bitmap faceBitmap = Bitmap.createBitmap(frameBitmap, safeBox.left, safeBox.top, width, height);

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