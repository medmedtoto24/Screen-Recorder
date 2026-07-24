package com.mohammedtahriyne.screenrecorder

import android.content.ContentValues
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mohammedtahriyne.screenrecorder.databinding.ActivityVideoTrimmerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

class VideoTrimmerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoTrimmerBinding
    private var videoUri: Uri? = null
    private var durationMs: Long = 0L
    private var startMs: Long = 0L
    private var endMs: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoTrimmerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyVerticalInsets()

        videoUri = intent.data
        if (videoUri == null) {
            Toast.makeText(this, "No video provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.videoView.setVideoURI(videoUri)
        binding.videoView.setOnPreparedListener { mp ->
            durationMs = mp.duration.toLong()
            endMs = durationMs
            binding.seekBar.max = durationMs.toInt()
            binding.seekBar.progress = 0
            updateLabels()
            binding.loadingView.visibility = View.GONE
            mp.isLooping = true
            mp.start()
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    startMs = progress.toLong()
                    binding.videoView.seekTo(progress)
                    updateLabels()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        binding.btnTrim.setOnClickListener {
            trimVideo()
        }
    }

    private fun updateLabels() {
        binding.tvStartTime.text = formatTime(startMs)
        binding.tvEndTime.text = formatTime(durationMs)
    }

    private fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format("%02d:%02d", min, sec)
    }

    private fun trimVideo() {
        val uri = videoUri ?: return
        if (startMs >= endMs) {
            Toast.makeText(this, getString(R.string.trimmer_failed), Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnTrim.isEnabled = false
        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    doTrim(uri)
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
            binding.btnTrim.isEnabled = true
            if (success) {
                Toast.makeText(this@VideoTrimmerActivity, getString(R.string.trimmer_saved), Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this@VideoTrimmerActivity, getString(R.string.trimmer_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun doTrim(inputUri: Uri): Boolean {
        val extractor = MediaExtractor()
        extractor.setDataSource(this, inputUri, null)

        val trackCount = extractor.trackCount
        val muxerFormat = extractor.getTrackFormat(0)
        val mime = muxerFormat.getString(MediaFormat.KEY_MIME) ?: return false

        val timestamp = System.currentTimeMillis()
        val fileName = "Trimmed_$timestamp.mp4"

        var outputUri: Uri? = null
        var outputPath: String? = null
        var muxer: MediaMuxer? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val cv = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/ScreenRecorder")
            }
            outputUri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cv)
        } else {
            val dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            val dir = File(dcim, "ScreenRecorder").apply { if (!exists()) mkdirs() }
            outputPath = File(dir, fileName).absolutePath
        }

        muxer = if (outputUri != null) {
            val pfd = contentResolver.openFileDescriptor(outputUri, "rw") ?: return false
            MediaMuxer(pfd.fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } else {
            MediaMuxer(outputPath!!, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        }

        val rotation = try { extractor.getTrackFormat(0).getInteger(MediaFormat.KEY_ROTATION) } catch (_: Exception) { 90 }
        muxer.setOrientationHint(rotation)

        val bufferInfo = MediaCodec.BufferInfo()
        val maxBufSize = 1024 * 1024
        val buffer = ByteBuffer.allocate(maxBufSize)

        val indexMap = HashMap<Int, Int>()
        for (i in 0 until trackCount) {
            indexMap[i] = muxer.addTrack(extractor.getTrackFormat(i))
        }

        extractor.selectTrack(0)
        extractor.seekTo(startMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

        var started = false
        while (true) {
            bufferInfo.offset = 0
            bufferInfo.size = extractor.readSampleData(buffer, 0)
            if (bufferInfo.size < 0) break

            bufferInfo.presentationTimeUs = extractor.sampleTime
            if (bufferInfo.presentationTimeUs > endMs * 1000) break
            if (bufferInfo.presentationTimeUs >= startMs * 1000) {
                bufferInfo.trackIndex = indexMap[extractor.sampleTrackIndex] ?: 0
                if (!started) {
                    muxer.start()
                    started = true
                }
                muxer.writeSampleData(bufferInfo.trackIndex, buffer, bufferInfo)
            }
            extractor.advance()
        }

        if (started) muxer.stop()
        muxer.release()
        extractor.release()

        return true
    }
}
