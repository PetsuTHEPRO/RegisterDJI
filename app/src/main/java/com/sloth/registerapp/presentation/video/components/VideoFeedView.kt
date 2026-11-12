package com.sloth.registerapp.presentation.video.components

import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun VideoFeedView(
    modifier: Modifier = Modifier,
    onTextureViewReady: (TextureView) -> Unit = {},
    onSurfaceTextureAvailable: (SurfaceTexture, Int, Int) -> Unit,
    onSurfaceTextureSizeChanged: (SurfaceTexture, Int, Int) -> Unit = { _, _, _ -> },
    onSurfaceTextureDestroyed: () -> Boolean
) {
    AndroidView(
        factory = { context ->
            TextureView(context).apply {
                onTextureViewReady(this)
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                        onSurfaceTextureAvailable(surface, width, height)
                    }

                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                        onSurfaceTextureSizeChanged(surface, width, height)
                    }

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        return onSurfaceTextureDestroyed()
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                }
            }
        },
        update = { textureView ->
            onTextureViewReady(textureView)
            if (textureView.isAvailable && textureView.surfaceTexture != null && textureView.width > 0 && textureView.height > 0) {
                onSurfaceTextureAvailable(textureView.surfaceTexture!!, textureView.width, textureView.height)
            }
        },
        modifier = modifier
    )
}
