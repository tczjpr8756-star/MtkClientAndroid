package com.mtkclientandroid

import java.util.concurrent.atomic.AtomicBoolean

/**
 * JNI/Chaquopy surface between Python (android_entry.py, gui_utils.py) and
 * the Compose UI. Listeners are always invoked on the thread that Python
 * is running on; the ViewModel hops to Main.
 */
object UiBridge {
    @JvmStatic
    var logListener: ((String) -> Unit)? = null

    @JvmStatic
    var progressListener: ((Int, String) -> Unit)? = null

    @JvmStatic
    var errorDetailListener: ((String) -> Unit)? = null

    private val cancelled = AtomicBoolean(false)

    @JvmStatic
    fun appendLog(msg: String) {
        logListener?.invoke(msg)
    }

    @JvmStatic
    fun setProgress(percent: Int, message: String) {
        progressListener?.invoke(percent.coerceIn(0, 100), message)
    }

    @JvmStatic
    fun setErrorDetail(detail: String) {
        errorDetailListener?.invoke(detail)
    }

    @JvmStatic
    fun isCancelled(): Boolean = cancelled.get()

    @JvmStatic
    fun requestCancel() {
        cancelled.set(true)
    }

    @JvmStatic
    fun clearCancel() {
        cancelled.set(false)
    }
}
