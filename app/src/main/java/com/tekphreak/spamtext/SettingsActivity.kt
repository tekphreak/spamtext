package com.tekphreak.spamtext

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.tekphreak.spamtext.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            requestCallScreeningRole()
        } else {
            Toast.makeText(this, getString(R.string.permissions_required), Toast.LENGTH_LONG).show()
            binding.switchEnable.isChecked = false
        }
    }

    private val roleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (hasCallScreeningRole()) {
            PrefsHelper.isServiceEnabled = true
            binding.switchEnable.isChecked = true
        } else {
            Toast.makeText(this, getString(R.string.grant_role_prompt), Toast.LENGTH_LONG).show()
            binding.switchEnable.isChecked = false
            PrefsHelper.isServiceEnabled = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings_title)

        PrefsHelper.init(this)

        onBackPressedDispatcher.addCallback(this,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() { finish() }
            })

        binding.switchEnable.isChecked = PrefsHelper.isServiceEnabled
        binding.cbTriggerNotInContacts.isChecked = PrefsHelper.isTriggerNotInContacts
        binding.cbTriggerFailedStir.isChecked = PrefsHelper.isTriggerFailedStir
        binding.etSmsMessage.setText(PrefsHelper.smsMessage)

        binding.switchEnable.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!hasRequiredPermissions()) {
                    requestPermissions()
                } else if (!hasCallScreeningRole()) {
                    requestCallScreeningRole()
                } else {
                    PrefsHelper.isServiceEnabled = true
                }
            } else {
                PrefsHelper.isServiceEnabled = false
            }
        }

        binding.cbTriggerNotInContacts.setOnCheckedChangeListener { _, isChecked ->
            PrefsHelper.isTriggerNotInContacts = isChecked
        }

        binding.cbTriggerFailedStir.setOnCheckedChangeListener { _, isChecked ->
            PrefsHelper.isTriggerFailedStir = isChecked
        }

        binding.btnSaveMessage.setOnClickListener {
            val msg = binding.etSmsMessage.text.toString().trim()
            if (msg.isNotEmpty()) {
                PrefsHelper.smsMessage = msg
                Toast.makeText(this, "Message saved", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnInsertTimestamp.setOnClickListener { insertTag("[timestamp]") }
        binding.btnInsertDate.setOnClickListener { insertTag("[date]") }
        binding.btnInsertTime.setOnClickListener { insertTag("[time]") }
        binding.btnInsertNumberCalled.setOnClickListener { insertTag("[numbercalled]") }

        binding.btnViewLog.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun insertTag(tag: String) {
        val et = binding.etSmsMessage
        val start = et.selectionStart.coerceAtLeast(0)
        val end = et.selectionEnd.coerceAtLeast(0)
        et.text.replace(minOf(start, end), maxOf(start, end), tag, 0, tag.length)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun hasRequiredPermissions(): Boolean {
        val perms = mutableListOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return perms.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        val perms = mutableListOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    private fun requestCallScreeningRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                roleLauncher.launch(intent)
            } else {
                PrefsHelper.isServiceEnabled = true
                binding.switchEnable.isChecked = true
            }
        }
    }

    private fun hasCallScreeningRole(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val roleManager = getSystemService(RoleManager::class.java)
        return roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }
}
