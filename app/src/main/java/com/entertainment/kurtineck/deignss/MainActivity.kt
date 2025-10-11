package com.entertainment.kurtineck.deignss

import android.content.Intent
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
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.entertainment.kurtineck.deignss.AdObject.FRAGMENT_LOADED
import com.entertainment.kurtineck.deignss.AdObject.fragmentsStack
import com.entertainment.kurtineck.deignss.AdObject.mCountDownTimer
import com.entertainment.kurtineck.deignss.NetworkWorker.adLimitEnabled
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity(), AppInterfaces {

    private lateinit var navController: NavController
    private lateinit var navHostFragment: NavHostFragment
    private var BannerLoaded = false
    private var DB_NAME = "db_temp01.db" //CHANGE THE DB NAME FOR EVERY APP

    private val StartScreen = "START_SCREEN"
    private val TOPICS = "TOPICS"
    private val MENUS = "MENUS"
    private val ITEM = "ITEM"
    private val BookmarkMenu = "BOOKMARK_MENU"
    private val BookmarkItem = "BOOKMARK_ITEM"
    private val PrivacyPolicy = "PRIVACY_POLICY"
    private lateinit var adBanner:com.google.android.gms.ads.AdView

    private lateinit var clMainActivity: View // Using the existing ID for the root layout


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        adBanner = findViewById(R.id.adBanner)
        clMainActivity = findViewById(R.id.clMainActivity) // Initialize clMainActivity
        applyWindowInsets()
        setupSystemBars() // Call the method to handle status and navigation bars
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
                    // If there's a fragment on the back stack, pop it and load the previous one
                    fragmentsStack.pop()
                    loadPreviousFragment()
                } else {
                    // This is the last screen, so we perform the exit logic
                    FRAGMENT_LOADED = false
                    AdObject.SPLASH_CALLED = false
                    exitApplication()
                }
            }
        })
    }

    private fun applyWindowInsets() {
        Log.d("EdgeToEdgeTiming", "applyWindowInsets START")
        val startTime = System.currentTimeMillis()

        val rootLayout = findViewById<ConstraintLayout>(R.id.clMainActivity)
        val navHostFragmentContainer = findViewById<FragmentContainerView>(R.id.nav_host_fragment)
        val adBannerLayout = findViewById<AdView>(R.id.adBanner)

        Log.d("EdgeToEdgeTiming", "Setting up ALL insets")
        setupRootLayoutInsets(rootLayout)
        setupNavHostInsets(navHostFragmentContainer)
        setupAdBannerInsets(adBannerLayout)
        Log.d(
            "EdgeToEdgeTiming",
            "ALL insets DONE. Duration: ${System.currentTimeMillis() - startTime}ms"
        )


        val endTime = System.currentTimeMillis()
        Log.d("EdgeToEdgeTiming", "applyWindowInsets END. Total Duration: ${endTime - startTime}ms")
    }

    private fun setupRootLayoutInsets(rootLayout: View) {
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view, windowInsets ->
            // If the root layout itself doesn't need padding (because children handle it),
            // you can simply return the insets to be passed along, or consume them if fully handled.
            // For example, if it has a background that should go edge to edge:
            // view.updatePadding(top = 0, bottom = 0, left = 0, right = 0) // Ensure no default padding interferes
            windowInsets // Pass insets down to children or other listeners.
        }
    }

    private fun setupNavHostInsets(navHostContainer: View) {
        ViewCompat.setOnApplyWindowInsetsListener(navHostContainer) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            if (view.paddingTop != systemBars.top ||
                view.paddingLeft != systemBars.left ||
                view.paddingRight != systemBars.right) {
                // Log.d("EdgeToEdgeDebug", "NavHost: Applying padding - Top: ${systemBars.top}, Left: ${systemBars.left}, Right: ${systemBars.right}")
                view.updatePadding(
                    top = systemBars.top,
                    left = systemBars.left,
                    right = systemBars.right
                )
            } else {
                // Log.d("EdgeToEdgeDebug", "NavHost: Padding already correct.")
            }
            windowInsets
        }
    }


    private fun setupAdBannerInsets(adBanner: View) {
        ViewCompat.setOnApplyWindowInsetsListener(adBanner) { view, windowInsets ->
            val navigationBarsAndIme = windowInsets.getInsets(
                WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.ime()
            )
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            val currentParams = view.layoutParams as? ViewGroup.MarginLayoutParams
            if (currentParams != null) {
                val originalMargins = view.tag as? IntArray ?: IntArray(4).apply {
                    this[0] = currentParams.leftMargin
                    this[1] = currentParams.topMargin
                    this[2] = currentParams.rightMargin
                    this[3] = currentParams.bottomMargin
                    view.tag = this
                }

                val newLeftMargin = originalMargins[0] + systemBars.left
                val newRightMargin = originalMargins[2] + systemBars.right
                val newBottomMargin = originalMargins[3] + navigationBarsAndIme.bottom

                if (currentParams.leftMargin != newLeftMargin ||
                    currentParams.rightMargin != newRightMargin ||
                    currentParams.bottomMargin != newBottomMargin) {
                    // Log.d("EdgeToEdgeDebug", "AdBanner: Applying margins - Bottom: $newBottomMargin, Left: $newLeftMargin, Right: $newRightMargin")
                    currentParams.leftMargin = newLeftMargin
                    currentParams.rightMargin = newRightMargin
                    currentParams.bottomMargin = newBottomMargin
                    view.layoutParams = currentParams // Important: Re-assign params
                } else {
                    // Log.d("EdgeToEdgeDebug", "AdBanner: Margins already correct.")
                }
            }
            windowInsets
        }
    }

    private fun setupSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // API 28
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { // API 21
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // API 30
                window.setDecorFitsSystemWindows(false) // Allow content to draw under system bars
                window.insetsController?.let {
                    it.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    it.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                    it.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                    )
                }
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                                or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0
                        )
            }
        }
    }


    private fun startANRWatchDog() {
//        ANRWatchDog().start()
    }

    private fun runInitializationInBackground() {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            AdObject.snackbarContainer= findViewById(R.id.clMainActivity)
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

    private fun getAssetsDBFileName() = packageName.toString().replace(".", "_")

    private fun initializeAdobject() {
        AdObject.connectivityManager = applicationContext.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        AdObject.PACKAGE_NAME = packageName
    }

    private fun createBookmarkDir() {
        ItemDataset.APP_DIR = File(filesDir, "bookmarks")
        ItemDataset.APP_DIR?.let { it.mkdirs() }
    }

    private fun loadDataFromAssets() {
        AppUtils().loadGalleryFromAssets(applicationContext)
    }

    private fun initializeNavgraph() {
        navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
    }

    private fun initializeAdmob() {
        MobileAds.initialize(this) {}
    }

    private fun setDefaultExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler { t, e -> System.err.println(e.printStackTrace()) }
    }

    private fun startNetworkMonitoringServiceUsingCoroutines() {
        NetworkWorker.runNetworkCheckingThread()
    }


    /*----------------------ON BACK PRESS FOR THE ACTIVITY AND FRAGMENTS-------------------*/

//    override fun onBackPressed() {
//        if (isNotLastScreen()) {
//            fragmentsStack.pop()
//            loadPreviousFragment()
//        }else{
//            FRAGMENT_LOADED = false /*WHEN APP IS GOING INTO BACKGROUND, SET FRAGMENT_LOADED = FALSE*/
//            AdObject.SPLASH_CALLED = false
//            exitApplication()
//        }
//
//    }

    private fun exitApplication() {
        val a = Intent(Intent.ACTION_MAIN)
        a.addCategory(Intent.CATEGORY_HOME)
        a.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        a.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(a)
        finish()
        finishAffinity()
        exitProcess(0)
    }
    private fun isNotLastScreen(): Boolean {
        return fragmentsStack.size > 1
    }

    /*--------------------------------------------SCREEN LOADING VIA FRAGMENTS--------------------------------------------------------*/
    /*-----------------------SCREEN 0 - THE SPLASH SCREEN----------------------*/
    override fun loadSplashScreen() {

        if (!AdObject.SPLASH_CALLED) navigateToScreenUsingNagGraph(SplashFragment())

    }


    /*-----------------------SCREEN 0 - THE TEST MODE SCREEN----------------------*/
    override fun loadTestModeScreen() {
        if (!isFinishing) {
            supportFragmentManager.beginTransaction().apply {
                replace(R.id.nav_host_fragment, TestModeFragment())
                commitAllowingStateLoss()
            }

            this.loadBannerWithConnectivityCheck()
        }
    }

    /*-----------------------SCREEN 0 - THE MAIN SCREEN----------------------*/
    override fun loadStartScreen() {
        navigateToScreenUsingNagGraph(StartScreenFragment())
        addFragmentToStack(StartScreen)
    }

    override fun loadPrivacyPolicy() {
        navigateToScreenUsingNagGraph(PrivacyPolicyFragment())
        addFragmentToStack(PrivacyPolicy)
    }

    /*-----------------------SCREEN 1 - THE IMAGE TOPICS----------------------*/
    override fun loadImageTopics() {
        navigateToScreenUsingNagGraph(TopicFragment())
        addFragmentToStack(TOPICS)
    }

    /*---------------------SCREEN 2 - IMAGE MENUS---------------------*/
    override fun loadMenus() {
        navigateToScreenUsingNagGraph(MenuFragment())
        addFragmentToStack(MENUS)
    }

    /*------------------------------SCREEN 3 - THE IMAGE ITEM------------------------*/
    override fun loadItem() {
        navigateToScreenUsingNagGraph(ItemFragment02())
        addFragmentToStack(ITEM)
    }

    /*------------------------------SCREEN 4 - THE BOOK MARK MENU------------------------*/
    override fun loadBookMarkMenu() {
        navigateToScreenUsingNagGraph(BookmarkFragment())
        addFragmentToStack(BookmarkMenu)
    }

    /*------------------------------SCREEN 5 - THE BOOK MARK ITEM------------------------*/
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
            this.loadBannerWithConnectivityCheck()
        }
    }




    private fun loadBannerWithConnectivityCheck() {
        if (AdObject.isNetworkAvailable() and !adBanner.isLoading and !BannerLoaded and !adLimitEnabled) {
            adBanner.loadAd(AdRequest.Builder().build())
        }
    }

    private fun setupBannerAdListeners() {
        adBanner.adListener = object : AdListener() {
            override fun onAdLoaded() {
                BannerLoaded = true
            }



            override fun onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
            }



            override fun onAdClosed() {
                // Code to be executed when when the user is about to return
                // to the app after tapping on an ad.
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                super.onAdFailedToLoad(error)
                AppUtils().showSnackbarMsg("Banner failed to load.${error.message}")
            }
        }
    }

    /*--------------TO RESTORE THE SCREEN STATE ON RESUME---------------*/
    override fun onResume() {
        super.onResume()
        if (AdObject.isTimerInProgress)
            restartTimer()
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
    private fun loadPreviousFragment(){
        when (fragmentsStack.pop() as String) {
            StartScreen -> {
                loadStartScreen()
            }
            TOPICS -> {
                loadImageTopics()
            }
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
