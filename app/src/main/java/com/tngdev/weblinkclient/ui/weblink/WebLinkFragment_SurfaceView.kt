package com.tngdev.weblinkclient.ui.weblink

import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import com.abaltatech.weblinkclient.WLClientDisplay
import com.tngdev.weblinkclient.R

class WebLinkFragment_SurfaceView: WebLinkFragment() {

    private var videoView: SurfaceView? = null

    lateinit var defaultClientDisplay: WLClientDisplay


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val rootView = inflater.inflate(R.layout.fragment_weblink_surfaceview, container, false)


        return rootView
    }

    override fun prepareVideoView(fragmentView: View?) {
        // The layout consists of a FrameLayout as the root and a single SurfaceView with an
        // id of `video_view` as the child of the root FrameLayout.
        val surfaceView = fragmentView?.findViewById<SurfaceView>(R.id.video_view)

        videoView = surfaceView
        surfaceView ?: return
        val holder = surfaceView.holder
        holder.addCallback(object: SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.d(TAG, "surfaceCreated!")
                val renderSize = wlClient.renderSize
                mScaleX = if (surfaceView.width > 0) renderSize.x / surfaceView.width.toFloat() else 1.0f
                mScaleY = if (surfaceView.height > 0) renderSize.y / surfaceView.height.toFloat() else 1.0f
                defaultClientDisplay.onEncodeSurfaceReady(holder.surface)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                val renderSize = wlClient.renderSize
                mScaleX = if (surfaceView.width > 0) renderSize.x / surfaceView.width.toFloat() else 1.0f
                mScaleY = if (surfaceView.height > 0) renderSize.y / surfaceView.height.toFloat() else 1.0f
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                Log.d(TAG, "surfaceDestroyed!")
                //Perform the pausing when the texture is destroyed
                //this represents when the texture is no longer valid
                //to the system so we will have to recreate it next start() cycle.
                defaultClientDisplay.onEncodeSurfaceDestroyed()
            }

        })
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.fragment_weblink_surfaceview
    }

    override fun getVideoViewDimensions(): Point {
        return Point(
            videoView?.width ?: 0,
            videoView?.height ?: 0
        )
    }

    override fun onFragmentStarted() {
    }

    override fun onFragmentStopped() {
    }

    companion object {
        val TAG = WebLinkFragment_SurfaceView::class.simpleName
    }
}