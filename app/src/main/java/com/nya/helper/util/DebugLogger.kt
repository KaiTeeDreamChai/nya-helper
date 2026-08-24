package com.nya.helper.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

object DebugLogger {

    private val logs = CopyOnWriteArrayList<String>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private var onLogUpdatedListener: (() -> Unit)? = null

    fun log(message: String) {
        val timeStr = dateFormat.format(Date())
        val entry = "[$timeStr] $message"
        logs.add(entry)
        if (logs.size > 100) {
            logs.removeAt(0)
        }
        onLogUpdatedListener?.invoke()
    }

    fun getLogs(): List<String> = logs.toList()

    fun getAllLogsText(): String {
        if (logs.isEmpty()) {
            return "暂无日志记录。请开启无障碍服务并在微信/QQ中打字测试。"
        }
        return logs.joinToString("\n")
    }

    fun clear() {
        logs.clear()
        onLogUpdatedListener?.invoke()
    }

    fun setOnLogUpdatedListener(listener: (() -> Unit)?) {
        onLogUpdatedListener = listener
    }
}
