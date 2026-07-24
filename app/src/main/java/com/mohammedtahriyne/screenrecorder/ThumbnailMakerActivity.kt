package com.mohammedtahriyne.screenrecorder

import android.content.ContentValues
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mohammedtahriyne.screenrecorder.databinding.ActivityThumbnailMakerBinding

class ThumbnailMakerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityThumbnailMakerBinding
    private var videoUri: Uri? = null
    private var selectedBitmap: Bitmap? = null
    private val frames = mutableListOf<Bitmap>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityThumbnailMakerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyVerticalInsets()

        videoUri = intent.data
        if (videoUri == null) {
            Toast.makeText(this, "No video provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.rvFrames.layoutManager = GridLayoutManager(this, 5, GridLayoutManager.HORIZONTAL, false)
        binding.rvFrames.adapter = FrameAdapter(frames) { bitmap ->
            selectedBitmap = bitmap
            binding.selectedFrame.setImageBitmap(bitmap)
        }

        extractFrames()

        binding.btnSave.setOnClickListener {
            saveThumbnail()
        }
    }

    private fun extractFrames() {
        val uri = videoUri ?: return
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(this, uri)
            val duration = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L

            val intervalMs = 1000L
            val frameCount = (duration / intervalMs).coerceAtMost(30)

            for (i in 0 until frameCount) {
                val timeUs = i * intervalMs * 1000
                val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (bitmap != null) {
                    val scaled = Bitmap.createScaledBitmap(bitmap, 200, 200 * bitmap.height / bitmap.width, true)
                    if (scaled != bitmap) bitmap.recycle()
                    frames.add(scaled)
                }
            }

            if (frames.isNotEmpty()) {
                selectedBitmap = frames[0]
                binding.selectedFrame.setImageBitmap(frames[0])
                binding.rvFrames.adapter?.notifyDataSetChanged()
            }

            binding.loadingView.visibility = View.GONE
        } catch (e: Exception) {
            e.printStackTrace()
            binding.loadingView.visibility = View.GONE
            Toast.makeText(this, getString(R.string.thumbnail_failed), Toast.LENGTH_SHORT).show()
        } finally {
            retriever.release()
        }
    }

    private fun saveThumbnail() {
        val bitmap = selectedBitmap ?: return
        val timestamp = System.currentTimeMillis()
        val fileName = "Thumbnail_$timestamp.jpg"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val cv = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/ScreenRecorder")
                }
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { os ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, os)
                    }
                }
            } else {
                val dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                val dir = java.io.File(dcim, "ScreenRecorder").apply { if (!exists()) mkdirs() }
                val file = java.io.File(dir, fileName)
                file.outputStream().use { os ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, os)
                }
            }
            Toast.makeText(this, getString(R.string.thumbnail_saved), Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, getString(R.string.thumbnail_failed), Toast.LENGTH_SHORT).show()
        }
    }

    inner class FrameAdapter(
        private val items: List<Bitmap>,
        private val onClick: (Bitmap) -> Unit
    ) : RecyclerView.Adapter<FrameAdapter.VH>() {
        inner class VH(val iv: ImageView) : RecyclerView.ViewHolder(iv)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val iv = ImageView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(180, 180)
                scaleType = ImageView.ScaleType.CENTER_CROP
                setPadding(4, 4, 4, 4)
            }
            return VH(iv)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.iv.setImageBitmap(items[position])
            holder.iv.setOnClickListener { onClick(items[position]) }
        }

        override fun getItemCount() = items.size
    }
}
