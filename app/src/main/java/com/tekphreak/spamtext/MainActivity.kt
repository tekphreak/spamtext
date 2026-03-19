package com.tekphreak.spamtext

import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.tekphreak.spamtext.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = getString(R.string.app_name)

        PrefsHelper.init(this)

        binding.btnClearLog.setOnClickListener {
            CallLogger.clearLog(this)
            refreshLog()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        refreshLog()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun refreshStatus() {
        val hasPermissions = hasRequiredPermissions()
        val hasRole = hasCallScreeningRole()
        val enabled = PrefsHelper.isServiceEnabled

        val (statusText, statusColor) = when {
            !hasPermissions -> Pair(getString(R.string.status_no_permissions), getColor(R.color.status_inactive))
            !hasRole -> Pair(getString(R.string.status_no_role), getColor(R.color.status_warning))
            !enabled -> Pair(getString(R.string.status_inactive), getColor(R.color.status_inactive))
            else -> Pair(getString(R.string.status_active), getColor(R.color.status_active))
        }
        binding.tvStatus.text = statusText
        binding.tvStatus.setTextColor(statusColor)
    }

    private fun refreshLog() {
        val entries = CallLogger.getRecentEntries(this)
        if (entries.isEmpty()) {
            binding.tvCallLog.text = getString(R.string.no_log_entries)
        } else {
            binding.tvCallLog.text = entries.joinToString("\n")
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val perms = mutableListOf(
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.SEND_SMS,
            android.Manifest.permission.READ_PHONE_STATE
        )
        return perms.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasCallScreeningRole(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val roleManager = getSystemService(RoleManager::class.java)
        return roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }
}
