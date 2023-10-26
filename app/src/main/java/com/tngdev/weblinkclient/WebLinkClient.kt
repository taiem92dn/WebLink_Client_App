package com.tngdev.weblinkclient

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import com.abaltatech.mcs.connectionmanager.EConnectionResult
import com.abaltatech.mcs.connectionmanager.PeerDevice
import com.abaltatech.weblink.core.authentication.DeviceIdentity
import com.abaltatech.weblink.core.commandhandling.Command
import com.abaltatech.weblinkclient.IClientNotification
import com.abaltatech.weblinkclient.WebLinkClientCore
import com.tngdev.weblinkclient.util.Utils

class WebLinkClient(private val context: Context) : IClientNotification {

    var pingHandler: IPingHandler? = null
    private val serverListeners: MutableList<IServerUpdateNotification> = mutableListOf()
    private val connListeners: MutableList<IConnectionStatusNotification> = mutableListOf()

    /**
     * Set the client notification listener.
     *  the listener, or null to remove.
     */
    var clientListener: IClientNotification? = null

    private var mDeviceIdentity: DeviceIdentity

    private var mClientCore: WebLinkClientCore

    init {

        mDeviceIdentity = DeviceIdentity().apply {
            systemId = Build.MANUFACTURER + Build.MODEL + Build.SERIAL
            displayNameEn = Build.DISPLAY
            manufacturer = Build.MANUFACTURER
            model = Build.MODEL
            countryCodes = "vn"
            serialNumber = Build.SERIAL

            displayNameMultiLanguage = "{\"en\":\" ${Build.DISPLAY} \"}"

            application = "WebLink Android Client"
            applicationVendor = "com.tngdev"
            appVersion = Utils.getVersionName(context)
            os = "Android"
            osVersion = Build.VERSION.SDK_INT.toString()
        }

       mClientCore = WebLinkClientCore(context, this, mDeviceIdentity, null)
    }

    fun getWebLinkClientCore() = mClientCore

    override fun onServerListUpdated(servers: Array<IClientNotification.ServerInfo>?) {
        clientListener?.onServerListUpdated(servers)

        servers ?: return
        synchronized(serverListeners) {
            for (listener in serverListeners) {
                listener.onServerListUpdated(servers)
            }
        }

    }

    override fun onConnectionEstablished(peerDevice: PeerDevice?) {
        clientListener?.onConnectionEstablished(peerDevice)

        peerDevice ?: return
        synchronized(connListeners) {
            for (listener in connListeners) {
                listener.onConnectionEstablished(peerDevice)
            }
        }
    }

    override fun onConnectionFailed(peerDevice: PeerDevice?, result: EConnectionResult?) {
        clientListener?.onConnectionFailed(peerDevice, result)

        peerDevice ?: return
        result ?: return
        synchronized(connListeners) {
            for (listener in connListeners) {
                listener.onConnectionFailed(peerDevice, result)
            }
        }

    }

    override fun onConnectionClosed(peerDevice: PeerDevice?) {
        clientListener?.onConnectionClosed(peerDevice)

        peerDevice ?: return
        synchronized(connListeners) {
            for (listener in connListeners) {
                listener.onConnectionClosed(peerDevice)
            }
        }
    }

    override fun onApplicationChanged(appId: Int) {
        clientListener?.onApplicationChanged(appId)
    }

    override fun onFrameRendered() {
        clientListener?.onFrameRendered()
    }

    override fun canProcessFrame(): Boolean {
        return clientListener?.canProcessFrame() ?: true
    }

    override fun onShowKeyboard(type: Short) {
        clientListener?.onShowKeyboard(type)
    }

    override fun onHideKeyboard() {
        clientListener?.onHideKeyboard()
    }

    override fun onWaitIndicator(showWaitIndicator: Boolean) {
        clientListener?.onWaitIndicator(showWaitIndicator)
    }

    override fun onAppImageChanged(appId: Int, image: Bitmap?) {
        clientListener?.onAppImageChanged(appId, image)
    }

    override fun onConnectionLost() {
        clientListener?.onConnectionLost()
    }

    override fun onConnectionResumed() {
        clientListener?.onConnectionResumed()
    }

    override fun onCommandReceived(command: Command?): Boolean {
       return true
    }

    override fun onAudioChannelStarted(channelID: Int) {
        clientListener?.onAudioChannelStarted(channelID)
    }

    override fun onAudioChannelStopped(channelID: Int) {
        clientListener?.onAudioChannelStopped(channelID)
    }

    fun registerConnectionListener(listener: IConnectionStatusNotification) {
        synchronized(connListeners) {
            if (!connListeners.contains(listener)) {
                connListeners.add(listener)
            }
        }
    }

    fun registerServerUpdateListener(listener: IServerUpdateNotification) {
        synchronized(serverListeners) {
            if (!serverListeners.contains(listener)) {
                serverListeners.add(listener)
            }
        }
    }

    fun unregisterServerUpdateListener(listener: IServerUpdateNotification) {
        synchronized(serverListeners) {
            serverListeners.remove(listener)
        }
    }

    fun unregisterConnectionListener(listener: IConnectionStatusNotification) {
        synchronized(connListeners) {
            connListeners.remove(listener)
        }
    }
}