package com.sloth.registerapp.vision;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class Permissions {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "PermissionsHelper";

    /**
     * Verifica as permissões necessárias.
     * @return true se todas as permissões já estão concedidas, false se uma solicitação foi feita.
     */
    public static boolean checkAndRequestPermissions(Activity activity) {
        Log.d(TAG, "checkAndRequestPermissions() chamado.");

        List<String> permissionsNeeded = new ArrayList<>();

        // Permissão da Câmera
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA);
        }

        // Permissões de Armazenamento (adaptado para novas versões do Android)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        // Se a lista de permissões necessárias não está vazia, solicita
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(activity,
                    permissionsNeeded.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
            return false; // CORREÇÃO: Retorna false porque tivemos que pedir permissão
        }

        // Se a lista está vazia, todas as permissões já foram concedidas
        Log.d(TAG, "Todas as permissões essenciais já concedidas.");
        return true; // CORREÇÃO: Retorna true porque as permissões já estavam OK
    }
}