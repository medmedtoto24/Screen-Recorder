package com.mohammedtahriyne.screenrecorder

import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mohammedtahriyne.screenrecorder.databinding.LayoutSettingsSheetBinding

class SettingsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: LayoutSettingsSheetBinding? = null
    private val binding get() = _binding!!

    private var _configManager: ConfigManager? = null
    private val configManager get() = _configManager!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutSettingsSheetBinding.inflate(inflater, container, false)
        try {
            _configManager = ConfigManager(requireContext())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (_configManager == null) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requireActivity().window.isNavigationBarContrastEnforced = false
            }
        } catch (_: Exception) {}

        loadCurrentSettings()
        setupListeners()
        setupDynamicQualities()
        setupAppearanceSection()
        updateLanguageDisplay()
    }

    private fun updateLanguageDisplay() {
        val cm = _configManager ?: return
        val currentLang = cm.appLanguage
        val displayName = when (currentLang) {
            ConfigManager.LANG_SYSTEM -> "System Default"
            ConfigManager.LANG_ENGLISH -> "English"
            ConfigManager.LANG_ARABIC -> "العربية"
            ConfigManager.LANG_FRENCH -> "Français"
            ConfigManager.LANG_SPANISH -> "Español"
            else -> "System Default"
        }
        binding.tvLanguageValue.text = displayName
    }

    private fun showLanguageDialog() {
        val cm = _configManager ?: return
        val languages = LocaleHelper.supportedLanguages
        val labels = languages.map { it.second }.toTypedArray()
        val codes = languages.map { it.first }.toTypedArray()
        val currentIndex = codes.indexOf(cm.appLanguage).coerceAtLeast(0)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.settings_language_title))
            .setSingleChoiceItems(labels, currentIndex) { dialog, which ->
                cm.appLanguage = codes[which]
                updateLanguageDisplay()
                LocaleHelper.applyLanguage(codes[which])
                dialog.dismiss()
            }
            .setNegativeButton(R.string.VideoAdapter_btn_cancel, null)
            .show()
    }

    private fun loadCurrentSettings() {
        val cm = _configManager ?: return
        binding.switchMic.isChecked = cm.isMicEnabled
        binding.switchSystemAudio.isChecked = cm.isSystemAudioEnabled
        binding.switchShowTouches.isChecked = cm.showTouches
        binding.switchDynamicColors.isChecked = cm.isDynamicColorsEnabled

        val qualityButtonId = when (cm.videoQuality) {
            ConfigManager.QUALITY_MAX -> R.id.btnQualityMax
            ConfigManager.QUALITY_4K -> R.id.btnQuality4K
            ConfigManager.QUALITY_2K -> R.id.btnQuality2K
            ConfigManager.QUALITY_1080P -> R.id.btnQuality1080
            ConfigManager.QUALITY_720P -> R.id.btnQuality720
            ConfigManager.QUALITY_480P -> R.id.btnQuality480
            ConfigManager.QUALITY_360P -> R.id.btnQuality360
            ConfigManager.QUALITY_240P -> R.id.btnQuality240
            else -> R.id.btnQualityMax
        }
        binding.buttonGroup.check(qualityButtonId)

        val themeButtonId = when (cm.themeMode) {
            ConfigManager.THEME_LIGHT -> R.id.btnThemeLight
            ConfigManager.THEME_DARK -> R.id.btnThemeDark
            else -> R.id.btnThemeSystem
        }
        binding.buttonGroupTheme.check(themeButtonId)
    }

    private fun setupAppearanceSection() {
        try {
            val isDynamicAvailable = DynamicColors.isDynamicColorAvailable()
            if (!isDynamicAvailable) {
                binding.cardDynamicColors.visibility = View.GONE
            }
        } catch (e: Exception) {
            binding.cardDynamicColors.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        val cm = _configManager ?: return

        binding.cardMic.setOnClickListener { binding.switchMic.toggle() }
        binding.switchMic.setOnCheckedChangeListener { _, isChecked ->
            cm.isMicEnabled = isChecked
        }

        binding.cardSystemAudio.setOnClickListener { binding.switchSystemAudio.toggle() }
        binding.switchSystemAudio.setOnCheckedChangeListener { _, isChecked ->
            cm.isSystemAudioEnabled = isChecked
        }

        binding.cardShowTouches.setOnClickListener { binding.switchShowTouches.toggle() }
        binding.switchShowTouches.setOnCheckedChangeListener { _, isChecked ->
            cm.showTouches = isChecked
        }

        binding.buttonGroup.addOnButtonCheckedListener { _: MaterialButtonToggleGroup, checkedId: Int, isChecked: Boolean ->
            if (isChecked) {
                cm.videoQuality = when (checkedId) {
                    R.id.btnQualityMax -> ConfigManager.QUALITY_MAX
                    R.id.btnQuality4K -> ConfigManager.QUALITY_4K
                    R.id.btnQuality2K -> ConfigManager.QUALITY_2K
                    R.id.btnQuality1080 -> ConfigManager.QUALITY_1080P
                    R.id.btnQuality720 -> ConfigManager.QUALITY_720P
                    R.id.btnQuality480 -> ConfigManager.QUALITY_480P
                    R.id.btnQuality360 -> ConfigManager.QUALITY_360P
                    R.id.btnQuality240 -> ConfigManager.QUALITY_240P
                    else -> ConfigManager.QUALITY_MAX
                }
            }
        }

        binding.cardDynamicColors.setOnClickListener { binding.switchDynamicColors.toggle() }
        binding.switchDynamicColors.setOnCheckedChangeListener { _, isChecked ->
            if (cm.isDynamicColorsEnabled != isChecked) {
                cm.isDynamicColorsEnabled = isChecked
                try {
                    requireActivity().recreate()
                } catch (_: Exception) {}
            }
        }

        binding.buttonGroupTheme.addOnButtonCheckedListener { _: MaterialButtonToggleGroup, checkedId: Int, isChecked: Boolean ->
            if (isChecked) {
                val newMode = when (checkedId) {
                    R.id.btnThemeLight -> ConfigManager.THEME_LIGHT
                    R.id.btnThemeDark -> ConfigManager.THEME_DARK
                    else -> ConfigManager.THEME_SYSTEM
                }
                cm.themeMode = newMode
                AppCompatDelegate.setDefaultNightMode(cm.getThemeModeValue())
            }
        }

        binding.btnAbout.setOnClickListener {
            try {
                startActivity(Intent(requireContext(), AboutActivity::class.java))
                dismiss()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        binding.cardLanguage.setOnClickListener { showLanguageDialog() }
    }

    private fun setupDynamicQualities() {
        val cm = _configManager ?: return
        binding.btnQualityMax.text = cm.getMaxQualityLabel()
        val supported = cm.getAvailableQualityOptions()
        binding.btnQuality4K.visibility = if (supported.contains(ConfigManager.QUALITY_4K)) View.VISIBLE else View.GONE
        binding.btnQuality2K.visibility = if (supported.contains(ConfigManager.QUALITY_2K)) View.VISIBLE else View.GONE
        binding.btnQuality1080.visibility = if (supported.contains(ConfigManager.QUALITY_1080P)) View.VISIBLE else View.GONE
        binding.btnQuality720.visibility = if (supported.contains(ConfigManager.QUALITY_720P)) View.VISIBLE else View.GONE
        binding.btnQuality480.visibility = if (supported.contains(ConfigManager.QUALITY_480P)) View.VISIBLE else View.GONE
        binding.btnQuality360.visibility = if (supported.contains(ConfigManager.QUALITY_360P)) View.VISIBLE else View.GONE
        binding.btnQuality240.visibility = if (supported.contains(ConfigManager.QUALITY_240P)) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = SettingsBottomSheet()
    }
}
