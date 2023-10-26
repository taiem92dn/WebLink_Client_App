package com.tngdev.weblinkclient

import android.app.Application
import com.abaltatech.mcs.logger.MCSLogger
import com.abaltatech.mcs.logger.android.LoggerAndroid
import com.abaltatech.weblinkclient.WebLinkClientCore

class App: Application() {

    lateinit var wlClient: WebLinkClient
        private set

    override fun onCreate() {
        super.onCreate()

        instance = this
        wlClient = WebLinkClient(this)

        //register MCSLogger. used for internal logs.
        MCSLogger.registerLogger(LoggerAndroid())
        MCSLogger.setLogLevel(MCSLogger.eAll)
    }

    fun getWebLinkClientCore() = instance.wlClient.getWebLinkClientCore()

    companion object {

        lateinit var instance: App
            private set

    }
}