package com.mohammedtahriyne.screenrecorder

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.mohammedtahriyne.screenrecorder.databinding.FragmentToolsBinding

class ToolsFragment : Fragment() {

    private var _binding: FragmentToolsBinding? = null
    private val binding get() = _binding!!
    private var lastSelectedTool = "trim"

    private val videoPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { openToolForVideo(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentToolsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.cardTrim.setOnClickListener { lastSelectedTool = "trim"; videoPicker.launch("video/*") }
        binding.cardThumbnail.setOnClickListener { lastSelectedTool = "thumbnail"; videoPicker.launch("video/*") }
        binding.cardSpeed.setOnClickListener { lastSelectedTool = "speed"; videoPicker.launch("video/*") }
        binding.cardGif.setOnClickListener { lastSelectedTool = "gif"; videoPicker.launch("video/*") }
        binding.cardAbout.setOnClickListener {
            startActivity(Intent(requireContext(), AboutActivity::class.java))
        }
    }

    private fun openToolForVideo(uri: android.net.Uri) {
        val ctx = context ?: return
        val clazz = when (lastSelectedTool) {
            "thumbnail" -> ThumbnailMakerActivity::class.java
            "speed" -> SpeedControlActivity::class.java
            "gif" -> GifConverterActivity::class.java
            else -> VideoTrimmerActivity::class.java
        }
        val intent = Intent(ctx, clazz).apply {
            data = uri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
