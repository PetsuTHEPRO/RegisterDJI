package com.sloth.registerapp.features.streaming.data

import android.util.Log
import android.view.SurfaceView
import com.pedro.rtmp.utils.ConnectCheckerRtmp
import com.pedro.rtplibrary.rtmp.RtmpCamera2
import com.sloth.registerapp.features.streaming.domain.StreamState
import com.sloth.registerapp.features.streaming.domain.StreamingController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PhoneRtmpStreamer(
    private val surfaceView: SurfaceView,
    private var rtmpUrl: String,
    private val config: VideoConfig = VideoConfig()
) : StreamingController {

    data class VideoConfig(
        val enableAudio: Boolean = false
    )

    private val _state = MutableStateFlow<StreamState>(StreamState.Idle)
    override val state: StateFlow<StreamState> = _state

    private val tag = "PhoneRtmpStreamer"

    private val rtmpCamera = RtmpCamera2(surfaceView, object : ConnectCheckerRtmp {
        override fun onConnectionSuccessRtmp() {
            _state.value = StreamState.Streaming
            Log.d(tag, "RTMP conectado")
        }

        override fun onConnectionFailedRtmp(reason: String) {
            Log.e(tag, "Falha RTMP: $reason")
            _state.value = StreamState.Error("Falha RTMP: $reason")
            if (rtmpCamera.isStreaming) {
                rtmpCamera.stopStream()
            }
        }

        override fun onDisconnectRtmp() {
            _state.value = StreamState.Idle
            Log.d(tag, "RTMP desconectado")
        }

        override fun onAuthErrorRtmp() {
            _state.value = StreamState.Error("Erro de autenticação RTMP")
            Log.e(tag, "Erro de autenticação RTMP")
        }

        override fun onAuthSuccessRtmp() {
            Log.d(tag, "Autenticação RTMP OK")
        }
    })

    override fun start() {
        if (_state.value is StreamState.Streaming || _state.value is StreamState.Connecting) return
        _state.value = StreamState.Connecting

        try {
            val videoPrepared = rtmpCamera.prepareVideo()
            val audioPrepared = if (config.enableAudio) {
                rtmpCamera.prepareAudio()
            } else {
                true
            }

            if (!videoPrepared || !audioPrepared) {
                _state.value = StreamState.Error("Falha ao preparar stream")
                Log.e(tag, "Falha ao preparar stream")
                return
            }

            rtmpCamera.startStream(rtmpUrl)
            Log.d(tag, "RTMP startStream chamado")
        } catch (e: Exception) {
            _state.value = StreamState.Error("Erro ao iniciar stream: ${e.message}")
            Log.e(tag, "Erro ao iniciar stream", e)
        }
    }

    override fun stop() {
        try {
            if (rtmpCamera.isStreaming) {
                rtmpCamera.stopStream()
            }
            _state.value = StreamState.Idle
            Log.d(tag, "RTMP streaming parado")
        } catch (e: Exception) {
            _state.value = StreamState.Error("Erro ao parar stream: ${e.message}")
            Log.e(tag, "Erro ao parar stream", e)
        }
    }

    override fun release() {
        stop()
        stopPreview()
    }

    fun updateUrl(url: String) {
        rtmpUrl = url
    }

    fun startPreview() {
        try {
            if (!rtmpCamera.isOnPreview) {
                rtmpCamera.startPreview()
            }
        } catch (e: Exception) {
            Log.e(tag, "Erro ao iniciar preview", e)
        }
    }

    fun stopPreview() {
        try {
            if (rtmpCamera.isOnPreview) {
                rtmpCamera.stopPreview()
            }
        } catch (e: Exception) {
            Log.e(tag, "Erro ao parar preview", e)
        }
    }
}
