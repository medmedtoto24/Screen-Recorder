package com.mohammedtahriyne.screenrecorder

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mohammedtahriyne.screenrecorder.databinding.ActivityRegistrationBinding

class RegistrationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrationBinding
    private lateinit var profileManager: ProfileManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applyVerticalInsets()
        profileManager = ProfileManager(this)

        binding.toolbar.setNavigationOnClickListener { finish() }

        loadExistingProfile()
        setupButtons()
    }

    private fun loadExistingProfile() {
        if (profileManager.isRegistered()) {
            binding.etName.setText(profileManager.profileName)
            binding.etEmail.setText(profileManager.profileEmail)
            binding.etBio.setText(profileManager.profileBio)
        }
    }

    private fun setupButtons() {
        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val bio = binding.etBio.text.toString().trim()

            if (name.isEmpty()) {
                binding.tilName.error = getString(R.string.registration_error_name)
                return@setOnClickListener
            }

            binding.tilName.error = null
            binding.tilEmail.error = null

            profileManager.profileName = name
            profileManager.profileEmail = email
            profileManager.profileBio = bio

            Toast.makeText(this, getString(R.string.registration_success), Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnSkip.setOnClickListener { finish() }
    }
}
