package com.mohammedtahriyne.screenrecorder

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mohammedtahriyne.screenrecorder.databinding.ActivitySpeedControlBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

class SpeedControlActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySpeedControlBinding
    private var videoUri: Uri? = null
    private var durationMs: Long = 0L
    private var speed: Float = 1.0f
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySpeedControlBinding.inflate(layoutInflater)
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
            mediaPlayer = mp
            durationMs = mp.duration.toLong()
            binding.loadingView.visibility = android.view.View.GONE
            mp.isLooping = true
            mp.start()
        }

        binding.sliderSpeed.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                speed = value
                binding.tvSpeedValue.text = "${value}x"
                applySpeed()
            }
        }

        binding.btnSpeed025.setOnClickListener { setSpeed(0.25f) }
        binding.btnSpeed05.setOnClickListener { setSpeed(0.5f) }
        binding.btnSpeed1.setOnClickListener { setSpeed(1.0f) }
        binding.btnSpeed2.setOnClickListener { setSpeed(2.0f) }

        binding.btnSave.setOnClickListener { processVideo() }
    }

    private fun setSpeed(s: Float) {
        speed = s
        binding.sliderSpeed.value = s
        binding.tvSpeedValue.text = "${s}x"
        applySpeed()
    }

    private fun applySpeed() {
        try {
            val mp = mediaPlayer ?: return
            val params = PlaybackParams()
            params.speed = speed
            mp.playbackParams = params
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun processVideo() {
        val uri = videoUri ?: return
        binding.btnSave.isEnabled = false
        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                try { doProcess(uri) } catch (e: Exception) { e.printStackTrace(); false }
            }
            binding.btnSave.isEnabled = true
            if (success) {
                Toast.makeText(this@SpeedControlActivity, getString(R.string.speed_saved), Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this@SpeedControlActivity, getString(R.string.speed_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun doProcess(inputUri: Uri): Boolean {
        val extractor = MediaExtractor()
        extractor.setDataSource(this, inputUri, null)

        val trackCount = extractor.trackCount
        val timestamp = System.currentTimeMillis()
        val fileName = "Speed_${speed}x_$timestamp.mp4"

        var outputUri: android.net.Uri? = null
        var muxer: MediaMuxer? = null

        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val cv = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/ScreenRecorder")
            }
            outputUri = contentResolver.insert(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cv)
        } else {
            val dcim = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DCIM)
            val dir = java.io.File(dcim, "ScreenRecorder").apply { if (!exists()) mkdirs() }
            val path = java.io.File(dir, fileName).absolutePath
            muxer = MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        }

        if (outputUri != null && muxer == null) {
            val pfd = contentResolver.openFileDescriptor(outputUri, "rw") ?: return false
            muxer = MediaMuxer(pfd.fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        }

        val rotation = try { extractor.getTrackFormat(0).getInteger(MediaFormat.KEY_ROTATION) } catch (_: Exception) { 0 }
        muxer?.setOrientationHint(rotation)

        val indexMap = HashMap<Int, Int>()
        for (i in 0 until trackCount) {
            indexMap[i] = muxer!!.addTrack(extractor.getTrackFormat(i))
        }

        val bufferInfo = MediaCodec.BufferInfo()
        val buffer = ByteBuffer.allocate(1024 * 1024)

        extractor.selectTrack(0)
        extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

        var started = false
        while (true) {
            bufferInfo.offset = 0
            bufferInfo.size = extractor.readSampleData(buffer, 0)
            if (bufferInfo.size < 0) break

            var pts = extractor.sampleTime
            pts = (pts / speed).toLong()

            bufferInfo.presentationTimeUs = pts
            bufferInfo.flags = extractor.sampleFlags

            if (!started) {
                muxer?.start()
                started = true
            }
            muxer?.writeSampleData(indexMap[extractor.sampleTrackIndex] ?: 0, buffer, bufferInfo)
            extractor.advance()
        }

        if (started) muxer?.stop()
        muxer?.release()
        extractor.release()
        return true
    }
}
