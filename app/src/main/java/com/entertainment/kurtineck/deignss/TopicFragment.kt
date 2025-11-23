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
    private lateinit var rvJokeHeader: RecyclerView
    private lateinit var tvHeader: TextView
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

        // Initialize RecyclerView
        rvJokeHeader = rootView.findViewById(R.id.rvJokeHeader)

        // Initialize TextView
        tvHeader = rootView.findViewById(R.id.tvHeader)


        // Setup RecyclerView
        rvJokeHeader.apply {
            layoutManager = GridLayoutManager(requireContext(), 1)
            adapter = TopicAdapter(requireContext(), requireActivity(), appInterfaces)
            setHasFixedSize(true)
            setItemViewCacheSize(10)
        }

        // Setup edge-to-edge support
        setupEdgeToEdge()

        return rootView
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            // ✅ 1. Header gets TOP padding (Status Bar)
            tvHeader.updatePadding(
                top = tvHeader.paddingTop + systemBars.top,
                left = systemBars.left,
                right = systemBars.right
            )

            // ✅ 2. RecyclerView gets BOTTOM padding (Navigation Bar)
            rvJokeHeader.updatePadding(
                left = systemBars.left,
                right = systemBars.right,
                bottom = systemBars.bottom
            )

            windowInsets
        }
    }
}
