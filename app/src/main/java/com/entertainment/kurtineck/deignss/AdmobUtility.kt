package com.entertainment.kurtineck.deignss

import android.app.Activity
import android.content.Context
import android.os.CountDownTimer
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.entertainment.kurtineck.deignss.AdObject.isTimerInProgress
import com.entertainment.kurtineck.deignss.AdObject.mCountDownTimer
import com.entertainment.kurtineck.deignss.NetworkWorker.adLimitEnabled
import com.entertainment.kurtineck.deignss.NetworkWorker.isOnline
import java.sql.Timestamp
import java.util.*
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat

private var mInterstitialAd: InterstitialAd? = null
private var mAdIsLoading: Boolean = false


var TAG = "Admob"

class AdmobUtility(private val ctx: FragmentActivity?, val appInterfaces: AppInterfaces, var SPLASH_SCREEN: Boolean = false) {
    var proceedToNextScreen: () -> Unit? = {
//        AppUtils().showSnackbarMsg("Ad failed to Load.Callback Not set.No connection.")
    }

    init {
        showAlertIfInterstitialIDNotSet()
        loadAdWithConnectivityCheck()
    }

    private fun showAlertIfInterstitialIDNotSet() {
        if (AdObject.INTERSTITIAL_ID.isEmpty()) {
            Log.d(TAG,"Please setup interstitial ID.")
//            AppUtils().showSnackbarMsg("Please setup the Interstitial ID.")
        }

    }

    /*------------LOAD THE NEXT SCREEN AFTER SHOWING THE INTERSTITIAL AD---------------------*/
    fun loadNextScreen(cb: () -> Unit) {
        proceedToNextScreen = cb
        if (isNetworkNotAvailOrTimerNotExpired()) {
            proceedToNextScreen()
            return
        }
        if (isAdLoaded()) {
            if (!showAdWithConnectivityCheck()) {
                proceedToNextScreen()
            }
        } else {
            loadAdWithConnectivityCheck()
//            AppUtils().logErrorMsg("The interstitial wasn't loaded yet. Loading it now and will show it next time.")
            Log.d("ERROR", "The interstitial wasn't loaded yet. Loading it now and will show it next time.")
            proceedToNextScreen() //show the ad next time.
        }
    }

    /*---------------------------------------------------------------------------------------------------------*/
    fun loadAdWithConnectivityCheck() {
        if (isAdNotLoadedAndNetworkAvail()) {
            setAdIsLoading()
            loadInterstitialAd()
        } else if (SPLASH_SCREEN == true) {
            appInterfaces.loadStartScreen()
        }
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(ctx as Context, AdObject.INTERSTITIAL_ID, adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
//                        AppUtils().logErrorMsg("--------Ad Failed to Load------------")
//                        AppUtils().logErrorMsg(adError.message)
                        Log.d(TAG, "Ad Failed to Load")
                        Log.d(TAG, adError.message)
                        clearOldInterstitialAd()
                        setAdIsNotLoading()
                        checkIfAdLimitEnabled(adError)
                        showAdFailedToLoadErrorMsg(adError)
                        onAdFailedLogic()
                    }

                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
//                        AppUtils().logDebugMsg("Ad loaded successfully.")
                        Log.d(TAG,"Ad loaded successfully.")
                        mInterstitialAd = interstitialAd
                        setAdIsNotLoading()
//                        AppUtils().showSnackbarMsg("onAdLoaded")
                        showAdIfInSplashScreen()
                    }
                })
    }
    private fun checkIfAdLimitEnabled(adError: LoadAdError) {
        if (adError.code==3) {
            adLimitEnabled = true
//            AppUtils().showSnackbarMsg("AdLimit Enabled for this app.")
        }
    }


    private fun onAdFailedLogic() {
        loadStartScreen()
    }

    private fun showAdFailedToLoadErrorMsg(adError: LoadAdError) {
        val error = "domain: ${adError.domain}, code: ${adError.
        code}, " +
                "message: ${adError.message}"
        /*Toast.makeText(
                ctx,
                "onAdFailedToLoad() with error $error",
                Toast.LENGTH_SHORT
        ).show()*/
//        AppUtils().logErrorMsg("onAdFailedToLoad() with error $error")
        Log.e(TAG,"onAdFailedToLoad() with error $error")
    }


    private fun isAdNotLoadedAndNetworkAvail(): Boolean {
        return (isOnline == true) and (mInterstitialAd == null) and (!adLimitEnabled)
    }

    private fun showAdWithConnectivityCheck(): Boolean {
        if (isAdLoaded()) {
            setupAdCallbacks()
            showInterstitialAd()
            return true
        } else if (SPLASH_SCREEN == true) {
            appInterfaces.loadStartScreen()
        }
        return false
    }

    private fun setupAdCallbacks() {
        /***********NEW*******/
        mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
//                AppUtils().logDebugMsg("Ad was dismissed.")
                Log.d(TAG, "Ad was dismissed.")
                // Restore insets when ad closes
                reapplyWindowInsets(ctx) // Pass ctx directly
                // 2. RESTORE THE APP CONTENT VISIBILITY
                showRootContentView(ctx)
                clearOldInterstitialAd()
                loadAdWithConnectivityCheck()
                proceedToNextScreen()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
//                AppUtils().logDebugMsg("Ad failed to show.")
                Log.d(TAG, "Ad failed to show.")
                // Restore insets when ad closes
                reapplyWindowInsets(ctx) // Pass ctx directly
                // 2. RESTORE THE APP CONTENT VISIBILITY
                showRootContentView(ctx)
                clearOldInterstitialAd()
                reloadAdToShowLater()
            }

            override fun onAdShowedFullScreenContent() {
//                AppUtils().logDebugMsg("Ad showed fullscreen content.")
                Log.d(TAG, "Ad showed fullscreen content.")
                mInterstitialAd = null
                getAdOpenTimestamp()
            }
        }
        /***********NEW*******/

    }


    private fun showInterstitialAd() {
        if (mInterstitialAd != null) {
            // Prepare UI for fullscreen ad BEFORE showing
            prepareForFullscreenAd(ctx) // Pass ctx directly
            // 2. FORCE HIDE THE APP CONTENT
            hideRootContentView(ctx)
            mInterstitialAd?.show(ctx as Activity)
        } else {
            AppUtils().logDebugMsg("The interstitial ad wasn't ready yet.")
//            Log.d(TAG, "The interstitial ad wasn't ready yet.")
        }
    }

    private fun reloadAdToShowLater() {
        AdObject.TIME_LAST_LOADED = Timestamp(Date().time)
        if (SPLASH_SCREEN) {
            proceedToNextScreen = { appInterfaces.loadStartScreen() }
            SPLASH_SCREEN = false
        }
        proceedToNextScreen()
    }

    private fun showAdIfInSplashScreen() {
        /*--SHOW THE AD IF THE SCREEN IS SPLASH SCREEN--*/
        if (SPLASH_SCREEN) {
            proceedToNextScreen = { appInterfaces.loadStartScreen() }
            proceedToNextScreen()
            SPLASH_SCREEN = false
        }
    }

    private fun getAdOpenTimestamp() {
        startTimer(AdObject.INTERSTITIAL_LENGTH_MILLISECONDS)
        loadAdWithConnectivityCheck()
        proceedToNextScreen()
    }

    fun startTimer(milliseconds: Long) {
        isTimerInProgress = true
        createTimer(milliseconds)
        mCountDownTimer?.start()
    }


}

private fun AdmobUtility.loadStartScreen() {
    /*--SHOW THE AD IF THE SCREEN IS SPLASH SCREEN--*/
    if (SPLASH_SCREEN == true) {
        proceedToNextScreen = { appInterfaces.loadStartScreen() }
        SPLASH_SCREEN = false
    }
    proceedToNextScreen()
}

private fun isAdLoaded(): Boolean {
    if (mInterstitialAd != null) {
        return true
    } else return false
}

private fun setAdIsLoading() {
    mAdIsLoading = true
}

private fun clearOldInterstitialAd() {
    mInterstitialAd = null
}

private fun setAdIsNotLoading() {
    mAdIsLoading = false
}

private fun isNetworkNotAvailOrTimerNotExpired(): Boolean {
    if ((isOnline == false) or !showAdOrNot()) {
        return true
    } else return false
}

private fun showAdOrNot(): Boolean {
    var result = false
    if (didTimerNotStart()) {
        result = true
    } else if (isTimerExpired()) {
        result = true
    }
    return result
}


private fun didTimerNotStart(): Boolean {
    return mCountDownTimer == null
}

fun isTimerExpired(): Boolean {
    if (isTimerInProgress) return false else return true
}


private fun createTimer(milliseconds: Long) {
    mCountDownTimer?.cancel()
    mCountDownTimer = object : CountDownTimer(milliseconds, 500) {
        override fun onTick(millisUntilFinished: Long) {
        }

        override fun onFinish() {
            isTimerInProgress = false
        }
    }
}

/****************************/
private fun prepareForFullscreenAd(ctx: FragmentActivity?) {
    // Remove redundant null check since ctx is already nullable
    ctx?.let { context ->
        try {
            // FragmentActivity IS an Activity, no need to cast
            val activity = context // ctx is already FragmentActivity which extends Activity

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Modern approach - just ensure immersive mode
//                activity.window.setDecorFitsSystemWindows(false)

                val controller = activity.window.insetsController
                controller?.let {
                    it.hide(WindowInsets.Type.systemBars())
                    it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
            Log.d(TAG, "Prepared UI for fullscreen ad")
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing for fullscreen ad: ${e.message}")
        }
    }
}
private fun reapplyWindowInsets(ctx: FragmentActivity?) {
    ctx?.let { context ->
        try {
            // FragmentActivity IS an Activity, no need to cast
            val activity = context

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Restore edge-to-edge with system bars visible
//                activity.window.setDecorFitsSystemWindows(false)

                val controller = activity.window.insetsController
                controller?.let {
                    it.show(WindowInsets.Type.systemBars())
                    it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT
                }
            }
            // Trigger insets reapplication with proper error handling
            val rootView = activity.findViewById<ConstraintLayout>(R.id.clMainActivity)
            if (rootView != null) {
                ViewCompat.requestApplyInsets(rootView)
                rootView.post {
                    applyWindowInsets(activity)
                }
                Log.d(TAG, "Root view found and insets requested")
            } else {
                Log.w(TAG, "Root view (clMainActivity) not found")
                // Fallback: try to find any ConstraintLayout
                val fallbackRoot = activity.findViewById<View>(android.R.id.content)
                fallbackRoot?.let {
                    ViewCompat.requestApplyInsets(it)
                    Log.d(TAG, "Used fallback content view for insets")
                }
            }

            Log.d(TAG, "Restored edge-to-edge UI with insets")
        } catch (e: Exception) {
            Log.e(TAG, "Error reapplying window insets: ${e.message}")
        }
    }
}
private fun applyWindowInsets(activity: FragmentActivity) {
    try {
        // More robust type checking and method calling
        when {
            // Check if it's your MainActivity
            activity.javaClass.simpleName == "MainActivity" -> {
                Log.d(TAG, "Triggering insets reapplication on MainActivity")

                // Try multiple approaches to reapply insets
                val rootView = activity.findViewById<ConstraintLayout>(R.id.clMainActivity)
                if (rootView != null) {
                    // Method 1: Request layout
                    rootView.requestLayout()

                    // Method 2: Force insets dispatch
                    ViewCompat.requestApplyInsets(rootView)

                    // Method 3: Post delayed to ensure UI thread availability
                    rootView.postDelayed({
                        rootView.invalidate()
                        Log.d(TAG, "Forced layout invalidation")
                    }, 100)

                } else {
                    Log.w(TAG, "MainActivity root view not found")
                }
            }

            // Generic fallback for any activity
            else -> {
                Log.d(TAG, "Generic insets reapplication for ${activity.javaClass.simpleName}")
                val contentView = activity.findViewById<View>(android.R.id.content)
                contentView?.let {
                    it.requestLayout()
                    ViewCompat.requestApplyInsets(it)
                }
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error in applyWindowInsets: ${e.message}")
    }
}

/**
 * Hides the Activity's root content view to ensure the Interstitial Ad draws over a blank screen.
 * Should be called right BEFORE mInterstitialAd?.show().
 */
private fun hideRootContentView(activity: FragmentActivity?) {
    activity?.let {
        try {
            // android.R.id.content is the root container of your Activity's layout.
            it.findViewById<View>(android.R.id.content)?.visibility = View.GONE
            Log.d(TAG, "Root Content View Hidden.")
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding root view: ${e.message}")
        }
    }
}

/**
 * Restores the visibility of the Activity's root content view.
 * Must be called in the Ad's FullScreenContentCallback (onAdDismissed/onAdFailedToShow).
 */
private fun showRootContentView(activity: FragmentActivity?) {
    activity?.let {
        try {
            it.findViewById<View>(android.R.id.content)?.visibility = View.VISIBLE
            Log.d(TAG, "Root Content View Shown.")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing root view: ${e.message}")
        }
    }
}
