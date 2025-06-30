package com.sloth.registerapp.UI

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.sloth.registerapp.R
import dji.midware.usb.P3.UsbAccessoryService
import dji.sdk.camera.VideoFeeder
import dji.sdk.codec.DJICodecManager

class VideoFeedActivity : ComponentActivity() {

    private lateinit var videoFeedView: TextureView
    private var codecManager: DJICodecManager? = null
    private var videoDataListener: VideoFeeder.VideoDataListener? = null
    private val TAG = "VideoFeedActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_feed)

        videoFeedView = findViewById(R.id.textureView)

        initVideoFeed()
    }

    private fun initVideoFeed() {
        val context: Context = this

        videoFeedView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                Log.d(TAG, "SurfaceTexture disponível - iniciando codec manager")
                try {
                    codecManager = DJICodecManager(
                        context,
                        surface,
                        width,
                        height,
                        UsbAccessoryService.VideoStreamSource.Camera
                    )
                    Log.d(TAG, "Codec manager iniciado com sucesso.")
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao criar codec manager: ${e.message}")
                    Toast.makeText(context, "Erro ao iniciar codec", Toast.LENGTH_LONG).show()
                }
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                val bitmap: Bitmap? = videoFeedView.bitmap
                if (bitmap != null) {
                    Log.d(TAG, "Frame atualizado. Bitmap capturado.")
                    // Aqui você pode processar ou enviar a imagem
                }
            }
        }

        videoDataListener = VideoFeeder.VideoDataListener { videoBuffer, size ->
            if (codecManager != null) {
                codecManager?.sendDataToDecoder(
                    videoBuffer,
                    size,
                    UsbAccessoryService.VideoStreamSource.Camera.index
                )
            } else {
                Log.e(TAG, "codecManager está null, frame ignorado.")
            }
        }

        val primaryFeed = VideoFeeder.getInstance()?.primaryVideoFeed
        if (primaryFeed != null) {
            primaryFeed.addVideoDataListener(videoDataListener!!)
            Log.d(TAG, "Listener de vídeo registrado.")
        } else {
            Log.e(TAG, "Feed de vídeo primário está null!")
            Toast.makeText(this, "Feed de vídeo não disponível", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            videoDataListener?.let {
                VideoFeeder.getInstance()?.primaryVideoFeed?.removeVideoDataListener(it)
            }
            codecManager?.cleanSurface()
            codecManager = null
            videoDataListener = null
            Log.d(TAG, "Liberando recursos de vídeo.")
        } catch (e: Exception) {
            Log.e(TAG, "Erro no onDestroy: ${e.message}")
        }
    }
}
