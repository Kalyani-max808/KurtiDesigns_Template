package com.entertainment.kurtineck.deignss

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MenuFragment : Fragment() {
    private lateinit var appInterfaces: AppInterfaces
    private lateinit var rootView: View
    private lateinit var tvMenuHeader: TextView
    private lateinit var rvMenuItems: RecyclerView

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
        // Inflate the layout
        rootView = inflater.inflate(R.layout.fragment_menu, container, false)

        // Initialize views
        tvMenuHeader = rootView.findViewById(R.id.tvMenuHeader)
        rvMenuItems = rootView.findViewById(R.id.rvMenuItems)

        // Setup header text
        tvMenuHeader.text = ItemDataset.item_current?.topic_title ?: ""

        // Setup RecyclerView
        rvMenuItems.apply {
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = MenuAdapter(requireContext(), requireActivity(), appInterfaces)
            setItemViewCacheSize(100)

            // Ensure content scrolls behind navigation bars
            clipToPadding = false
        }

        // ✅ Setup edge-to-edge support
        setupEdgeToEdge()

        return rootView
    }

    private fun setupEdgeToEdge() {
        // Capture initial padding values to prevent accumulation
        val headerInitialTop = tvMenuHeader.paddingTop
        val headerInitialLeft = tvMenuHeader.paddingLeft
        val headerInitialRight = tvMenuHeader.paddingRight

        val rvInitialLeft = rvMenuItems.paddingLeft
        val rvInitialRight = rvMenuItems.paddingRight
        val rvInitialBottom = rvMenuItems.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Apply padding to header (top, left, right) using initial values
            tvMenuHeader.updatePadding(
                top = headerInitialTop + systemBars.top,
                left = headerInitialLeft + systemBars.left,
                right = headerInitialRight + systemBars.right
            )

            // Apply padding to RecyclerView (left, right, bottom) using initial values
            rvMenuItems.updatePadding(
                left = rvInitialLeft + systemBars.left,
                right = rvInitialRight + systemBars.right,
                bottom = rvInitialBottom + systemBars.bottom
            )

            windowInsets
        }
    }
}
