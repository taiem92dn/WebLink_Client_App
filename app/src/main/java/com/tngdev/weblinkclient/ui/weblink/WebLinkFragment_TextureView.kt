package com.tngdev.weblinkclient.ui.weblink

import android.graphics.Point
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import com.tngdev.weblinkclient.R


/**
 * Extension of the {@link WebLinkFragment} that uses TextureView as the target view for the video
 * decoder.
 */
class WebLinkFragment_TextureView: WebLinkFragment() {

    private var videoView: TextureView? = null

    override fun prepareVideoView(fragmentView: View?) {
        // The layout consists of a FrameLayout as the root and a single TextureView with an
        // id of `video_view` as the child of the root FrameLayout.
        val textureView = fragmentView?.findViewById<TextureView>(R.id.video_view)

        videoView = textureView
        textureView?.surfaceTextureListener = object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                val renderSize = wlClient.renderSize
                mScaleX = if (width > 0) renderSize.x / width.toFloat() else 1.0f
                mScaleY = if (height > 0) renderSize.y / height.toFloat() else 1.0f
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                val renderSize = wlClient.renderSize
                mScaleX = if (width > 0) renderSize.x / width.toFloat() else 1.0f
                mScaleY = if (height > 0) renderSize.y / height.toFloat() else 1.0f
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                Log.d(TAG, "onSurfaceTextureDestroyed!")
                //Perform the pausing when the texture is destroyed
                //this represents when the texture is no longer valid
                //to the system so we will have to recreate it next start() cycle.
                if (wlClient.isConnected) {
                    val res = wlClient.pauseVideoEncoding()
                    if (!res) {
                        wlClient.disconnect()
                    }
                }

                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            }

        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.fragment_weblink_textureview
    }

    override fun getVideoViewDimensions(): Point {
        return Point(
            videoView?.width ?: 0,
            videoView?.height ?: 0
        )
    }

    override fun onFragmentStarted() {
        //due to the weird stop->start transition when turning screen on / off on
        //some systems, only attach if the view isn't setup already.
        if (videoView?.surfaceTexture == null) {
            videoView?.setSurfaceTexture(wlClient.surface.surfaceTexture)
        }
    }

    override fun onFragmentStopped() {
    }

    companion object {
        val TAG = WebLinkFragment_TextureView::class.simpleName
    }
}