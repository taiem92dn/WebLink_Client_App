package com.tngdev.weblinkclient.util

import android.app.ActivityManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Build
import java.util.SortedMap
import java.util.TreeMap

object Utils {
    fun getVersionName(context: Context): String? {
        val packageManager = context.packageManager
        if (packageManager != null) {
            return try {
                val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
                packageInfo.versionName
            } catch (e: NameNotFoundException) {
                null
            }
        }

        return null
    }

    /**
     * Helper method which checks if an application is running.
     *
     * @param context Context
     * @param packageName Package name of the application to check
     * @return true if the application is running, false otherwise
     */
    fun isAppRunning(context: Context, packageName: String): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            val procInfos = activityManager.runningAppProcesses
            if (procInfos != null) {
                for (processInfo in procInfos) {
                    if (processInfo.processName == packageName) {
                        return true
                    }
                }
            }
        } else {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

            val time = System.currentTimeMillis()
            val appList =
                usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 1000, time)
            if (appList != null && appList.size > 0) {
                val mySortedMap: SortedMap<Long, UsageStats> = TreeMap()
                for (usageStats in appList) {
                    mySortedMap[usageStats.lastTimeUsed] = usageStats
                }
                if (!mySortedMap.isEmpty()) {
                    if (mySortedMap[mySortedMap.lastKey()]!!.packageName == packageName) {
                        return true
                    }
                }
            }
        }
        return false
    }
}