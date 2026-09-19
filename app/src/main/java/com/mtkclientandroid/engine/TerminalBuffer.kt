package com.mtkclientandroid.engine

/**
 * In-memory terminal with '\r' in-place rewrite, matching the previous
 * Editable-backed TerminalBuffer so mtkclient progress bars still collapse
 * onto one line.
 */
class TerminalBuffer {
    private val sb = StringBuilder()
    private var lineStart = 0
    private val maxChars = 60_000
    private val trimToChars = 45_000

    @Synchronized
    fun append(chunk: String): String {
        var i = 0
        while (i < chunk.length) {
            when (chunk[i]) {
                '\r' -> {
                    if (sb.length > lineStart) sb.delete(lineStart, sb.length)
                    i++
                }
                '\n' -> {
                    sb.append('\n')
                    lineStart = sb.length
                    i++
                }
                '\b' -> {
                    if (sb.length > lineStart) sb.delete(sb.length - 1, sb.length)
                    i++
                }
                else -> {
                    var j = i + 1
                    while (j < chunk.length && chunk[j] != '\r' && chunk[j] != '\n' && chunk[j] != '\b') j++
                    sb.append(chunk, i, j)
                    i = j
                }
            }
        }
        trim()
        return sb.toString()
    }

    @Synchronized
    fun clear() {
        sb.clear()
        lineStart = 0
    }

    @Synchronized
    fun snapshot(): String = sb.toString()

    private fun trim() {
        if (sb.length > maxChars) {
            val drop = sb.length - trimToChars
            sb.delete(0, drop)
            lineStart = (lineStart - drop).coerceAtLeast(0)
        }
    }
}

class LogFilter(
    var enabled: Boolean = true,
    var windowMs: Long = 5_000L
) {
    private val recent = LinkedHashMap<String, Long>(64, 0.75f, true)
    private val pending = StringBuilder()

    @Synchronized
    fun filter(chunk: String): String {
        if (!enabled) {
            pending.clear()
            return chunk
        }
        if ('\r' in chunk) {
            pending.clear()
            return chunk
        }
        val out = StringBuilder(chunk.length)
        for (c in chunk) {
            if (c == '\n') {
                val line = pending.toString()
                pending.clear()
                if (shouldShow(line)) out.append(line).append('\n')
            } else {
                pending.append(c)
            }
        }
        return out.toString()
    }

    @Synchronized
    fun reset() {
        recent.clear()
        pending.clear()
    }

    private fun shouldShow(line: String): Boolean {
        val key = line.trimEnd()
        if (key.isBlank()) return true
        val mapKey = if (key.length > 120) key.take(120) else key
        val now = System.currentTimeMillis()
        val last = recent[mapKey]
        recent[mapKey] = now
        if (recent.size > 128) {
            recent.entries.iterator().let { it.next(); it.remove() }
        }
        return last == null || (now - last) >= windowMs
    }
}
