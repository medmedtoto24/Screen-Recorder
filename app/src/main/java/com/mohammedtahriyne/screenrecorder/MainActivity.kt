package com.mohammedtahriyne.screenrecorder

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.mohammedtahriyne.screenrecorder.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_PERMISSIONS_CODE = 2001
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var configManager: ConfigManager

    private val homeFragment = HomeFragment()
    private val toolsFragment = ToolsFragment()
    private val settingsFragment = SettingsFragment()
    private var activeFragment: Fragment = homeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configManager = ConfigManager(this)

        LocaleHelper.applyLanguage(configManager.appLanguage)

        setSupportActionBar(binding.toolbar)
        setupBottomNavigation()
        setupFragments()
        performFullPermissionCheck()
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    switchFragment(homeFragment)
                    binding.toolbar.title = getString(R.string.app_name)
                    true
                }
                R.id.nav_tools -> {
                    switchFragment(toolsFragment)
                    binding.toolbar.title = getString(R.string.nav_tools)
                    true
                }
                R.id.nav_settings -> {
                    switchFragment(settingsFragment)
                    binding.toolbar.title = getString(R.string.nav_settings)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupFragments() {
        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, settingsFragment, "settings").hide(settingsFragment)
            .add(R.id.fragmentContainer, toolsFragment, "tools").hide(toolsFragment)
            .add(R.id.fragmentContainer, homeFragment, "home")
            .commit()
    }

    private fun switchFragment(target: Fragment) {
        if (target === activeFragment) return
        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .show(target)
            .commit()
        activeFragment = target
    }

    private fun performFullPermissionCheck() {
        if (!checkAndRequestRuntimePermissions()) return
        if (!checkOverlayPermission()) return
        if (!checkWriteSettingsPermission()) return
    }

    private fun checkAndRequestRuntimePermissions(): Boolean {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        return if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), REQUEST_PERMISSIONS_CODE)
            false
        } else {
            true
        }
    }

    private fun checkOverlayPermission(): Boolean {
        return if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            false
        } else true
    }

    private fun checkWriteSettingsPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName")))
            false
        } else true
    }

    override fun onResume() {
        super.onResume()
        configManager = ConfigManager(this)
    }
}
