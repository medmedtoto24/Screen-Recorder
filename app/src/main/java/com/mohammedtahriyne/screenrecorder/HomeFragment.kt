package com.mohammedtahriyne.screenrecorder

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mohammedtahriyne.screenrecorder.databinding.FragmentHomeBinding
import kotlinx.coroutines.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: VideoAdapter
    private lateinit var configManager: ConfigManager

    private val recordingStateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ScreenRecordService.ACTION_STATE_CHANGED) {
                syncRecordingUi()
            }
        }
    }

    private val videoObserver = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            loadVideos()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configManager = ConfigManager(requireContext())
        setupRecyclerView()
        setupClickListeners()
        registerSystemObservers()
        syncRecordingUi()
    }

    override fun onResume() {
        super.onResume()
        syncRecordingUi()
        loadVideos()
    }

    private fun setupRecyclerView() {
        adapter = VideoAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        loadVideos()
    }

    private fun setupClickListeners() {
        binding.fabRecord.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            handleRecordAction()
        }
    }

    private fun handleRecordAction() {
        val activity = requireActivity() as? AppCompatActivity ?: return
        if (ScreenRecordService.isRecording) {
            requireContext().startService(Intent(requireContext(), ScreenRecordService::class.java).apply {
                action = ScreenRecordService.ACTION_STOP
            })
        } else {
            val intent = Intent(requireContext(), MediaProjectionPermissionActivity::class.java).apply {
                putExtra("RECORD_MIC", configManager.isMicEnabled)
                putExtra("RECORD_SYSTEM_AUDIO", configManager.isSystemAudioEnabled)
            }
            startActivity(intent)
        }
    }

    private fun syncRecordingUi() {
        if (!isAdded || _binding == null) return
        val isRecording = ScreenRecordService.isRecording
        binding.fabRecord.apply {
            setIconResource(if (isRecording) R.drawable.ic_stop else R.drawable.ic_screen_record)
            text = if (isRecording) getString(R.string.MainActivity_record_stop) else getString(R.string.MainActivity_record_start)
        }
    }

    @android.annotation.SuppressLint("Range")
    private fun loadVideos() {
        if (!isAdded) return
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val videos = mutableListOf<VideoFile>()
            val projection = arrayOf(
                android.provider.MediaStore.Video.Media._ID,
                android.provider.MediaStore.Video.Media.DISPLAY_NAME,
                android.provider.MediaStore.Video.Media.DURATION,
                android.provider.MediaStore.Video.Media.SIZE,
                android.provider.MediaStore.Video.Media.DATE_ADDED
            )

            val selection = "${android.provider.MediaStore.Video.Media.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("ScreenRecord_%.mp4")

            requireContext().contentResolver.query(
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection, selection, selectionArgs,
                "${android.provider.MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media._ID))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DISPLAY_NAME)) ?: ""
                    val duration = cursor.getLong(cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DURATION))
                    val size = cursor.getLong(cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.SIZE))
                    val dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DATE_ADDED))

                    if (size > 0L) {
                        val uri = Uri.withAppendedPath(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())
                        videos.add(VideoFile(id, uri, name, duration, size, dateAdded))
                    }
                }
            }

            withContext(Dispatchers.Main) {
                if (_binding != null) {
                    adapter.submitList(videos)
                    val isEmpty = videos.isEmpty()
                    binding.emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
                    binding.recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
                }
            }
        }
    }

    @android.annotation.SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerSystemObservers() {
        requireContext().contentResolver.registerContentObserver(
            android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, videoObserver
        )
        val filter = android.content.IntentFilter(ScreenRecordService.ACTION_STATE_CHANGED)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(recordingStateReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            requireContext().registerReceiver(recordingStateReceiver, filter)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try { requireContext().contentResolver.unregisterContentObserver(videoObserver) } catch (_: Exception) {}
        try { requireContext().unregisterReceiver(recordingStateReceiver) } catch (_: Exception) {}
        _binding = null
    }
}
