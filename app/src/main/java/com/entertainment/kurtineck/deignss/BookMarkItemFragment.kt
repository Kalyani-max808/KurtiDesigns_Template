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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.viewpager.widget.ViewPager
import com.entertainment.kurtineck.deignss.AdObject.AD_DISPLAY_SCROLL_COUNT
import com.entertainment.kurtineck.deignss.AdObject.admob
import java.io.FileNotFoundException
import java.io.IOException

class BookMarkItemFragment : Fragment() {
    private var bookMarkItemLayout: View? = null
    private var imgBitmap: Bitmap? = null
    private var mImages: ArrayList<String>? = null
    private var bookmarkItems: ArrayList<String>

    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var vpBookMarkImage: ViewPager
    private var vpAdapter: androidx.viewpager.widget.PagerAdapter? = null
    private var mAdCount = 0
    private lateinit var appInterfaces: AppInterfaces
    private lateinit var mLayoutInflater: LayoutInflater

    init {
        bookmarkItems = ItemDataset.mDbHelper?.getAllBookMarks() ?: arrayListOf()
        mImages = ItemDataset.mDbHelper?.getAllBookMarks() ?: arrayListOf()
        vpAdapter = mImages?.let { BookMarkImagePagerAdapter(it) }
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
        setupToolbarActions(bookMarkItemLayout!!)
        loadViewPager()
        setupToolbarTitle()
        setupViewPagerListener()

        // Setup menu (modern way)
        setupMenu()

        // Setup edge-to-edge support
        setupEdgeToEdge()

        return bookMarkItemLayout
    }

    private fun layoutInit(inflater: LayoutInflater, container: ViewGroup?) {
        mLayoutInflater =
            requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        bookMarkItemLayout = inflater.inflate(R.layout.fragment_book_mark_item, container, false)
        vpBookMarkImage = bookMarkItemLayout?.findViewById(R.id.vpBookMarkImage)!!
    }

    private fun setupToolbarActions(bookMarkItemLayout: View) {
        val act = activity as AppCompatActivity
        toolbar = bookMarkItemLayout.findViewById(R.id.toolbar_bookmarks_item)
        act.setSupportActionBar(toolbar)
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_bookmarks, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                val imgSelected = getSelectedItem()
                loadBitmapFromAssets(imgSelected)
                return when (menuItem.itemId) {
                    R.id.btnWallpaper -> {
                        setupWallpaper()
                        true
                    }
                    R.id.btnShare -> {
                        shareImage()
                        true
                    }
                    R.id.btnDelete -> {
                        deleteBookMarkedImage(imgSelected)
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
        bookMarkItemLayout?.let { view ->
            ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
                val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

                // Apply padding to toolbar to avoid status bar overlap
                toolbar.updatePadding(
                    top = systemBars.top,
                    left = systemBars.left,
                    right = systemBars.right
                )

                windowInsets
            }
        }
    }

    private fun loadViewPager() {
        vpBookMarkImage.adapter = vpAdapter
        vpBookMarkImage.currentItem = ItemDataset.position_bookmark
    }

    private fun setupToolbarTitle() {
        toolbar.title = "${vpBookMarkImage.currentItem + 1}/${bookmarkItems.size}"
    }

    private fun setupViewPagerListener() {
        vpBookMarkImage.addOnPageChangeListener(object :
            ViewPager.SimpleOnPageChangeListener() {

            override fun onPageSelected(position: Int) {
                loadNextItemWithAds(position)
            }

            override fun onPageScrollStateChanged(state: Int) {
                setupToolbarTitle()
            }
        })
    }

    private fun loadNextItemWithAds(position: Int) {
        mAdCount++
        if (mAdCount % AD_DISPLAY_SCROLL_COUNT == 0) admob?.loadNextScreen { }
        ItemDataset.position_bookmark = position
    }

    private fun loadBitmapFromAssets(imgSelected: String) {
        try {
            imgBitmap = BitmapFactory.decodeStream(requireActivity().assets.open(imgSelected))
        } catch (ex: FileNotFoundException) {
            admob?.loadNextScreen { appInterfaces.loadItem() }
        }
    }

    private fun setupWallpaper() {
        val myWallpaperManager = WallpaperManager.getInstance(context)
        try {
            myWallpaperManager.clear()
            imgBitmap?.let { myWallpaperManager.setBitmap(it) }
            Toast.makeText(
                requireContext(),
                "Wallpaper Set Successfully!!",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: IOException) {
            Toast.makeText(
                requireContext(),
                "Setting WallPaper Failed!!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun shareImage() {
        imgBitmap?.let {
            AppUtils().shareImage(it, requireContext())
        }
    }

    private fun deleteBookMarkedImage(imgSelected: String) {
        val status = ItemDataset.mDbHelper?.deleteBookMark(imgSelected)
        deleteImage(status, imgSelected)
        showImageDeletedMessage(imgSelected)
        loadBookmarkItems()
        if (isBookmarkListNotEmpty()) {
            vpBookMarkImage.adapter?.notifyDataSetChanged()
            setupToolbarTitle()
        } else {
            showItemScreen()
        }
    }

    private fun loadBookMarkListScreen() {
        admob?.loadNextScreen {
            appInterfaces.loadBookMarkMenu()
        }
    }

    private fun isBookmarkListNotEmpty() = mImages?.isNotEmpty() ?: false

    private fun showItemScreen() {
        AdObject.fragmentsStack.pop() // Remove BookMarkItemFragment
        AdObject.fragmentsStack.pop() // Remove BookMarkFragment
        appInterfaces.loadItem()
    }

    private fun loadBookmarkItems() {
        bookmarkItems = ItemDataset.mDbHelper?.getAllBookMarks() ?: arrayListOf()
    }

    private fun deleteImage(status: Boolean?, imgSelected: String) {
        if (status == true) {
            mImages?.remove(imgSelected)
        }
    }

    private fun showImageDeletedMessage(imgSelected: String) {
        AppUtils().showSneakerMsg(requireActivity(), "Deleted $imgSelected Successfully.")
    }

    private fun getSelectedItem(): String {
        var item = ""
        try {
            item = bookmarkItems.elementAt(vpBookMarkImage.currentItem)
        } catch (ex: Exception) {
            ex.printStackTrace()
            admob?.loadNextScreen { appInterfaces.loadItem() }
        }
        return item
    }

    private inner class BookMarkImagePagerAdapter(mImages: Collection<String>) :
        androidx.viewpager.widget.PagerAdapter() {

        override fun getItemPosition(`object`: Any): Int {
            return POSITION_NONE
        }

        override fun getCount(): Int {
            return mImages?.size ?: 0
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view === `object` as LinearLayout
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val itemView = mLayoutInflater.inflate(R.layout.pager_item, container, false)
            val iconPath = AppUtils().getItemImagePath(mImages, position)
            loadImageIntoView(itemView, iconPath.toString(), container)
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

    private fun getIconPath(position: Int) =
        ItemDataset.ASSET_URI + (mImages?.elementAt(position) ?: 0)
}
