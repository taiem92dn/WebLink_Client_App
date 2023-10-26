package com.tngdev.weblinkclient.ui.autoconnect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.abaltatech.mcs.connectionmanager.EConnectionResult
import com.abaltatech.mcs.connectionmanager.PeerDevice
import com.abaltatech.mcs.logger.MCSLogger
import com.abaltatech.mcs.usbhost.android.ConnectionMethodAOA
import com.abaltatech.weblinkclient.IClientNotification
import com.tngdev.weblinkclient.App
import com.tngdev.weblinkclient.IConnectionStatusNotification
import com.tngdev.weblinkclient.IServerUpdateNotification
import com.tngdev.weblinkclient.IWebLinkActivity
import com.tngdev.weblinkclient.databinding.FragmentAutoConnectBinding
import java.util.Queue
import java.util.concurrent.ArrayBlockingQueue

private const val MAX_SERVERS_QUEUE_CAPACITY = 10

private const val DEVICE_SCANNING_INTERVAL_MS = 3000L

class AutoConnectFragment : Fragment(), IConnectionStatusNotification, IServerUpdateNotification {

    // Thread that will periodically scan for devices.
    private var deviceScanThread: Thread? = null

    // List of servers that have been discovered during the scanning process.
    private val serversQueue: Queue<IClientNotification.ServerInfo> =
        ArrayBlockingQueue<IClientNotification.ServerInfo>(MAX_SERVERS_QUEUE_CAPACITY)

    private var connectionThread: ConnectionThread? = null

    private var _binding: FragmentAutoConnectBinding? = null
    private val binding: FragmentAutoConnectBinding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        connectionThread = ConnectionThread()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAutoConnectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        MCSLogger.log(MCSLogger.eDebug, TAG, "Start")

        // Start scanning for devices
        startListening()
    }

    /**
     * Starts listening for available WebLink Servers.
     */
    private fun startListening() {
        MCSLogger.log(MCSLogger.eDebug, TAG, "Start scanning for devices")

        val wlClient = App.instance.wlClient
        wlClient.registerConnectionListener(this)
        wlClient.registerServerUpdateListener(this)

        if (deviceScanThread != null && deviceScanThread?.isAlive == true) {
            deviceScanThread?.interrupt() // stop old thread
        }

        deviceScanThread = object : Thread() {
            override fun run() {
                try {
                    while (!isInterrupted) {
                        MCSLogger.log(MCSLogger.eDebug, TAG, "Reset server list")
                        App.instance.getWebLinkClientCore().resetServerList()
                        MCSLogger.log(MCSLogger.eDebug, TAG, "Scan devices")
                        App.instance.getWebLinkClientCore().scanDeviceList()
                        Thread.sleep(DEVICE_SCANNING_INTERVAL_MS)
                    }
                } catch (ex: InterruptedException) {
                    //do nothing
                }
            }
        }
        deviceScanThread?.start()
    }

    /**
     * Stops listening for available WebLink Servers.
     */
    private fun stopListening() {
        MCSLogger.log(MCSLogger.eDebug, TAG, "Stop scanning for devices")

        // Unregister from notifications
        val wlClient = App.instance.wlClient
        wlClient.unregisterServerUpdateListener(this)
        wlClient.unregisterConnectionListener(this)

        // Stop the device scanning thread
        if (deviceScanThread != null) {
            deviceScanThread?.interrupt()
            deviceScanThread = null
        }
    }


    override fun onStop() {
        MCSLogger.log(MCSLogger.eDebug, TAG, "Stop")

        // Stop listening for devices
        stopListening()

        super.onStop()
    }

    /**
     * Requests that the WebLink Activity displays the WebLink Fragment
     */
    private fun startClient() {
        if (activity is IWebLinkActivity) {
            (activity as IWebLinkActivity).startWebLink()
        }
    }

    // region IConnectionStatusNotification
    /**
     * Called when a connection with the WebLink Host has been established.
     */
    override fun onConnectionEstablished(peerDevice: PeerDevice) {
        MCSLogger.log(TAG, "Connection Established")
        val activity = getActivity()
        activity?.runOnUiThread {
            startClient()
        }

        if (connectionThread?.isAlive == true) {
            connectionThread?.connectionEstablished(peerDevice)
        }
    }

    /**
     * Received when connection request failed.
     */
    override fun onConnectionFailed(peerDevice: PeerDevice, result: EConnectionResult) {
        MCSLogger.log(MCSLogger.eWarning, TAG, "Failed to establish a connection, reason $result")
        if (connectionThread?.isAlive == true) {
            connectionThread?.connectionFailed(peerDevice)
        }
    }

    /**
     * Called when the connection with the WebLink Host has been closed.
     */
    override fun onConnectionClosed(peerDevice: PeerDevice) {
        MCSLogger.log(MCSLogger.eWarning, TAG, "Connection closed!")
        if (connectionThread?.isAlive == true) {
            connectionThread?.connectionClosed(peerDevice)
        }
    }
    // endregion IConnectionStatusNotification

    // region IServerUpdateNotification
    /**
     * Called when the list with available WebLink servers has changed.
     *
     * @param servers the list of active devices
     */
    override fun onServerListUpdated(servers: Array<IClientNotification.ServerInfo>) {
        MCSLogger.log(MCSLogger.eDebug, TAG, "On server list updated")

        synchronized(this) {
            for (serverInfo in servers) {
                if (!serversQueue.contains(serverInfo)) {
                    try {
                        serversQueue.add(serverInfo)
                    } catch (e: IllegalStateException) {
                        MCSLogger.log(MCSLogger.eWarning, TAG, "Fail to add server, queue is full")
                    }
                }
            }
            if (!App.instance.getWebLinkClientCore().isConnected && connectionThread?.isAlive == false) {
                connectionThread?.start()
            }
        }

        for (info in servers) {
            MCSLogger.log(
                MCSLogger.eDebug, TAG, "Device: " +
                        "${info.m_peerDevice.name}, Address: ${info.m_peerDevice.address}"
            )
        }
    }
    // endregion IServerUpdateNotification

    inner class ConnectionThread : Thread() {
        var deviceConnectionRequested: PeerDevice? = null
        var deviceConnected: PeerDevice? = null

        override fun run() {
            MCSLogger.log(TAG, "ConnectionThread started!")

            autoConnect(ConnectionMethodAOA.ID)

            MCSLogger.log(TAG, "ConnectionThread end!")
        }

        /**
         * Notify ConnectionThread for connection fails
         *
         * @param peerDevice PeerDevice which fails to connect
         */
        fun connectionFailed(peerDevice: PeerDevice) {
            synchronized(this@AutoConnectFragment) {
                if (peerDevice == deviceConnectionRequested) {
                    deviceConnectionRequested = null
                } else {
                    MCSLogger.log(
                        MCSLogger.eWarning, TAG, "PeerDevice mismatch! " +
                                "Connection Failed with different than requested device!"
                    )
                }
            }
        }

        /**
         * Notify ConnectionThread for connection established
         *
         * @param peerDevice PeerDevice which connects successfully
         */
        fun connectionEstablished(peerDevice: PeerDevice) {
            synchronized(this@AutoConnectFragment) {
                if (peerDevice === deviceConnectionRequested) {
                    deviceConnected = peerDevice
                } else {
                    MCSLogger.log(
                        MCSLogger.eWarning, TAG, "PeerDevice mismatch! " +
                                "ConnectionEstablished with different than requested device!"
                    )
                }
            }
        }

        /**
         * Notify ConnectionThread for connection closed
         *
         * @param peerDevice PeerDevice which connection was closed
         */
        fun connectionClosed(peerDevice: PeerDevice) {
            synchronized(this@AutoConnectFragment) {
                if (peerDevice == deviceConnected) {
                    deviceConnected = null
                } else {
                    MCSLogger.log(
                        MCSLogger.eWarning, TAG, "PeerDevice mismatch!" +
                                "Connection closed with different than requested device!"
                    )
                }
            }
        }

        /**
         * Attempt to automatically connect to those devices in the available servers list that
         * are connected using corresponding connection method.
         *
         * @param connectionMethodID The ID of the connection method which should be used
         */
        private fun autoConnect(connectionMethodId: String) {
            while (!isInterrupted) {
                synchronized(this@AutoConnectFragment) {
                    if (deviceConnectionRequested != null) {

                        // Confirm that connection with requested device is successful and exit
                        if (deviceConnectionRequested == deviceConnected) {
                            deviceConnectionRequested = null
                            return@autoConnect
                        }

                        return@synchronized
                    }

                    // Get next element in the queue if it is available.
                    var info: IClientNotification.ServerInfo?
                    synchronized(this@AutoConnectFragment) {
                        info = serversQueue.poll()
                    }

                    if (info?.m_peerDevice?.connectionMethodID == connectionMethodId) {
                        val activity = getActivity()
                        if (activity != null && activity is IWebLinkActivity) {
                            synchronized(this@AutoConnectFragment) {
                                deviceConnectionRequested = info?.m_peerDevice
                            }

                            // Initiate connection process with the corresponding PeerDevice.
                            val webLinkActivity = activity as IWebLinkActivity
                            if (!webLinkActivity.connectRequest(info!!.m_peerDevice)) {
                                synchronized(this@AutoConnectFragment) {
                                    deviceConnectionRequested = null
                                }
                                MCSLogger.log(
                                    MCSLogger.eWarning,
                                    TAG,
                                    "Connection request failed, device Name: ${info?.m_peerDevice?.name}"
                                )

                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        val TAG = AutoConnectFragment::class.simpleName
    }
}