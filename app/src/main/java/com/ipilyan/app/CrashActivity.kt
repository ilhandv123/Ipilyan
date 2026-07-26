package com.ipilyan.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ipilyan.app.databinding.ActivityCrashBinding

class CrashActivity : AppCompatActivity() {

  private lateinit var binding: ActivityCrashBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityCrashBinding.inflate(layoutInflater)
    setContentView(binding.root)

    val log = getSharedPreferences("crash_prefs", MODE_PRIVATE)
      .getString("crash_log", "Unknown error") ?: "Unknown error"

    binding.tvCrashLog.text = log

    binding.btnCopyLog.setOnClickListener {
      val clip = ClipData.newPlainText("crash_log", log)
      val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
      cm.setPrimaryClip(clip)
      Toast.makeText(this, "COPIED TO CLIPBOARD", Toast.LENGTH_SHORT).show()
    }

    binding.btnRestart.setOnClickListener {
      getSharedPreferences("crash_prefs", MODE_PRIVATE)
        .edit().remove("crash_log").apply()
      val intent = Intent(this, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
      }
      startActivity(intent)
      finish()
      Runtime.getRuntime().exit(0)
    }
  }
}
