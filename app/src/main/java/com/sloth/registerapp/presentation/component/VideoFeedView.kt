package com.sloth.registerapp.presentation.component

import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import dji.sdk.camera.VideoFeeder
import dji.sdk.codec.DJICodecManager

@Composable
fun VideoFeedView() {
    val context = LocalContext.current
    val textureView = remember { TextureView(context) }
    var codecManager: DJICodecManager? = null
    var videoDataListener: VideoFeeder.VideoDataListener? = null

    DisposableEffect(Unit) {
        val primaryFeed = VideoFeeder.getInstance()?.primaryVideoFeed

        if (primaryFeed != null) {
            videoDataListener = VideoFeeder.VideoDataListener { videoBuffer, size ->
                codecManager?.sendDataToDecoder(videoBuffer, size)
            }

            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                    if (codecManager == null) {
                        codecManager = DJICodecManager(context, surface, width, height)
                    }
                }

                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    codecManager?.cleanSurface()
                    codecManager = null
                    return false
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
            }

            primaryFeed.addVideoDataListener(videoDataListener)
        }

        onDispose {
            videoDataListener?.let {
                primaryFeed?.removeVideoDataListener(it)
            }
            codecManager?.destroyCodec()
        }
    }

    AndroidView(factory = { textureView })
}
