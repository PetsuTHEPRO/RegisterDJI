package com.sloth.registerapp.vision;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

public class FaceSaver {

    private static final String TAG = "FaceSaver";

    /**
     * Salva um BITMAP em uma pasta pública na galeria (DCIM/Drone App/face-search).
     * Este método unificado funciona para todas as versões do Android.
     *
     * @param context    O contexto da aplicação.
     * @param name       O nome base para o arquivo da imagem.
     * @param faceBitmap O Bitmap da imagem a ser salva.
     */
    public static void saveFaceToGallery(Context context, String name, Bitmap faceBitmap) {
        String cleanName = name.replaceAll("[^a-zA-Z0-9.-]", "_");
        String fileName = cleanName + "_" + System.currentTimeMillis() + ".jpg";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveBitmapWithMediaStore(context, fileName, faceBitmap);
        } else {
            saveBitmapWithLegacyMethod(context, fileName, faceBitmap);
        }
    }

    /**
     * NOVO MÉTODO: Salva uma imagem a partir de uma URI de origem para a galeria pública.
     * Tradução direta da sua lógica em Kotlin.
     *
     * @param context   O contexto da aplicação.
     * @param sourceUri A Uri da imagem que será copiada.
     */
    public static void saveUriToGallery(Context context, Uri sourceUri) {
        ContentResolver contentResolver = context.getContentResolver();

        // 1. Definir os metadados da imagem
        String fileName = "face_" + System.currentTimeMillis() + ".jpg";
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

        // Define a subpasta dentro de DCIM para Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String relativePath = Environment.DIRECTORY_DCIM + File.separator + "Drone App" + File.separator + "face-search";
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);
        }

        Uri destinationUri = null;
        try {
            // 2. Criar uma entrada vazia no MediaStore para obter a URI de destino
            destinationUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            Objects.requireNonNull(destinationUri, "Falha ao criar arquivo de mídia.");

            // 3. Copiar os dados da imagem original para a nova URI de destino
            try (InputStream inputStream = contentResolver.openInputStream(sourceUri);
                 OutputStream outputStream = contentResolver.openOutputStream(destinationUri)) {

                Objects.requireNonNull(inputStream, "Falha ao abrir o input stream da URI de origem.");
                Objects.requireNonNull(outputStream, "Falha ao abrir o output stream da URI de destino.");

                // Copia os bytes do arquivo
                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, len);
                }
            }

            Log.d(TAG, "Imagem da URI salva com sucesso em: " + destinationUri);
            Toast.makeText(context, "Imagem salva com sucesso!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            // Se ocorreu um erro, remove a entrada de mídia que pode ter sido criada
            if (destinationUri != null) {
                contentResolver.delete(destinationUri, null, null);
            }
            Log.e(TAG, "Erro ao salvar imagem da URI: " + e.getMessage());
            Toast.makeText(context, "Erro ao salvar imagem.", Toast.LENGTH_SHORT).show();
        }
    }


    // --- MÉTODOS PRIVADOS PARA SALVAR BITMAP ---

    private static void saveBitmapWithMediaStore(Context context, String fileName, Bitmap faceBitmap) {
        ContentResolver contentResolver = context.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        String relativePath = Environment.DIRECTORY_DCIM + File.separator + "Drone App" + File.separator + "face-search";
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);

        Uri imageUri = null;
        try {
            Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            imageUri = contentResolver.insert(contentUri, contentValues);
            Objects.requireNonNull(imageUri, "Falha ao criar entrada no MediaStore.");

            try (OutputStream outputStream = contentResolver.openOutputStream(imageUri)) {
                Objects.requireNonNull(outputStream, "Falha ao obter o output stream.");
                faceBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
                Log.d(TAG, "Bitmap salvo na galeria em: " + imageUri);
                Toast.makeText(context, "Imagem salva com sucesso!", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            if (imageUri != null) {
                contentResolver.delete(imageUri, null, null);
            }
            Log.e(TAG, "Erro ao salvar bitmap com MediaStore: " + e.getMessage());
            Toast.makeText(context, "Erro ao salvar imagem.", Toast.LENGTH_SHORT).show();
        }
    }

    private static void saveBitmapWithLegacyMethod(Context context, String fileName, Bitmap faceBitmap) {
        String imageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString()
                + File.separator + "Drone App" + File.separator + "face-search";

        File droneAppDir = new File(imageDir);
        if (!droneAppDir.exists()) {
            if (!droneAppDir.mkdirs()) {
                Log.e(TAG, "Não foi possível criar o diretório: " + droneAppDir.getAbsolutePath());
                Toast.makeText(context, "Erro ao criar diretório.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        File imageFile = new File(droneAppDir, fileName);
        try (FileOutputStream out = new FileOutputStream(imageFile)) {
            faceBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            Log.d(TAG, "Bitmap salvo em (legado): " + imageFile.getAbsolutePath());

            MediaScannerConnection.scanFile(context,
                    new String[]{imageFile.getAbsolutePath()},
                    new String[]{"image/jpeg"},
                    null);

            Toast.makeText(context, "Imagem salva com sucesso!", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Log.e(TAG, "Erro ao salvar bitmap (legado): " + e.getMessage());
            Toast.makeText(context, "Erro ao salvar imagem.", Toast.LENGTH_SHORT).show();
        }
    }
}
