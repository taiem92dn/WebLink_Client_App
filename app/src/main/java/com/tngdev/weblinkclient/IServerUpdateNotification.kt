package com.tngdev.weblinkclient

import com.abaltatech.weblinkclient.IClientNotification.ServerInfo

/**
 * The server list update notification interface.
 */
interface IServerUpdateNotification {

    /**
     * Called when the list of active WL servers has changed
     * @param servers the list of active WL servers
     */
    fun onServerListUpdated(servers: Array<ServerInfo>)
}