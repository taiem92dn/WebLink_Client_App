package com.tngdev.weblinkclient

import android.view.View
import com.abaltatech.mcs.connectionmanager.PeerDevice
import com.abaltatech.weblinkclient.IClientNotification

interface IWebLinkActivity {

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
    fun connectRequest(device: PeerDevice): Boolean

    /**
     * Starts the WebLink fragment.
     *
     * The first time this method is called, it will create the WebLink Fragment.
     *
     * This method attaches the WebLink fragment to the WebLink Container layout.
     */
    fun startWebLink()

    /***
     * Hide the soft input keyboard
     */
    fun hideKeyboard()

    /**
     * Detaches the WebLink Fragment and destroys it.
     */
    fun stopWebLink()

    /**
     * Show soft input for the dummy edit text.  While shown, the events will be forwarded
     * to the provided Key listener.
     * @param listener Key listener for the duration of this keyboard.
     */
    fun showKeyboard(listener: View.OnKeyListener)
}