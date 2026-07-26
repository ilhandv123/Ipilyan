package com.ipilyan.app

import android.content.Context
import android.content.Intent
import java.io.PrintWriter
import java.io.StringWriter

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

  override fun uncaughtException(thread: Thread, throwable: Throwable) {
    val sw = StringWriter()
    val pw = PrintWriter(sw)
    throwable.printStackTrace(pw)
    pw.flush()
    val stackTrace = sw.toString()

    val info = buildString {
      appendLine("=== IPILYAN CRASH REPORT ===")
      appendLine()
      appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
      appendLine("Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
      appendLine("App: ${context.packageName}")
      appendLine()
      appendLine("--- Exception ---")
      appendLine("${throwable.javaClass.name}: ${throwable.message}")
      appendLine()
      appendLine("--- Stack Trace ---")
      appendLine(stackTrace)
    }

    context.getSharedPreferences("crash_prefs", Context.MODE_PRIVATE)
      .edit()
      .putString("crash_log", info)
      .apply()

    val intent = Intent(context, CrashActivity::class.java).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
    context.startActivity(intent)
  }
}
