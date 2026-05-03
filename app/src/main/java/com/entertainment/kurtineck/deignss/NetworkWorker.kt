package com.entertainment.kurtineck.deignss

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.*

object NetworkWorker {

    private val mSleepDurationConnected: Int = 60
    private val mSleepDurationDisconnected: Int = 10
    var isOnline: Boolean = true
    var adLimitEnabled: Boolean = false

    // ❌ Removed: lateinit var appContext: Context

    fun runNetworkCheckingThread(context: Context) { // ✅ Accept Context as parameter
        val appContext = context.applicationContext   // ✅ Safe, no lateinit needed
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            try {
                while (!adLimitEnabled) {
                    isOnline = isDeviceOnline(appContext)
                    logTheDeviceStatus()
                    sleepForSomeTime(isOnline)
                }
            } catch (throwable: Throwable) {
                Log.e("error", throwable.printStackTrace().toString())
            }
        }
    }

    private fun logTheDeviceStatus() {
        Log.d("NetworkStatus", isOnline.toString())
    }

    private fun sleepForSomeTime(networkStatus: Boolean) {
        try {
            if (networkStatus) sleepUsingCoroutines(mSleepDurationConnected)
            else sleepUsingCoroutines(mSleepDurationDisconnected)
        } catch (ex: Exception) {
            Log.e("error", ex.printStackTrace().toString())
        }
    }

    private fun sleepUsingCoroutines(timeinSec: Int) {
        try {
            runBlocking { delay(timeinSec * 1000L) }
        } catch (ex: Exception) {
            Log.e("ex", ex.printStackTrace().toString())
        }
    }

    fun isDeviceOnline(context: Context): Boolean { // ✅ Context passed in, no lateinit
        return try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities =
                connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}