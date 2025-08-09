package com.sloth.registerapp.vision;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;

public class FaceSaver {

    public static void saveFaceImage(Context context, String name, Bitmap bitmap) {
        try {
            // Salva dentro da pasta da aplicação -> Android/data/com.sloth.registerapp/files/Pictures/facesCadastradas
            File directory = new File(
                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "facesCadastradas"
            );

            if (!directory.exists()) {
                directory.mkdirs();
            }

            String fileName = name + "_" + System.currentTimeMillis() + ".jpg";
            File file = new File(directory, fileName);

            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();

            Toast.makeText(context, "Imagem salva em: " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(context, "Erro ao salvar imagem: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}
