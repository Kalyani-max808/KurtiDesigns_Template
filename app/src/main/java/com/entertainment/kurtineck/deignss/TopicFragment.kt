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

class TopicFragment : Fragment() {
    private lateinit var appInterfaces: AppInterfaces
    private lateinit var rootView: View
    private lateinit var rvTopicItems: RecyclerView
    private lateinit var tvTopicHeader: TextView

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
        rootView = inflater.inflate(R.layout.fragment_topics, container, false)

        // Initialize Views
        rvTopicItems = rootView.findViewById(R.id.rvTopicItems)
        tvTopicHeader = rootView.findViewById(R.id.tvTopicHeader)

        // Setup RecyclerView
        rvTopicItems.apply {
            layoutManager = GridLayoutManager(requireContext(), 1)
            adapter = TopicAdapter(requireContext(), requireActivity(), appInterfaces)
            setHasFixedSize(true)
            setItemViewCacheSize(10)

            // Ensure content scrolls behind navigation bars
            clipToPadding = false
        }

        // Setup edge-to-edge support
        setupEdgeToEdge()

        return rootView
    }

    private fun setupEdgeToEdge() {
        // Capture initial padding values to prevent accumulation
        val headerInitialTop = tvTopicHeader.paddingTop
        val headerInitialLeft = tvTopicHeader.paddingLeft
        val headerInitialRight = tvTopicHeader.paddingRight

        val rvInitialLeft = rvTopicItems.paddingLeft
        val rvInitialRight = rvTopicItems.paddingRight
        val rvInitialBottom = rvTopicItems.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            // ✅ 1. Header gets TOP padding (Status Bar)
            tvTopicHeader.updatePadding(
                top = headerInitialTop + systemBars.top,
                left = headerInitialLeft + systemBars.left,
                right = headerInitialRight + systemBars.right
            )

            // ✅ 2. RecyclerView gets BOTTOM padding (Navigation Bar)
            rvTopicItems.updatePadding(
                left = rvInitialLeft + systemBars.left,
                right = rvInitialRight + systemBars.right,
                bottom = rvInitialBottom + systemBars.bottom
            )

            windowInsets
        }
    }
}
