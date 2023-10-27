package com.tngdev.weblinkclient.ui.weblink

import android.graphics.Bitmap
import android.graphics.Point
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.abaltatech.mcs.connectionmanager.EConnectionResult
import com.abaltatech.mcs.connectionmanager.PeerDevice
import com.abaltatech.mcs.logger.MCSLogger
import com.abaltatech.weblink.core.WLTypes
import com.abaltatech.weblink.core.commandhandling.BrowserCommand
import com.abaltatech.weblink.core.commandhandling.Command
import com.abaltatech.weblink.core.commandhandling.HideKeyboardCommand
import com.abaltatech.weblink.core.commandhandling.KeyboardCommand
import com.abaltatech.weblink.core.commandhandling.ShowKeyboardCommand
import com.abaltatech.weblinkclient.IClientNotification
import com.abaltatech.weblinkclient.commandhandling.TouchCommand
import com.abaltatech.weblinkclient.framedecoding.FrameDecoder_H264
import com.abaltatech.wlappservices.WLServicesHTTPProxy
import com.tngdev.weblinkclient.App
import com.tngdev.weblinkclient.IPingHandler
import com.tngdev.weblinkclient.IWebLinkActivity
import com.tngdev.weblinkclient.R
import com.tngdev.weblinkclient.databinding.WeblinkFragmentCommonBinding
import com.tngdev.weblinkclient.framedecoding.FrameDecoder_H264_Custom
import com.tngdev.weblinkclient.util.KeyMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.StringBuilder


/**
 * WebLinkFragment is the main screen that displays the video stream received from the WebLink server
 * on the connected phone. This fragment intercepts touches to sends them the WebLink server.
 * <p>
 * This is a base abstract class that has the main functionality. There are different derived
 * classes {@link WebLinkFragment_TextureView} and {@link WebLinkFragment_SurfaceView} that use
 * different types of video view. They override the abstract methods and implement the different
 * behaviour based on the view type.
 */
abstract class WebLinkFragment : Fragment(), View.OnTouchListener, IClientNotification,
    View.OnKeyListener, IPingHandler {

    private val activeAudioChannels: MutableSet<Int> = mutableSetOf()
    private val handler: Handler = Handler(Looper.getMainLooper())
    private var startProxy: Boolean = false
    private var showStats: Boolean = false
    private var showCmdBar: Boolean = false
    private var cmdBarVisible: Boolean = false
    protected val wlClient = App.instance.getWebLinkClientCore()

    protected var mScaleX: Float = 1.0f
    protected var mScaleY: Float = 1.0f

    private var _binding: WeblinkFragmentCommonBinding? = null
    val binding: WeblinkFragmentCommonBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(getLayoutResourceId(), container, false)
        _binding = WeblinkFragmentCommonBinding.bind(view.findViewById(R.id.common_root))

        //receive all touch events on the base view.
        view.setOnTouchListener(this)

        cmdBarVisible = true
        updateUI()
        prepareVideoView(view)

        setLoadingScreenState(true)
        showCmdBar(showCmdBar)
        showStats(showStats)

        //the bar is default shown, do a animated hide after 500ms.
        binding.cmdBar.postDelayed({
            showCmdBar(false)
        }, 500)

        bindEvents()

        return view
    }

    private fun setLoadingScreenState(shown: Boolean) {
        lifecycleScope.launch(Dispatchers.Main) {
            binding.loadingScreen.isVisible = shown
        }
    }

    /**
     * Called by the base class for the derived class to prepare the video view and store it internally.
     *
     * @param fragmentView - the parent fragment view
     */
    abstract fun prepareVideoView(fragmentView: View?)

    /**
     * Returns the resource id for the fragment layout to use.
     *
     * @return
     */
    protected abstract fun getLayoutResourceId(): Int

    override fun onStart() {
        if (wlClient.isConnected) {
            if (wlClient.isVideoDecodingPaused) {
                val res = wlClient.resumeVideoEncoding()
                Log.d(TAG, "resumed video encoding: $res")
            }

            // Display the loading screen
            setLoadingScreenState(true)

            binding.waitIndicator.isVisible = wlClient.isWaitIndicatorShown

            // listen for connection state changes
//            wlClient.registerConnectionListener(this)

            // attach to receive client notification
            App.instance.wlClient.clientListener = this
            App.instance.wlClient.pingHandler = this

            onFragmentStarted()

            //Check if any scaling is needed to sync between the render size and the client.
            val videoViewSize = getVideoViewDimensions()
            val renderSize = wlClient.renderSize
            mScaleX = if (videoViewSize.x > 0) renderSize.x / videoViewSize.x.toFloat() else 1.0f
            mScaleY = if (videoViewSize.y > 0) renderSize.y / videoViewSize.y.toFloat() else 1.0f

            if (showStats) {
                handler.postDelayed(updateStats, 0)
            }

            if (startProxy) {
                if (!WLServicesHTTPProxy.getInstance().startWLServicesClient()) {
                    MCSLogger.log(
                        MCSLogger.eWarning,
                        TAG,
                        "Could not start WL Services Client HTTP Proxy"
                    )
                }
            }

            handler.post {
                binding.waitIndicator.isInvisible = true
            }
        } else {
            onConnectionClosed(null)
        }

        super.onStart()
    }

    /**
     * Returns the dimensions of the video view.
     *
     * @return
     */
    protected abstract fun getVideoViewDimensions(): Point

    /**
     * Called when the fragment has been started.
     */
    protected abstract fun onFragmentStarted()

    override fun onStop() {
        handler.removeCallbacks(updateStats)

        // unregister from client notification
        App.instance.wlClient.clientListener = null

        // force hide keyboard (not automatic on all platforms)
        if (activity is IWebLinkActivity) {
            (activity as IWebLinkActivity).hideKeyboard()
        }

        setLoadingScreenState(false)

        onFragmentStopped()

        super.onStop()
    }

    /**
     * Called when the fragment has been stopped.
     */
    protected abstract fun onFragmentStopped()

    /**
     * Send the back command to the client core.
     *
     * @return
     */
    fun onBackPressed(): Boolean {
        wlClient.sendCommand(BrowserCommand(BrowserCommand.ACT_BACK, null))
        return true
    }

    /**
     * Send the home command to the client core.
     *
     * @return
     */
    fun onHomePressed(): Boolean {
        val ret = wlClient.canGoHome()
        wlClient.sendCommand(BrowserCommand(BrowserCommand.ACT_HOME, null))
        return ret
    }

    private fun showStats(state: Boolean) {
        binding.textStats.isVisible = state
        cmdBarVisible = state
    }

    private fun bindEvents() {
        binding.apply {
            hideButton.setOnClickListener {
                showCmdBar(!cmdBarVisible)
            }

            minimizeButton.setOnClickListener {
                showCmdBar(false)
            }

            closeButton.setOnClickListener {
                wlClient.disconnect()
            }

            backButton.setOnClickListener {
                wlClient.sendCommand(BrowserCommand(BrowserCommand.ACT_BACK, null))
            }

            homeButton.setOnClickListener {
                wlClient.sendCommand(BrowserCommand(BrowserCommand.ACT_HOME, null))
            }

            forwardButton.setOnClickListener {
                wlClient.sendCommand(BrowserCommand(BrowserCommand.ACT_FORWARD, null))
            }

            // When this button is toggled off, this means that the handbrake is оn.
            // When the handbrake is none there are no Driver Distraction restrictions, hence we send
            // UIRESTRICTION_LEVEL_NONE. When the button is toggled on, this means that the handbrake is
            // off and there is a Major Driver Distraction Restriction in effect, hence we send
            // UIRESTRICTION_LEVEL_MAJOR to the Host Application.
            handbrakeButton.setOnClickListener { view ->
                view.isActivated = !view.isActivated
                val level = if (view.isActivated)
                    BrowserCommand.UIRESTRICTION_LEVEL_MAJOR else
                    BrowserCommand.UIRESTRICTION_LEVEL_NONE
                wlClient.sendCommand(BrowserCommand(BrowserCommand.ACT_UI_RESTRICTION_LEVEL, level))
            }

            prev.setOnClickListener {
                if (wlClient.isConnected) {
                    wlClient.sendCommand(
                        KeyboardCommand(
                            WLTypes.VK_MEDIA_PREV_TRACK.toShort(),
                            KeyboardCommand.ACT_KEY_DOWN
                        )
                    )
                    wlClient.sendCommand(
                        KeyboardCommand(
                            WLTypes.VK_MEDIA_PREV_TRACK.toShort(),
                            KeyboardCommand.ACT_KEY_UP
                        )
                    )
                } else {
                    MCSLogger.log(MCSLogger.eWarning, "Client is not connected!")
                }
            }

            next.setOnClickListener {
                if (wlClient.isConnected) {
                    wlClient.sendCommand(
                        KeyboardCommand(
                            WLTypes.VK_MEDIA_NEXT_TRACK.toShort(),
                            KeyboardCommand.ACT_KEY_DOWN
                        )
                    )
                    wlClient.sendCommand(
                        KeyboardCommand(
                            WLTypes.VK_MEDIA_NEXT_TRACK.toShort(),
                            KeyboardCommand.ACT_KEY_UP
                        )
                    )
                } else {
                    MCSLogger.log(MCSLogger.eWarning, "Client is not connected!")
                }
            }
        }
    }

    private fun showCmdBar(state: Boolean) {
        binding.cmdBar.animate()
            .translationY(if (state) 0f else binding.cmdBarButtons.measuredHeight.toFloat())
            .duration = 300

        cmdBarVisible = state
    }

    private fun updateUI() {
        showStats = true
        showCmdBar = false
        startProxy = false

        cmdBarVisible = binding.cmdBar.visibility == View.VISIBLE

        showCmdBar(showCmdBar)
        showStats(showStats)
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (!wlClient.sendCommand(TouchCommand(event, mScaleX, mScaleY))) {
            MCSLogger.log(MCSLogger.eWarning, TAG, "Failed to send touch event!")
        }
        return true
    }

    private val updateStats = object : Runnable {
        override fun run() {
            if (showStats) {
                val frameRate = wlClient.frameRate
                val dataRate = wlClient.dataRate / 1024

                var frameCount: Long = -1
                var keyFrameCount: Long = -1

                val decoder = wlClient.frameDecoder
                if (decoder != null) {
                    if (decoder is FrameDecoder_H264) {
                        frameCount = decoder.frameInputCount
                        keyFrameCount = decoder.keyFrameInputCount
                    }
                    if (decoder is FrameDecoder_H264_Custom) {
                        frameCount = decoder.frameInputCount
                        keyFrameCount = decoder.keyFrameInputCount
                    }
                }

                var count = 0
                val sb = StringBuilder()
                if (frameRate != -1) {
                    count++
                    sb.append(String.format("Frame rate ${frameRate}fps"))
                }
                if (dataRate != -1) {
                    if (count > 0) {
                        sb.append("\n")
                        count--
                    }
                    sb.append(String.format("Data rate: ${dataRate}KB/s"))
                    count++
                }
                if (frameCount != -1L) {
                    if (count > 0) {
                        sb.append("\n")
                        count--
                    }
                    sb.append(String.format("Frame #: $frameCount"))
                    count++
                }
                if (keyFrameCount != -1L) {
                    if (count > 0) {
                        sb.append("\n")
                        count--
                    }
                    sb.append(String.format("KeyFrame #: $keyFrameCount"))
                    count++
                }
                if (activeAudioChannels.isNotEmpty()) {
                    sb.append("\n")
                    for (activeAudioChannel in activeAudioChannels) {
                        sb.append(String.format("Active Audio channel #: ${activeAudioChannel}\n"));
                    }
                }

                binding.textStats.text = sb.toString()
                handler.postDelayed(this, 1000)
            }
        }
    }


    // region IClientNotification
    override fun onServerListUpdated(p0: Array<out IClientNotification.ServerInfo>?) {
        // already connected
    }

    override fun onConnectionEstablished(p0: PeerDevice?) {
        MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "onConnectionEstablished()")
        // do nothing !
        if (startProxy) {
            if (!WLServicesHTTPProxy.getInstance().startWLServicesClient()) {
                MCSLogger.log(
                    MCSLogger.eWarning,
                    TAG,
                    "Could not start WL Services Client HTTP Proxy"
                )
            }
        }

        // No restrictions by default. Here a check for the handbrake should be made like the
        // OnClickListener of the handbrake_button.
        wlClient.sendCommand(
            BrowserCommand(
                BrowserCommand.ACT_UI_RESTRICTION_LEVEL,
                BrowserCommand.UIRESTRICTION_LEVEL_NONE
            )
        )
    }

    override fun onConnectionFailed(peerDevice: PeerDevice?, result: EConnectionResult?) {
        MCSLogger.log(
            MCSLogger.eWarning,
            TAG,
            "onConnectionFailed, device: %s, reason: %s",
            peerDevice?.name,
            result?.name
        );
    }

    override fun onConnectionClosed(p0: PeerDevice?) {
        WLServicesHTTPProxy.getInstance().stopWLServicesClient()

        // the connection is gone, close this fragment and return to non-connected screen.
        lifecycleScope.launch(Dispatchers.Main) {
            if (activity is IWebLinkActivity) {
                (activity as IWebLinkActivity).stopWebLink()
            }
        }

        activeAudioChannels.clear()

        lifecycleScope.launch(Dispatchers.Main) {
            binding.waitIndicator.isInvisible = true
        }
    }

    /**
     * Called when the current application has changed
     *
     * @param appID the current application ID
     */
    override fun onApplicationChanged(appID: Int) {
        MCSLogger.log(TAG, "onApplicationChanged: appID $appID")
    }

    /**
     * Called when new frame has been decoded and rendered to the frame buffer
     */
    override fun onFrameRendered() {
        // Hide the loading screen
        setLoadingScreenState(false)
    }

    /**
     * Called to check if the client is able to receive new frame
     * <p>
     * return boolean - true to receive the frame, false to ignore it
     */
    override fun canProcessFrame(): Boolean {
        return true
    }

    /**
     * Called when a "Show Keyboard" command has been received
     *
     * @param type the requested keyboard type
     */
    override fun onShowKeyboard(type: Short) {
        lifecycleScope.launch(Dispatchers.Main) {
            if (activity is IWebLinkActivity) {
                (activity as IWebLinkActivity).showKeyboard(this@WebLinkFragment)
                wlClient.sendCommand(ShowKeyboardCommand(type))
            }
        }
    }

    /**
     * Called when a "Hide Keyboard" command has been received
     */
    override fun onHideKeyboard() {
        lifecycleScope.launch(Dispatchers.Main) {
            if (activity is IWebLinkActivity) {
                (activity as IWebLinkActivity).hideKeyboard()

                wlClient.sendCommand(HideKeyboardCommand())
            }

        }
    }

    /**
     * Called when a "Wait Indicator" command has been received
     *
     * @param showWaitIndicator true to show the wait indicator, false to hide it
     */
    override fun onWaitIndicator(showWaitIndicator: Boolean) {
        // do not show the spinner
        lifecycleScope.launch(Dispatchers.Main) {
            binding.waitIndicator.isVisible = showWaitIndicator
        }
    }

    /**
     * Called to update the image for the application with the given ID
     *
     * @param appID the application ID
     * @param image the application Image
     */
    override fun onAppImageChanged(appID: Int, image: Bitmap?) {
        Log.v(TAG, "onAppImageChanged()")
    }

    /**
     * Called when the server connection has been lost
     */
    override fun onConnectionLost() {
        MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "onConnectionLost()")
    }

    /**
     * Called when the server connection has been resumed
     */
    override fun onConnectionResumed() {
    }

    /**
     * See {@link IClientNotification#onCommandReceived(Command)}
     *
     * @param command the command received
     * @return
     */
    override fun onCommandReceived(p0: Command?): Boolean {
        return true
    }

    override fun onAudioChannelStarted(channelID: Int) {
        activeAudioChannels.add(channelID)
    }

    override fun onAudioChannelStopped(channelID: Int) {
        activeAudioChannels.remove(channelID)
    }
    // endregion IClientNotification

    // region View.OnKeyListener
    /**
     * Handle key events sent to this View.
     */
    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
        when (event?.action) {
            KeyEvent.ACTION_UP -> {
                return onKeyUp(keyCode, event)
            }

            KeyEvent.ACTION_DOWN -> {
                return onKeyDown(keyCode, event)
            }

            KeyEvent.ACTION_MULTIPLE -> {
                return onKeyMultiple(keyCode, event)
            }
        }
        return false
    }

    /**
     * Process the android Key multiple event.
     *
     * @param keyCode
     * @param event
     * @return
     */
    private fun onKeyMultiple(keyCode: Int, event: KeyEvent): Boolean {
        var result = false
        if (keyCode == 0) {
            val characters = event.characters
            val len = characters.codePointCount(0, characters.length)
            //NOTE: for now only send 1 character keys.  There are some generic IME events
            //that will emit the dummy buffer (del fix) characters
            if (len == 1) {
                val code = characters.codePointAt(0)
                Log.d(TAG, "onKeyMultiple sending: $code")
                if (code != 0) {
                    wlClient.sendCommand(KeyboardCommand(code as Short, KeyboardCommand.ACT_KEY_DOWN))
                }
            } else {
                Log.d(TAG, "onKeyMultiple not handling event len=$len")
            }
        }
        result = true

        return result
    }

    /**
     * Handle the key events sent to this view.
     *
     * @param keyCode
     * @param event
     * @return
     */
    private fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_BACK) {
            wlClient.sendCommand(BrowserCommand(BrowserCommand.ACT_BACK, null))
            true
        } else {
            // Pass the keys to weblink client core.
            // First pass through extraction to get the underlying key code.
            val key = extractVirtualKeyCode(event)
            if (key != 0) {
                wlClient.sendCommand(KeyboardCommand(key as Short, KeyboardCommand.ACT_KEY_DOWN))
            }
            true
        }
    }

    private fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        var result = false
        if (keyCode != KeyEvent.KEYCODE_BACK && keyCode != KeyEvent.KEYCODE_MENU) {
            val key = extractVirtualKeyCode(event)
            if (key != 0) {
                wlClient.sendCommand(KeyboardCommand(key as Short, KeyboardCommand.ACT_KEY_UP))
            }
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                if (activity is IWebLinkActivity) {
                    (activity as IWebLinkActivity).hideKeyboard()
                }
            }
            result = true
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (event.isLongPress) {
                wlClient.sendCommand(BrowserCommand(BrowserCommand.ACT_HOME, null))
            }
            result = true
        } else {
            val key = extractVirtualKeyCode(event)
            if (key != 0) {
                wlClient.sendCommand(KeyboardCommand(key as Short, KeyboardCommand.ACT_KEY_UP))
            }
            result = true
        }
        return result
    }

    private fun extractVirtualKeyCode(event: KeyEvent): Int {
        var keyCode = event.keyCode

        keyCode = if (event.isShiftPressed)
            KeyMap.KEY_MAPPINGS_SHIFT.get(keyCode)
        else
            KeyMap.KEY_MAPPINGS_NORMAL.get(keyCode)

        // fall back to the unicode value
        if (keyCode == 0) {
            keyCode = event.unicodeChar
        }

        return keyCode
    }
    // endregion

    // region IPingHandler
    /**
     * Example handling the ping timeout.
     * <p>
     * The client disconnects.
     */
    override fun onPingResponseTimeout() {
        wlClient.disconnect()
    }

    /**
     * See {@link IPingHandler#onPingResponseReceived(boolean)}
     *
     * @param isSenderInactive the host app's reported activity status.
     */
    override fun onPingResponseReceived(isSenderInactive: Boolean) {
    }
    // endregion IPingHandler

    companion object {
        val TAG = WebLinkFragment::class.simpleName
    }
}