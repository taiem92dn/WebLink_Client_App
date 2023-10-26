package com.tngdev.weblinkclient

/**
 * Helper interface for handlers of the ping timeout.
 */
interface IPingHandler {

    /**
     * Called when the host app is no longer communicating with the client actively.
     */
    fun onPingResponseTimeout()

    /**
     * Called while communication is active and the host app is responding.
     *
     * The host app reports its status in this call, so if needed the client may want to handle the
     * case where the host app is connected and responsive, but reports as inactive.
     *
     * @param isSenderInactive the host app's reported activity status.
     *                         If true, the host app is not configured.
     */
    fun onPingResponseReceived(isSenderInactive: Boolean)
}