package com.entertainment.kurtineck.deignss

import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
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

        // 1. Enable Edge-to-Edge
        enableEdgeToEdge()

        // 2. Set Content
        setContentView(R.layout.activity_main)

        // 3. Initialize Views
        adBanner = findViewById(R.id.adBanner)
        clMainActivity = findViewById(R.id.clMainActivity)
        adBanner.visibility = View.GONE

        // 4. Backward Compatibility
        ViewGroupCompat.installCompatInsetsDispatch(clMainActivity as ViewGroup)

        // 5. Setup System Bars
        setupSystemBars()

        // 6. Apply Insets
        applyWindowInsets()

        // Initialization
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
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.show(WindowInsets.Type.systemBars())
                // Status Bar = White Icons (0), Nav Bar = Black Icons (APPEARANCE_LIGHT...)
                controller.setSystemBarsAppearance(
                    0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
                controller.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                )
            }
        } else {
            WindowCompat.getInsetsController(window, window.decorView).let { controller ->
                controller.isAppearanceLightStatusBars = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    controller.isAppearanceLightNavigationBars = true
                }
            }
        }
    }

    private fun applyWindowInsets() {
        val navBarSpacer = findViewById<View>(R.id.navBarSpacer)
        val adBannerLayout = findViewById<AdView>(R.id.adBanner)

        // ✅ 1. Set Spacer Height to match Nav Bar
        if (navBarSpacer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(navBarSpacer) { view, windowInsets ->
                val bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or
                            WindowInsetsCompat.Type.displayCutout()
                )
                val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())

                // Height = Nav Bar + Keyboard
                val newHeight = maxOf(bars.bottom, ime.bottom)

                if (view.layoutParams.height != newHeight) {
                    view.updateLayoutParams { height = newHeight }
                }
                windowInsets
            }
        }

        // ✅ 2. Handle Side Insets for Ad (No bottom margin needed anymore)
        ViewCompat.setOnApplyWindowInsetsListener(adBannerLayout) { view, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.displayCutout()
            )
            // Just apply side padding/margin if needed
            view.updatePadding(left = bars.left, right = bars.right)
            windowInsets
        }
    }

    // ... Rest of initialization methods (unchanged) ...
    private fun startANRWatchDog() {}
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
            override fun onAdLoaded() {
                BannerLoaded = true
                adBanner.visibility = View.VISIBLE
            }
            override fun onAdOpened() {}
            override fun onAdClosed() {}
            override fun onAdFailedToLoad(error: LoadAdError) {
                super.onAdFailedToLoad(error)
                adBanner.visibility = View.GONE
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
