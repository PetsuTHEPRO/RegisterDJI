package com.sloth.registerapp.data.sdk

import android.content.Context
import android.util.Log
import android.widget.Toast
import dji.common.error.DJIError
import dji.common.error.DJISDKError
import dji.sdk.base.BaseComponent
import dji.sdk.base.BaseProduct
import dji.sdk.sdkmanager.DJISDKInitEvent
import dji.sdk.sdkmanager.DJISDKManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object DJIConnectionHelper {

    private const val TAG = "ApplicationDJI"

    // StateFlow para emitir o status da conexão para a UI
    private val _connectionStatus = MutableStateFlow("Aguardando inicialização...")
    val connectionStatus = _connectionStatus.asStateFlow()

    // StateFlow para emitir o produto (drone) quando ele se conecta
    private val _product = MutableStateFlow<BaseProduct?>(null)
    val product = _product.asStateFlow()

    // Função para obter a instância do produto de forma síncrona
    fun getProductInstance(): BaseProduct? = _product.value

    // Função principal que a MainActivity chamará
    fun registerApp(context: Context) {
        _connectionStatus.value = "Registrando aplicativo..."
        Log.d(TAG, "Iniciando o registro do SDK DJI...")

        DJISDKManager.getInstance().registerApp(context, object : DJISDKManager.SDKManagerCallback {
            override fun onRegister(error: DJIError?) {
                if (error == DJISDKError.REGISTRATION_SUCCESS) {
                    Log.d(TAG, "Registro do SDK bem-sucedido!")
                    _connectionStatus.value = "Pronto para Conexão"
                    // Não precisa de runOnUiThread aqui, o Toast já faz isso se necessário
                    Toast.makeText(context, "Registro do SDK bem-sucedido!", Toast.LENGTH_SHORT).show()
                    DJISDKManager.getInstance().startConnectionToProduct()
                } else {
                    Log.d(TAG, "Falha no registro do SDK: ${error?.description}")
                    _connectionStatus.value = "Falha no registro"
                    Toast.makeText(context, "Falha no registro: ${error?.description}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onProductDisconnect() {
                Log.d(TAG, "Produto DJI desconectado.")
                _connectionStatus.value = "Produto Desconectado"
                _product.value = null
            }

            override fun onProductConnect(product: BaseProduct?) {
                val modelName = product?.model?.displayName ?: "Modelo Desconhecido"
                Log.d(TAG, "Produto DJI conectado: $modelName")
                _connectionStatus.value = "Conectado a: $modelName"
                _product.value = product
            }

            override fun onProductChanged(product: BaseProduct?) {
                _product.value = product
            }

            override fun onComponentChange(key: BaseProduct.ComponentKey?, oldC: BaseComponent?, newC: BaseComponent?) {}
            override fun onInitProcess(event: DJISDKInitEvent?, totalProcess: Int) {}
            override fun onDatabaseDownloadProgress(current: Long, total: Long) {}
        })
    }
}