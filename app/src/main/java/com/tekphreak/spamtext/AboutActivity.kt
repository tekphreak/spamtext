package com.tekphreak.spamtext

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.tekphreak.spamtext.BuildConfig
import com.tekphreak.spamtext.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding
    private val handler = Handler(Looper.getMainLooper())
    private val advanceToSettings = Runnable {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        PrefsHelper.init(this)

        binding.tvVersion.text = getString(R.string.version_prefix) + BuildConfig.VERSION_NAME

        binding.btnWebsite.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://tekphreak.com")))
        }

        binding.btnDonate.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/tekphreak")))
        }

        NotePlayer.playChord()

        handler.postDelayed(advanceToSettings, 3000)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(advanceToSettings)
    }
}
