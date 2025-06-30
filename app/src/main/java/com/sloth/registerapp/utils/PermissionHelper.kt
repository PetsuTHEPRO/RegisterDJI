package com.sloth.registerapp.utils

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

object PermissionHelper {

    private const val TAG = "ApplicationDJI"

    // A lista de permissões agora vive aqui.
    private val REQUIRED_PERMISSION_LIST: Array<String> = arrayOf(
        Manifest.permission.CAMERA, // <-- ADICIONE ESTA LINHA AQUI
        Manifest.permission.VIBRATE,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.WAKE_LOCK,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_AUDIO,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.RECORD_AUDIO
    )

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var systemAlertWindowLauncher: ActivityResultLauncher<Intent>
    private lateinit var manageExternalStorageLauncher: ActivityResultLauncher<Intent>

    // Função que inicializa os launchers. Ela precisa da Activity para registrá-los.
    fun initializeLaunchers(activity: ComponentActivity, onAllPermissionsGranted: () -> Unit) {
        requestPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissionsMap ->
            if (permissionsMap.all { it.value }) {
                Log.d(TAG, "Todas as permissões de runtime concedidas.")
                checkSpecialPermissions(activity, onAllPermissionsGranted)
            } else {
                val deniedPermissions = permissionsMap.filter { !it.value }.keys
                Log.d(TAG, "Permissões de runtime negadas: ${deniedPermissions.joinToString()}")
                Toast.makeText(activity, "Permissões de runtime necessárias negadas.", Toast.LENGTH_LONG).show()
            }
        }

        systemAlertWindowLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            checkSpecialPermissions(activity, onAllPermissionsGranted)
        }

        manageExternalStorageLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            checkSpecialPermissions(activity, onAllPermissionsGranted)
        }
    }

    // Função principal que inicia todo o processo de verificação.
    fun checkAndRequestPermissions(activity: ComponentActivity, onAllPermissionsGranted: () -> Unit) {
        val missingPermissions = REQUIRED_PERMISSION_LIST.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            Log.d(TAG, "Todas as permissões de runtime já concedidas.")
            checkSpecialPermissions(activity, onAllPermissionsGranted)
        }
    }

    // Função que verifica as permissões especiais (desenhar sobre outros apps, gerenciar armazenamento).
    private fun checkSpecialPermissions(activity: ComponentActivity, onAllPermissionsGranted: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(activity)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${activity.packageName}"))
            systemAlertWindowLauncher.launch(intent)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${activity.packageName}"))
            manageExternalStorageLauncher.launch(intent)
            return
        }

        // Se todas as permissões (runtime e especiais) foram concedidas,
        // executa a ação final que a MainActivity nos passou.
        Log.d(TAG, "Todas as permissões (runtime e especiais) foram concedidas.")
        onAllPermissionsGranted()
    }
}