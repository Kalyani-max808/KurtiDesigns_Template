package com.entertainment.kurtineck.deignss

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.*
import android.view.Menu
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.viewpager.widget.ViewPager
import com.google.android.material.snackbar.Snackbar
import com.entertainment.kurtineck.deignss.AdObject.AD_DISPLAY_SCROLL_COUNT
import com.entertainment.kurtineck.deignss.AdObject.admob
import java.io.IOException

class ItemFragment02 : Fragment() {
    private var itemLayout: View? = null
    private var imgBitmap: Bitmap? = null
    private var mImages: ArrayList<String>? = null

    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var vpItemImage: ViewPager
    private var vpAdapter: androidx.viewpager.widget.PagerAdapter? = null
    private var mAdCount = 0
    private lateinit var appInterfaces: AppInterfaces
    private lateinit var mLayoutInflater: LayoutInflater

    init {
        mImages = getCurrentItemMenus()
        vpAdapter = ImagePagerAdapter()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is AppInterfaces) {
            appInterfaces = context
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        layoutInit(inflater, container)
        setupToolbarActions()
        loadViewPager()
        setupToolbarTitle()
        setupViewPagerListener()

        // Setup menu (modern way)
        setupMenu()

        // Setup edge-to-edge support
        setupEdgeToEdge()

        return itemLayout
    }

    private fun setupToolbarActions() {
        val act = activity as AppCompatActivity
        itemLayout?.let {
            toolbar = it.findViewById(R.id.my_toolbar)
            act.setSupportActionBar(toolbar)
        }
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                imgBitmap = getBitmapImage()
                return when (menuItem.itemId) {
                    R.id.btnWallpaper -> {
                        setupWallpaper()
                        true
                    }
                    R.id.btnShare -> {
                        shareImage()
                        true
                    }
                    R.id.btnBookmark -> {
                        ItemDataset.mDbHelper?.addBookMark(imgName = getCurrentItemFromDataset())
                        Snackbar.make(
                            requireActivity().findViewById(R.id.clMainActivity),
                            "Image ${getCurrentItemFromDataset()} saved successfully.",
                            Snackbar.LENGTH_SHORT
                        ).show()
                        true
                    }
                    R.id.btnBookMarksList -> {
                        loadBookMarkListScreen()
                        true
                    }
                    R.id.btnRate -> {
                        AppUtils().rateApp(requireContext())
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupEdgeToEdge() {
        itemLayout?.let { view ->
            ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
                val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

                toolbar.updatePadding(
                    top = systemBars.top,
                    left = systemBars.left,
                    right = systemBars.right
                )

                windowInsets
            }
        }
    }

    private fun layoutInit(inflater: LayoutInflater, container: ViewGroup?) {
        mLayoutInflater =
            requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        itemLayout = inflater.inflate(R.layout.fragment_item, container, false)
        vpItemImage = itemLayout?.findViewById(R.id.vpItemImage)!!
    }

    private fun loadViewPager() {
        vpItemImage.adapter = vpAdapter
        vpItemImage.currentItem = ItemDataset.position
    }

    private fun setupToolbarTitle() {
        ItemDataset.position_bookmark = getCurrentItemFromVP()
        toolbar.title = "${getItemPositionFromView() + 1}/${getCurrentItemPosition()}"
    }

    private fun getItemPositionFromView(): Int {
        return vpItemImage.currentItem
    }

    private fun getCurrentItemPosition(): Int {
        return getCurrentItemMenus().size
    }

    private fun getCurrentItemMenus(): ArrayList<String> {
        return ItemDataset.item_current?.menus ?: arrayListOf()
    }

    private fun setupViewPagerListener() {
        vpItemImage.addOnPageChangeListener(object :
            ViewPager.SimpleOnPageChangeListener() {

            override fun onPageSelected(position: Int) {
                // loadNextItemWithAds(position)
            }

            override fun onPageScrollStateChanged(state: Int) {
                setupToolbarTitle()
            }
        })
    }

    private fun loadNextItemWithAds(position: Int) {
        mAdCount++
        if (mAdCount % AD_DISPLAY_SCROLL_COUNT == 0) admob?.loadNextScreen { }
    }

    private fun getBitmapImage(): Bitmap {
        var img: Bitmap? = null
        try {
            val uri = getCurrentItemFromDataset()
            if (uri.isNotBlank()) {
                img = BitmapFactory.decodeStream(activity?.assets?.open(getCurrentItemFromDataset()))
            }
        } catch (e: Exception) {
            img = getDummyImage()
        }
        return img ?: getDummyImage()
    }

    private fun setupWallpaper() {
        val myWallpaperManager = WallpaperManager.getInstance(context)
        try {
            myWallpaperManager.clear()
            imgBitmap?.let { myWallpaperManager.setBitmap(it) }
            activity?.let { AppUtils().showSneakerMsg(it, "Wallpaper Set Successfully.") }
        } catch (e: IOException) {
            activity?.let { AppUtils().showSneakerMsg(it, "Unable to set Wallpaper!") }
        }
    }

    private fun shareImage() {
        imgBitmap?.let { AppUtils().shareImage(it, requireContext()) }
    }

    private fun loadBookMarkListScreen() {
        admob?.loadNextScreen {
            appInterfaces.loadBookMarkMenu()
        }
    }

    private fun getDummyImage(): Bitmap {
        return BitmapFactory.decodeResource(requireContext().resources, R.drawable.gallery)
    }

    private fun getCurrentItemFromDataset(): String {
        return getCurrentItemMenus().getOrNull(getCurrentItemFromVP()) ?: ""
    }

    private fun getCurrentItemFromVP(): Int {
        return vpItemImage.currentItem
    }

    private inner class ImagePagerAdapter : androidx.viewpager.widget.PagerAdapter() {
        private val mImages = getCurrentItemMenus()

        override fun getItemPosition(`object`: Any): Int {
            return POSITION_NONE
        }

        override fun getCount(): Int {
            return mImages.size
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view === `object` as LinearLayout
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val itemView = mLayoutInflater.inflate(R.layout.pager_item, container, false)
            val iconPath = getIconPath(position)
            loadImageIntoView(itemView, iconPath, container)
            return itemView
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as LinearLayout)
        }
    }

    private fun loadImageIntoView(
        itemView: View,
        iconPath: String,
        container: ViewGroup
    ) {
        val imgHolder = itemView.findViewById<ImageView>(R.id.suit_image)
        imgHolder.setImageURI(iconPath.toUri())
        AppUtils().setupPinchZoom(itemView, iconPath, container)
    }

    private fun getIconPath(position: Int): String {
        var iconPath = ""
        try {
            iconPath = ItemDataset.ASSET_URI + (mImages?.elementAt(position) ?: 0)
        } catch (ex: IndexOutOfBoundsException) {
            appInterfaces.loadMenus()
        }
        return iconPath
    }
}
