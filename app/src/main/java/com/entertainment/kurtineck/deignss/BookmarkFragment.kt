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

class BookmarkFragment : Fragment() {
    private lateinit var appInterfaces: AppInterfaces
    private lateinit var rootView: View
    private lateinit var tvBookMarkTitle: TextView
    private lateinit var rvBookMarks: RecyclerView

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is AppInterfaces) {
            appInterfaces = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            // Handle arguments if needed
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout
        rootView = inflater.inflate(R.layout.fragment_bookmark, container, false)

        // Initialize views
        tvBookMarkTitle = rootView.findViewById(R.id.tvBookMarkTitle)
        rvBookMarks = rootView.findViewById(R.id.rvBookMarks)

        // Setup header text
        tvBookMarkTitle.text = "BookMarked Items"

        // Setup RecyclerView
        rvBookMarks.apply {
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = BookMarkAdapter(requireContext(), requireActivity(), appInterfaces)
        }

        // ✅ Setup edge-to-edge support
        setupEdgeToEdge()

        return rootView
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Apply padding to header (top, left, right)
            tvBookMarkTitle.updatePadding(
                top = tvBookMarkTitle.paddingTop + systemBars.top,
                left = systemBars.left,
                right = systemBars.right
            )

            // Apply padding to RecyclerView (left, right, bottom)
            rvBookMarks.updatePadding(
                left = systemBars.left,
                right = systemBars.right,
                bottom = systemBars.bottom
            )

            windowInsets
        }
    }
}
