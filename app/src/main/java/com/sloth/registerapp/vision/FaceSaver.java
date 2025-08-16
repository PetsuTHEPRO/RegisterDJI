package com.sloth.registerapp.vision;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FaceSaver {

    private static final String TAG = "FaceSaver";

    public static void saveFaceImage(Context context, String name, Bitmap faceBitmap) {
        File facesDir = new File(context.getExternalFilesDir(null), "faces");
        if (!facesDir.exists()) {
            facesDir.mkdirs();
        }

        String fileName = name.replaceAll("[^a-zA-Z0-9.-]", "_") + "_" + System.currentTimeMillis() + ".jpg";
        File imageFile = new File(facesDir, fileName);

        try (FileOutputStream out = new FileOutputStream(imageFile)) {
            faceBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            Log.d(TAG, "Imagem do rosto salva em: " + imageFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Erro ao salvar imagem do rosto: " + e.getMessage());
            Toast.makeText(context, "Erro ao salvar imagem.", Toast.LENGTH_SHORT).show();
        }
    }
}