package com.entertainment.kurtineck.deignss

import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.ViewGroupCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import com.entertainment.kurtineck.deignss.AdObject.FRAGMENT_LOADED
import com.entertainment.kurtineck.deignss.AdObject.fragmentsStack
import com.entertainment.kurtineck.deignss.AdObject.mCountDownTimer
import com.entertainment.kurtineck.deignss.NetworkWorker.adLimitEnabled
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity(), AppInterfaces {

    private var BannerLoaded = false
    private var DB_NAME = "db_temp01.db"

    private val StartScreen = "START_SCREEN"
    private val TOPICS = "TOPICS"
    private val MENUS = "MENUS"
    private val ITEM = "ITEM"
    private val BookmarkMenu = "BOOKMARK_MENU"
    private val BookmarkItem = "BOOKMARK_ITEM"
    private val PrivacyPolicy = "PRIVACY_POLICY"
    private lateinit var adBanner: AdView
    private lateinit var clMainActivity: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ STEP 1: Enable edge-to-edge
        enableEdgeToEdge()

        // ✅ STEP 2: Set content view
        setContentView(R.layout.activity_main)

        // ✅ STEP 3: Initialize views
        adBanner = findViewById(R.id.adBanner)
        clMainActivity = findViewById(R.id.clMainActivity)

        // ✅ STEP 4: Enable backward compatible inset dispatching
        ViewGroupCompat.installCompatInsetsDispatch(clMainActivity as ViewGroup)

        // ✅ STEP 5: Setup system bars (Force transparency)
        setupSystemBars()

        // ✅ STEP 6: Apply window insets
        applyWindowInsets()

        // Continue with other initialization
        startANRWatchDog()
        initializeAdmob()
        loadSplashScreen()
        setupBannerAdListeners()
        loadBannerWithConnectivityCheck()
        startNetworkMonitoringServiceUsingCoroutines()
        runInitializationInBackground()
        setDefaultExceptionHandler()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isNotLastScreen()) {
                    fragmentsStack.pop()
                    loadPreviousFragment()
                } else {
                    FRAGMENT_LOADED = false
                    AdObject.SPLASH_CALLED = false
                    exitApplication()
                }
            }
        })
    }

    private fun setupSystemBars() {
        // 1. Force Transparent Navigation Bar
        window.navigationBarColor = Color.TRANSPARENT

        // 2. Disable contrast enforcement on Android 10+ to prevent system scrims
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        // 3. Handle display cutouts
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        // 4. Configure icons (Light/Dark)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.show(WindowInsets.Type.systemBars())
                controller.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                            WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                            WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                )
            }
        } else {
            WindowCompat.getInsetsController(window, window.decorView).let { controller ->
                controller.isAppearanceLightStatusBars = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    controller.isAppearanceLightNavigationBars = true
                }
            }
        }
    }

    private fun applyWindowInsets() {
        val rootLayout = findViewById<ConstraintLayout>(R.id.clMainActivity)
        val navHostFragmentContainer = findViewById<FragmentContainerView>(R.id.nav_host_fragment)
        val adBannerLayout = findViewById<AdView>(R.id.adBanner)
        val bottomSeparatorLine = findViewById<View>(R.id.bottom_separator_line)

        setupRootLayoutInsets(rootLayout)
        setupNavHostInsets(navHostFragmentContainer)
        setupAdBannerInsets(adBannerLayout)

        // ✅ Apply insets to the bottom separator
        if (bottomSeparatorLine != null) {
            setupBottomSeparatorInsets(bottomSeparatorLine)
        }
    }

    private fun setupBottomSeparatorInsets(separator: View) {
        ViewCompat.setOnApplyWindowInsetsListener(separator) { view, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.displayCutout()
            )

            // Set the height of this view to exactly match the bottom inset (Nav Bar height)
            // This creates the colored background behind the nav bar
            if (view.layoutParams.height != bars.bottom) {
                view.updateLayoutParams {
                    height = bars.bottom
                }
            }

            windowInsets
        }
    }

    private fun setupRootLayoutInsets(rootLayout: View) {
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { _, windowInsets ->
            windowInsets
        }
    }

    private fun setupNavHostInsets(navHostContainer: View) {
        ViewCompat.setOnApplyWindowInsetsListener(navHostContainer) { view, windowInsets ->
            // NavHost handles its own insets via Fragments
            windowInsets
        }
    }

    private fun setupAdBannerInsets(adBanner: View) {
        ViewCompat.setOnApplyWindowInsetsListener(adBanner) { view, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.displayCutout()
            )
            val navigationBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())

            val currentParams = view.layoutParams as? ViewGroup.MarginLayoutParams
            if (currentParams != null) {
                val originalMargins = view.tag as? IntArray ?: IntArray(4).apply {
                    this[0] = currentParams.leftMargin
                    this[1] = currentParams.topMargin
                    this[2] = currentParams.rightMargin
                    this[3] = currentParams.bottomMargin
                    view.tag = this
                }

                // Just apply side margins if needed
                val newLeftMargin = originalMargins[0] + bars.left
                val newRightMargin = originalMargins[2] + bars.right

                // ✅ Push AdBanner UP by the height of nav bar / keyboard
                // We add 8dp (original margin) + system bar height
                val newBottomMargin = originalMargins[3] + maxOf(navigationBars.bottom, ime.bottom)

                if (currentParams.leftMargin != newLeftMargin ||
                    currentParams.rightMargin != newRightMargin ||
                    currentParams.bottomMargin != newBottomMargin
                ) {
                    currentParams.leftMargin = newLeftMargin
                    currentParams.rightMargin = newRightMargin
                    currentParams.bottomMargin = newBottomMargin
                    view.layoutParams = currentParams
                }
            }
            windowInsets
        }
    }

    // ... Rest of your existing methods (startANRWatchDog, runInitializationInBackground, etc.) ...
    // Keep them exactly as they were in your previous code

    private fun startANRWatchDog() { /* ... */ }
    private fun runInitializationInBackground() {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            AdObject.snackbarContainer = findViewById(R.id.clMainActivity)
            loadDataFromAssets()
            setupDB()
            initializeAdobject()
            createBookmarkDir()
        }
    }
    private fun setupDB() {
        DB_NAME = getAssetsDBFileName()
        DataBaseHelper(this, DB_NAME).let { ItemDataset.mDbHelper = it }
        ItemDataset.TOPIC_ID = 1
        ItemDataset.MENU_ID = 1
    }
    private fun getAssetsDBFileName() = packageName.replace(".", "_")
    private fun initializeAdobject() {
        AdObject.connectivityManager =
            applicationContext.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        AdObject.PACKAGE_NAME = packageName
    }
    private fun createBookmarkDir() {
        ItemDataset.APP_DIR = File(filesDir, "bookmarks")
        ItemDataset.APP_DIR?.mkdirs()
    }
    private fun loadDataFromAssets() {
        AppUtils().loadGalleryFromAssets(applicationContext)
    }
    private fun initializeAdmob() {
        MobileAds.initialize(this) {}
    }
    private fun setDefaultExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            System.err.println(e.printStackTrace())
        }
    }
    private fun startNetworkMonitoringServiceUsingCoroutines() {
        NetworkWorker.runNetworkCheckingThread()
    }
    private fun exitApplication() {
        val a = Intent(Intent.ACTION_MAIN)
        a.addCategory(Intent.CATEGORY_HOME)
        a.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(a)
        finish()
        finishAffinity()
        exitProcess(0)
    }
    private fun isNotLastScreen(): Boolean {
        return fragmentsStack.size > 1
    }
    override fun loadSplashScreen() {
        if (!AdObject.SPLASH_CALLED) navigateToScreenUsingNagGraph(SplashFragment())
    }
    override fun loadTestModeScreen() {
        if (!isFinishing) {
            supportFragmentManager.beginTransaction().apply {
                replace(R.id.nav_host_fragment, TestModeFragment())
                commitAllowingStateLoss()
            }
            loadBannerWithConnectivityCheck()
        }
    }
    override fun loadStartScreen() {
        navigateToScreenUsingNagGraph(StartScreenFragment())
        addFragmentToStack(StartScreen)
    }
    override fun loadPrivacyPolicy() {
        navigateToScreenUsingNagGraph(PrivacyPolicyFragment())
        addFragmentToStack(PrivacyPolicy)
    }
    override fun loadImageTopics() {
        navigateToScreenUsingNagGraph(TopicFragment())
        addFragmentToStack(TOPICS)
    }
    override fun loadMenus() {
        navigateToScreenUsingNagGraph(MenuFragment())
        addFragmentToStack(MENUS)
    }
    override fun loadItem() {
        navigateToScreenUsingNagGraph(ItemFragment02())
        addFragmentToStack(ITEM)
    }
    override fun loadBookMarkMenu() {
        navigateToScreenUsingNagGraph(BookmarkFragment())
        addFragmentToStack(BookmarkMenu)
    }
    override fun loadBookMarkItem() {
        navigateToScreenUsingNagGraph(BookMarkItemFragment())
        addFragmentToStack(BookmarkItem)
    }
    private fun addFragmentToStack(fragmentScreen: String) {
        FRAGMENT_LOADED = true
        fragmentsStack.push(fragmentScreen)
    }
    private fun navigateToScreenUsingNagGraph(destinationFrag: Fragment) {
        if (!isFinishing) {
            supportFragmentManager.beginTransaction().apply {
                replace(R.id.nav_host_fragment, destinationFrag)
                commitAllowingStateLoss()
                addToBackStack(null)
            }
            FRAGMENT_LOADED = true
            loadBannerWithConnectivityCheck()
        }
    }
    private fun loadBannerWithConnectivityCheck() {
        if (AdObject.isNetworkAvailable() &&
            !adBanner.isLoading &&
            !BannerLoaded &&
            !adLimitEnabled) {
            adBanner.loadAd(AdRequest.Builder().build())
        }
    }
    private fun setupBannerAdListeners() {
        adBanner.adListener = object : AdListener() {
            override fun onAdLoaded() { BannerLoaded = true }
            override fun onAdOpened() {}
            override fun onAdClosed() {}
            override fun onAdFailedToLoad(error: LoadAdError) {
                super.onAdFailedToLoad(error)
                AppUtils().showSnackbarMsg("Banner failed to load.${error.message}")
            }
        }
    }
    override fun onResume() {
        super.onResume()
        if (AdObject.isTimerInProgress) restartTimer()
        resumePausedFragment()
    }
    private fun resumePausedFragment() {
        if (FRAGMENT_LOADED == true) {
            val prevScreen = fragmentsStack.peek()
            when (prevScreen) {
                StartScreen -> loadStartScreen()
                TOPICS -> loadImageTopics()
                MENUS -> loadMenus()
                ITEM -> loadItem()
                BookmarkItem -> loadBookMarkItem()
                BookmarkMenu -> loadBookMarkMenu()
                PrivacyPolicy -> loadPrivacyPolicy()
            }
        } else {
            loadSplashScreen()
        }
    }
    private fun loadPreviousFragment() {
        when (fragmentsStack.pop() as String) {
            StartScreen -> loadStartScreen()
            TOPICS -> loadImageTopics()
            MENUS -> loadMenus()
            ITEM -> loadItem()
            BookmarkItem -> loadBookMarkItem()
            BookmarkMenu -> loadBookMarkMenu()
            PrivacyPolicy -> loadPrivacyPolicy()
        }
    }
    private fun restartTimer() {
        AdObject.admob?.startTimer(AdObject.INTERSTITIAL_LENGTH_MILLISECONDS)
    }
    override fun onPause() {
        cancelTimer()
        super.onPause()
    }
    private fun cancelTimer() {
        mCountDownTimer?.cancel()
    }
}
