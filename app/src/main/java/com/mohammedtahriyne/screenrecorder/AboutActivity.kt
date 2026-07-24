package com.mohammedtahriyne.screenrecorder

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.google.android.material.listitem.ListItemViewHolder
import com.mohammedtahriyne.screenrecorder.databinding.LayoutAboutItemBinding
import com.mohammedtahriyne.screenrecorder.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAboutBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applyTopInsets()
        binding.appBarLayout.applySystemBarInsets()
        binding.recyclerView.applyBottomInsets()
        binding.toolbar.setNavigationOnClickListener { finish() }
        setupRecycler()
    }
    private fun setupRecycler() {
        val items = listOf(
            AboutItem("Mohammed Tahriyne", getString(R.string.about_developer_label),
                "https://youtube.com/@mohammedtahriyne?si=MU2W_B_TobhQqGuN", Icon.Drawable(R.drawable.ic_info)),
            AboutItem("YouTube", getString(R.string.about_youtube_label),
                "https://youtube.com/@mohammedtahriyne?si=MU2W_B_TobhQqGuN", Icon.Drawable(R.drawable.ic_youtube)),
            AboutItem("Instagram", getString(R.string.about_instagram_label),
                "https://www.instagram.com/mohammed_tahriyne?igsh=MWpoNDkzYWVvcGZh", Icon.Drawable(R.drawable.ic_instagram)),
            AboutItem("Facebook", getString(R.string.about_facebook_label),
                "https://www.facebook.com/MohammedTahriyne2002", Icon.Drawable(R.drawable.ic_facebook)),
            AboutItem(getString(R.string.about_support_email_title), getString(R.string.about_support_email_subtitle),
                "mailto:mtahriyne20@gmail.com", Icon.Drawable(R.drawable.ic_info))
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = AboutAdapter(items)
    }
    private fun openUrl(url: String) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    data class AboutItem(val title: String, val subtitle: String, val url: String, val icon: Icon?)
    sealed class Icon {
        data class Drawable(val resId: Int) : Icon()
        data class Url(val value: String) : Icon()
        data class Asset(val path: String) : Icon()
    }
    inner class AboutAdapter(private val items: List<AboutItem>) : RecyclerView.Adapter<ListItemViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListItemViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_about_item, parent, false)
            return ListItemViewHolder(view)
        }
        override fun onBindViewHolder(holder: ListItemViewHolder, position: Int) {
            val item = items[position]
            val b = LayoutAboutItemBinding.bind(holder.itemView)
            b.title.text = item.title
            b.subtitle.text = item.subtitle
            loadIcon(b, item.icon)
            b.root.setOnClickListener { openUrl(item.url) }
        }
        private fun loadIcon(binding: LayoutAboutItemBinding, icon: Icon?) {
            when (icon) {
                is Icon.Drawable -> binding.avatar.setImageResource(icon.resId)
                is Icon.Url -> Glide.with(binding.root.context).load(icon.value).transform(CircleCrop()).into(binding.avatar)
                is Icon.Asset -> Glide.with(binding.root.context).load("file:///android_asset/${icon.path}").transform(CircleCrop()).into(binding.avatar)
                null -> binding.avatar.setImageDrawable(null)
            }
        }
        override fun getItemCount() = items.size
    }
}
