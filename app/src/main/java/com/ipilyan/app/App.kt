package com.ipilyan.app

import android.app.Application
import android.content.Intent

class App : Application() {

  override fun onCreate() {
    super.onCreate()
    Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))
  }

  fun checkCrashAndLaunch() {
    val prefs = getSharedPreferences("crash_prefs", MODE_PRIVATE)
    val log = prefs.getString("crash_log", null)
    if (log != null) {
      val intent = Intent(this, CrashActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
      }
      startActivity(intent)
    }
  }
}
