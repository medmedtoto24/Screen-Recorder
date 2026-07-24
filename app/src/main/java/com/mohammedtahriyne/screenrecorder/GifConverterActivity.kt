package com.mohammedtahriyne.screenrecorder

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mohammedtahriyne.screenrecorder.databinding.ActivityGifConverterBinding

class GifConverterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGifConverterBinding
    private var videoUri: Uri? = null
    private var durationMs: Long = 0L
    private var durationSeconds: Int = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGifConverterBinding.inflate(layoutInflater)
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
            binding.loadingView.visibility = android.view.View.GONE
            mp.isLooping = true
            mp.start()
        }

        binding.sliderDuration.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                durationSeconds = value.toInt()
                binding.tvDurationValue.text = "${durationSeconds}s"
            }
        }

        binding.btnSave.setOnClickListener { processVideo() }
    }

    private fun processVideo() {
        val uri = videoUri ?: return
        binding.btnSave.isEnabled = false

        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(this, uri)

            val frames = mutableListOf<Bitmap>()
            val intervalMs = 100L
            val totalFrames = ((durationSeconds * 1000L) / intervalMs).toInt().coerceAtMost(50)

            for (i in 0 until totalFrames) {
                val timeUs = i * intervalMs * 1000
                val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (bitmap != null) {
                    val scaled = Bitmap.createScaledBitmap(bitmap, 320, 320 * bitmap.height / bitmap.width, true)
                    if (scaled != bitmap) bitmap.recycle()
                    frames.add(scaled)
                }
            }
            retriever.release()

            if (frames.isNotEmpty()) {
                val timestamp = System.currentTimeMillis()
                val fileName = "Gif_$timestamp.gif"
                val file = java.io.File(cacheDir, fileName)

                val os = java.io.BufferedOutputStream(file.outputStream())
                writeGif(os, frames)
                os.close()

                frames.forEach { it.recycle() }

                android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.fromFile(file), "image/gif")
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }.also { startActivity(it) }

                Toast.makeText(this, getString(R.string.gif_saved), Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, getString(R.string.gif_failed), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, getString(R.string.gif_failed), Toast.LENGTH_SHORT).show()
        }

        binding.btnSave.isEnabled = true
    }

    private fun writeGif(os: java.io.BufferedOutputStream, frames: List<Bitmap>) {
        if (frames.isEmpty()) return
        val w = frames[0].width
        val h = frames[0].height

        os.write("GIF89a".toByteArray())

        os.write(byteArrayOf(
            (w and 0xFF).toByte(), ((w shr 8) and 0xFF).toByte(),
            (h and 0xFF).toByte(), ((h shr 8) and 0xFF).toByte()
        ))

        os.write(0xF7)
        os.write(0)
        os.write(0)

        val colorTable = ByteArray(256 * 3)
        for (i in 0 until 256) {
            val idx = i * 3
            colorTable[idx] = ((i shr 5) and 7) * 36
            colorTable[idx + 1] = ((i shr 2) and 7) * 36
            colorTable[idx + 2] = (i and 3) * 85
        }
        os.write(colorTable)

        for (frame in frames) {
            os.write(0x2C)
            os.write(byteArrayOf(0, 0, 0, 0))
            os.write(byteArrayOf((w and 0xFF).toByte(), ((w shr 8) and 0xFF).toByte()))
            os.write(byteArrayOf((h and 0xFF).toByte(), ((h shr 8) and 0xFF).toByte()))
            os.write(0x00)

            os.write(8)

            val pixels = IntArray(w * h)
            frame.getPixels(pixels, 0, w, 0, 0, w, h)
            val indexed = ByteArray(w * h)
            for (i in pixels.indices) {
                indexed[i] = (((pixels[i] shr 16) and 0xFF) / 32 shl 5 or (((pixels[i] shr 8) and 0xFF) / 32 shl 2) or ((pixels[i] and 0xFF) / 85)).toByte()
            }

            var offset = 0
            while (offset < indexed.size) {
                val chunk = indexed.sliceArray(offset until minOf(offset + 255, indexed.size))
                os.write(chunk.size)
                os.write(chunk)
                offset += 255
            }

            os.write(0x00)
        }

        os.write(0x3B)
    }
}
