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

class StartScreenFragment : Fragment() {
    private lateinit var act: AppInterfaces
    private lateinit var startScreen: View
    private lateinit var tvStartHeader: TextView

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is AppInterfaces) {
            act = context
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout
        startScreen = inflater.inflate(R.layout.fragment_start_screen, container, false)

        // Initialize views
        val btnGallery03 = startScreen.findViewById<View>(R.id.btnGallery03)
        val btnGalleryLayout03 = startScreen.findViewById<View>(R.id.btnGalleryLayout03)
        val imgBtnRateMe03 = startScreen.findViewById<View>(R.id.imgBtnRateMe03)
        val imgBtnShare03 = startScreen.findViewById<View>(R.id.imgBtnShare03)
        val tvPrivacy03 = startScreen.findViewById<View>(R.id.tvPrivacy03)
        tvStartHeader = startScreen.findViewById<TextView>(R.id.tvStartHeader)
        // Setup click listeners
        btnGalleryLayout03.setOnClickListener {
            AdObject.admob?.loadNextScreen { act.loadImageTopics() }
        }

        btnGallery03.setOnClickListener {
            AdObject.admob?.loadNextScreen { act.loadImageTopics() }
        }

        imgBtnRateMe03.setOnClickListener {
            AppUtils().rateApp(requireContext())
        }

        imgBtnShare03.setOnClickListener {
            AppUtils().shareApp(requireContext())
        }

        tvPrivacy03.setOnClickListener {
            AdObject.admob?.loadNextScreen { act.loadPrivacyPolicy() }
        }

        // Setup edge-to-edge support
        setupEdgeToEdge()

        return startScreen
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(startScreen) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            tvStartHeader.updatePadding(
                top = tvStartHeader.paddingTop + systemBars.top,
                left = systemBars.left,
                right = systemBars.right
            )

            windowInsets
        }
    }
}
