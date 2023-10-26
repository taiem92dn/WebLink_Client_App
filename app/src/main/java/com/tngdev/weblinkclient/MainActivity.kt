package com.tngdev.weblinkclient

import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.Point
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.abaltatech.mcs.connectionmanager.PeerDevice
import com.abaltatech.mcs.logger.MCSLogger
import com.abaltatech.weblink.core.WLClientFeatures
import com.abaltatech.weblink.core.WLTypes
import com.abaltatech.weblink.core.clientactions.EClientResponse
import com.abaltatech.weblinkclient.IClientNotification
import com.abaltatech.weblinkclient.WLClientDisplay
import com.abaltatech.weblinkclient.clientactions.EAppLaunchStatus
import com.abaltatech.weblinkclient.clientactions.ICARHLaunchApp
import com.abaltatech.weblinkclient.clientactions.ICARHWebLinkClientState
import com.abaltatech.weblinkclient.clientactions.LaunchAppRequest
import com.abaltatech.weblinkclient.framedecoding.FrameDecoderFactory
import com.abaltatech.weblinkclient.framedecoding.IFrameDecoder
import com.tngdev.weblinkclient.databinding.ActivityMainBinding
import com.tngdev.weblinkclient.framedecoding.FrameDecoder_H264_Custom
import com.tngdev.weblinkclient.ui.autoconnect.AutoConnectFragment
import com.tngdev.weblinkclient.ui.weblink.WebLinkFragment
import com.tngdev.weblinkclient.ui.weblink.WebLinkFragment_SurfaceView
import com.tngdev.weblinkclient.ui.weblink.WebLinkFragment_TextureView
import com.tngdev.weblinkclient.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : FragmentActivity(), IWebLinkActivity {

    private  var autoConnectFragment: AutoConnectFragment? = null
    private var inputListener: View.OnKeyListener? = null
    private var webLinkFragment: WebLinkFragment? = null
    private var mClientDisplay: WLClientDisplay? = null

    private var _binding: ActivityMainBinding? = null
    private val binding: ActivityMainBinding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // set activity to full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val wlClient = App.instance.getWebLinkClientCore()

        initWLClientDisplay()

        wlClient.clientActionHandlerManager.registerRequestHandler(object : ICARHWebLinkClientState {
            override fun onWebLinkClientExitRequested(): Boolean {
                // The client is free to decide what to do when an exit is requested. In this example
                // the connection is terminated.
                MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "onWebLinkClientExitRequested")
                if (wlClient.isConnected) {
                    wlClient.disconnect()
                    return true
                }

                return false
            }
        })

        wlClient.clientActionHandlerManager.registerRequestHandler(object: ICARHLaunchApp {
            override fun processAppLaunchRequest(request: LaunchAppRequest?): Boolean {
                MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "processAppLaunchRequest: ${request?.applicationID}")

                // Check if we can launch the application
                if (canLaunchApp(request?.applicationID) == EAppLaunchStatus.ALS_CAN_LAUNCH) {
                    // Launch the application
                    request ?: return false
                    val launchIntent = packageManager.getLaunchIntentForPackage(request!!.applicationID)
                    startActivity(launchIntent)

                    lifecycleScope.launch(Dispatchers.Main) {
                        // Periodically check if the application is running.
                        for (i in 0 until 10) {
                            delay(1000)

                            if (Utils.isAppRunning(this@MainActivity, request.applicationID)) {
                                // Return a success response
                                request.sendResponse(EClientResponse.CR_SUCCESS, "")
                                return@launch
                            }
                        }
                    }

                    // Error response. The launch timed out.
                    request.sendResponse(EClientResponse.CR_TIMEOUT, "The application failed to start within 5 seconds!")
                } else {
                    request?.sendResponse(EClientResponse.CR_FAILED, "Application not installed or launching it is not supported!")
                }

                return true
            }

            override fun canLaunchApp(applicationID: String?): EAppLaunchStatus {
                // In this example the client will check if the specified application is installed.
                MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "canLaunchApp: $applicationID")

                applicationID ?: return EAppLaunchStatus.ALS_UNSUPPORTED_ACTION

                // Obtain information about the package
                val pm = packageManager
                try {
                    val info = pm.getPackageInfo(applicationID, 0)
                    val appInfo = pm.getApplicationInfo(applicationID, 0)
                    // If the package exists, get the application name and respond with a can launch
                    if (info != null) {
                        val applicationName = pm.getApplicationLabel(appInfo)
                        MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "canLaunchApp: $applicationName v(${info.versionName}) can be launched.")
                        return EAppLaunchStatus.ALS_CAN_LAUNCH
                    }
                } catch (e: NameNotFoundException) {
                    return EAppLaunchStatus.ALS_UNSUPPORTED_ACTION
                }

                return EAppLaunchStatus.ALS_UNSUPPORTED_ACTION
            }

        })

        showConnect(true)
    }

    private fun initWLClientDisplay() {
        val wlClient = App.instance.getWebLinkClientCore()

        // Terminate any previous session
        wlClient.terminate()

        // Get size of the screen. This is the client size
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val clientSize = Point()
        display.getSize(clientSize)

        // Sets the resolution of the video, generated by the WebLink Host.
        // This is also the resolution that the decoder on the client side
        // will use.
        val encodeWidth = 800
        val encodeHeight = 480

        // Sets the resolution at which WebLink applications are rendered before
        // being encoded into video.
        val renderWidth = encodeWidth
        val renderHeight = encodeHeight

        // Sets the client dimensions
        val clientWidth = encodeWidth
        val clientHeight = encodeHeight

        // Set the Client Display's DPI
        val metrics = resources.displayMetrics
        val xdpi = metrics.xdpi.toInt()
        val ydpi = metrics.ydpi.toInt()

        val clientFeatures = WLClientFeatures.SUPPORTS_CLIENT_ACTIONS
        val clientFeaturesString = String.format(Locale.US, "xdpi=$xdpi|ydpi=$ydpi")

        if (isDecoderTargetSurfaceView()) {
            // We are using a custom display with an external encoder surface
            // Register it with the client before it is initialized
            wlClient.registerClientDisplay(WLClientDisplay(null /* Has external encoder surface*/))
        } else {
            // We are using a custom display with an internal encoder surface
            wlClient.registerClientDisplay(WLClientDisplay(/* Uses internal encoder texture surface*/));
        }

//        wlClient.setEncoderParams(getEncoderParams())
//        wlClient.setMaximumFrameRate()
//        wlClient.enableAutoFPSManagement()

        if (!wlClient.init(
                renderWidth, renderHeight,
                encodeWidth, encodeHeight,
                clientWidth, clientHeight,
                clientFeatures, clientFeaturesString
            )
        ) {
            Toast.makeText(
                this,
                "Could not initialize WLClient",
                Toast.LENGTH_LONG
            ).show()
        }

        mClientDisplay = wlClient.defaultDisplay


        var decoderClass: Class<out IFrameDecoder?>? = null
        val decoderType = WLTypes.FRAME_ENCODING_H264

        MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "Chosen decoder: Custom Hardware H264")
        decoderClass = FrameDecoder_H264_Custom::class.java

        //unregister all other decoders, re-register only the selected version.
        FrameDecoderFactory.instance().unregisterDecoder(WLTypes.FRAME_ENCODING_H264)
        FrameDecoderFactory.instance().unregisterDecoder(WLTypes.FRAME_ENCODING_I420)
        FrameDecoderFactory.instance().unregisterDecoder(WLTypes.FRAME_ENCODING_YUV)
        FrameDecoderFactory.instance().registerDecoder(decoderType, decoderClass)

        if (mClientDisplay != null) {
            val decoderMask = FrameDecoderFactory.instance().registeredDecodersMask
            mClientDisplay?.supportedDecodersMask = decoderMask
        }
    }

    private fun isDecoderTargetSurfaceView(): Boolean {
        return false
    }


    /**
     * Tries to connect to the specified Peer Device.
     *
     * The result of this operation is delivered through the {@link IClientNotification}
     * interface.
     *
     * This method returns true if the connect request was accepted by the WebLink Client SDk
     * and false otherwise.
     *
     * @see IClientNotification#onConnectionEstablished(PeerDevice)
     * @see IClientNotification#onConnectionFailed(PeerDevice, EConnectionResult)
     *
     * @param device Peer device
     *
     * @return true if request was accepted, false otherwise
     */
    override fun connectRequest(device: PeerDevice): Boolean {
        if (device == null) {
            MCSLogger.log(MCSLogger.eWarning, TAG, "Fail to connect to device, PeerDevice is null")
            return false
        }

        val wlClient = App.instance.getWebLinkClientCore()
        MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "Connecting to device ${device}")
        return wlClient.connect(device, IClientNotification.EProtocolType.ePT_WL, -1)
    }

    /**
     * Starts the WebLink fragment.
     *
     * The first time this method is called, it will create the WebLink Fragment.
     *
     * This method attaches the WebLink fragment to the WebLink Container layout.
     */
    override fun startWebLink() {
        // Instantiate the correct fragment, depending on whether a SurfaceView or TextureView
        // is used to render the decoded WebLink Projection.
        if (webLinkFragment == null) {
            if (isDecoderTargetSurfaceView()) {
                webLinkFragment = WebLinkFragment_SurfaceView()
                (webLinkFragment as WebLinkFragment_SurfaceView).defaultClientDisplay =
                    mClientDisplay!!
            } else {
                webLinkFragment = WebLinkFragment_TextureView()
            }

            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.replace(R.id.weblink_container, webLinkFragment!!)
            fragmentTransaction.commit()
        }

        showWebLink()
    }

    override fun hideKeyboard() {
        inputListener = null
        val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        im.hideSoftInputFromWindow(binding.fragmentContainer.windowToken, 0)
    }

    /**
     * Detaches the WebLink Fragment and destroys it.
     */
    override fun stopWebLink() {
        hideWebLink()

        if (webLinkFragment != null) {
            val ft = supportFragmentManager.beginTransaction()
            ft.remove(webLinkFragment!!)
            ft.commit()
            webLinkFragment = null
        }
    }


    /**
     * Show soft input for the dummy edit text.  While shown, the events will be forwarded
     * to the provided Key listener.
     * @param listener Key listener for the duration of this keyboard.
     */
    override fun showKeyboard(listener: View.OnKeyListener) {
        inputListener = listener
        // Show soft input
        val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val dummyView = binding.fragmentContainer
        dummyView.requestFocus()
        dummyView.requestFocusFromTouch()
        im.showSoftInput(dummyView, InputMethodManager.SHOW_FORCED)

    }

    /**
     * Hides the WebLink Projection fragment and makes the non-projection fragment
     * container visible.
     */
    private fun hideWebLink() {
        binding.weblinkContainer.isInvisible = true
        showConnect(false)
    }

    /**
     * Shows the WebLink Projection fragment and makes the non-projection fragment
     * container invisible.
     */
    private fun showWebLink() {
        // Hide the fragment container
        binding.fragmentContainer.visibility = View.GONE

        // Remove the fragment
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment != null) {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.remove(fragment)
            fragmentTransaction.commit()
        }

        binding.weblinkContainer.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        App.instance.getWebLinkClientCore().terminate()

        super.onDestroy()
    }

    /**
     * Handles the system back button
     */
    override fun onBackPressed() {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment != null) {
            if (webLinkFragment == null) {
                showConnect(false)
            } else {
                showWebLink()
            }
        } else {
            webLinkFragment?.onBackPressed()
        }
    }

    /**
     * Transition to the connect UI fragment, depending on what is the current app settings.
     * @param isFirstShow for non-frst showing, allow the fragments to retain their enable values.
     */
    private fun showConnect(isFirstShow: Boolean) {
        binding.fragmentContainer.isVisible = true

        if (isFirstShow) {
            autoConnectFragment = AutoConnectFragment()
        }

        val ft = supportFragmentManager.beginTransaction()
        if (autoConnectFragment != null) {
            ft.replace(R.id.fragment_container, autoConnectFragment!!)
        }
        else {
            MCSLogger.log(MCSLogger.eDebug, "This should not happen!")
        }

        ft.commit()
    }

    companion object {
        val TAG = MainActivity::class.simpleName
    }

}