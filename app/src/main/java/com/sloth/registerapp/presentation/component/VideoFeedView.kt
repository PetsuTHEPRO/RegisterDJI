package com.sloth.registerapp.presentation.component

import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun VideoFeedView(
    modifier: Modifier = Modifier,
    onSurfaceTextureAvailable: (SurfaceTexture, Int, Int) -> Unit,
    onSurfaceTextureDestroyed: () -> Boolean
) {
    AndroidView(
        factory = { context ->
            TextureView(context).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                        onSurfaceTextureAvailable(surface, width, height)
                    }

                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        return onSurfaceTextureDestroyed()
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                }
            }
        },
        modifier = modifier
    )
}
