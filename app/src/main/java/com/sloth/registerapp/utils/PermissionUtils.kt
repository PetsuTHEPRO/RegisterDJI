package com.sloth.registerapp.utils

import android.Manifest

// Data class para armazenar um nome amigável e uma descrição para cada permissão.
data class PermissionInfo(
    val name: String,
    val description: String
)

// Mapeia as constantes de permissão do Android para nossos nomes e descrições amigáveis.
object PermissionUtils {
    val permissionMap = mapOf(
        Manifest.permission.CAMERA to PermissionInfo("Câmera", "Necessária para capturar fotos e vídeos."),
        Manifest.permission.VIBRATE to PermissionInfo("Vibração", "Usada para fornecer feedback tátil."),
        Manifest.permission.INTERNET to PermissionInfo("Acesso à Internet", "Essencial para comunicação com a rede."),
        Manifest.permission.ACCESS_WIFI_STATE to PermissionInfo("Status do Wi-Fi", "Verifica o estado da conexão Wi-Fi."),
        Manifest.permission.WAKE_LOCK to PermissionInfo("Manter Ativo (Wake Lock)", "Impede o dispositivo de dormir durante operações críticas."),
        Manifest.permission.ACCESS_COARSE_LOCATION to PermissionInfo("Localização Aproximada", "Acessa sua localização de forma aproximada."),
        Manifest.permission.ACCESS_FINE_LOCATION to PermissionInfo("Localização Precisa", "Acessa sua localização exata via GPS."),
        Manifest.permission.BLUETOOTH to PermissionInfo("Bluetooth (Legado)", "Para dispositivos Bluetooth mais antigos."),
        Manifest.permission.BLUETOOTH_ADMIN to PermissionInfo("Admin Bluetooth (Legado)", "Para gerenciar dispositivos Bluetooth antigos."),
        Manifest.permission.BLUETOOTH_CONNECT to PermissionInfo("Conexão Bluetooth", "Necessária para se conectar a dispositivos Bluetooth."),
        Manifest.permission.BLUETOOTH_SCAN to PermissionInfo("Busca Bluetooth", "Necessária para encontrar dispositivos Bluetooth próximos."),
        Manifest.permission.BLUETOOTH_ADVERTISE to PermissionInfo("Anúncio Bluetooth", "Permite que o app seja visível para outros dispositivos."),
        Manifest.permission.READ_MEDIA_IMAGES to PermissionInfo("Ler Imagens", "Acessa as imagens armazenadas no dispositivo."),
        Manifest.permission.READ_MEDIA_VIDEO to PermissionInfo("Ler Vídeos", "Acessa os vídeos armazenados no dispositivo."),
        Manifest.permission.READ_MEDIA_AUDIO to PermissionInfo("Ler Áudio", "Acessa os áudios armazenados no dispositivo."),
        Manifest.permission.READ_PHONE_STATE to PermissionInfo("Status do Telefone", "Lê informações do estado do telefone."),
        Manifest.permission.RECORD_AUDIO to PermissionInfo("Gravar Áudio", "Necessária para gravar áudio com o microfone.")
    )

    fun getInfo(permission: String): PermissionInfo {
        return permissionMap[permission] ?: PermissionInfo("Desconhecida", "Permissão não documentada.")
    }
}