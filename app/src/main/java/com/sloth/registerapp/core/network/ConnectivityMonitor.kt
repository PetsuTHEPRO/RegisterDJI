package com.sloth.registerapp.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConnectivityMonitor private constructor(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isConnected = MutableStateFlow(isConnectedNow())
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val networkRequest = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        .build()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "🔗 Internet DISPONÍVEL - Sincronização pode começar")
            _isConnected.value = true
        }

        override fun onLost(network: Network) {
            Log.d(TAG, "📡 Internet PERDIDA - Modo offline ativado")
            _isConnected.value = false
        }
    }

    init {
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    fun isConnectedNow(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    companion object {
        private const val TAG = "ConnectivityMonitor"

        @Volatile
        private var INSTANCE: ConnectivityMonitor? = null

        fun getInstance(context: Context): ConnectivityMonitor {
            return INSTANCE ?: synchronized(this) {
                val instance = ConnectivityMonitor(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}