package app.rt521.schedule.data

import java.net.HttpURLConnection
import java.net.URL

/** One shared HTTP implementation for the app and the WorkManager worker. */
object ScheduleRemote {

    fun isConfigured(url: String): Boolean =
        url.isNotBlank() && !url.contains("OWNER", ignoreCase = true)

    fun fetch(url: String): Pair<String, ScheduleSnapshot> {
        val cacheBuster = if (url.contains('?')) "&ts=" else "?ts="
        val connection = (URL(url + cacheBuster + System.currentTimeMillis())
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "RT521-Schedule/2.0")
            setRequestProperty("Cache-Control", "no-cache")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code")
            val raw = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            return raw to ScheduleJsonParser.parse(raw)
        } finally {
            connection.disconnect()
        }
    }
}
