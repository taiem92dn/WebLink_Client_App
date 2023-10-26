package com.tngdev.weblinkclient

import com.abaltatech.mcs.connectionmanager.EConnectionResult
import com.abaltatech.mcs.connectionmanager.PeerDevice

/**
 * The WEBLINK connection status notification interface.
 */
interface IConnectionStatusNotification {
    /**
     * Called when connection to the server has been established
     *
     * @param peerDevice PeerDevice which connects successfully
     */
    fun onConnectionEstablished(peerDevice: PeerDevice)

    /**
     * Called when failed to connect to the server
     *
     * @param peerDevice PeerDevice which fails to connect
     * @param result Reason for connection failing
     */
    fun onConnectionFailed(peerDevice: PeerDevice, result: EConnectionResult)

    /**
     * Called when connection to the server has been closed
     *
     * @param peerDevice PeerDevice which connection is closed
     */
    fun onConnectionClosed(peerDevice: PeerDevice)
}