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
    private lateinit var tvHeader: TextView
    private lateinit var rvMenuHeader: RecyclerView

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
        tvHeader = rootView.findViewById(R.id.tvHeader)
        rvMenuHeader = rootView.findViewById(R.id.rvMenuHeader)

        // Setup header text
        tvHeader.text = ItemDataset.item_current?.topic_title ?: ""

        // Setup RecyclerView
        rvMenuHeader.apply {
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = MenuAdapter(context, activity, appInterfaces)
            setItemViewCacheSize(100)
        }

        // ✅ Setup edge-to-edge support
        setupEdgeToEdge()

        return rootView
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Apply padding to header (top, left, right)
            tvHeader.updatePadding(
                top = tvHeader.paddingTop + systemBars.top,
                left = systemBars.left,
                right = systemBars.right
            )

            // Apply padding to RecyclerView (left, right, bottom)
            rvMenuHeader.updatePadding(
                left = systemBars.left,
                right = systemBars.right,
                bottom = systemBars.bottom
            )

            windowInsets
        }
    }
}
