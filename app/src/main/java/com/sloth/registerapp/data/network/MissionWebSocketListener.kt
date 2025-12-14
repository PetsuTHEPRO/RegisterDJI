package com.sloth.registerapp.data.network

import android.util.Log
import com.sloth.registerapp.data.model.ServerMission
import com.google.gson.Gson
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

private const val TAG = "MissionWebSocket"

class MissionWebSocketListener(private val onMissionReceived: (ServerMission) -> Unit) : WebSocketListener() {

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d(TAG, "WebSocket connection opened")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d(TAG, "Receiving: $text")
        try {
            val mission = Gson().fromJson(text, ServerMission::class.java)
            onMissionReceived(mission)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing mission from WebSocket message", e)
        }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(1000, null)
        Log.d(TAG, "Closing: $code / $reason")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e(TAG, "Error: " + t.message, t)
    }
}
