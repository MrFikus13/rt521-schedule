package app.rt521.schedule.data

import android.content.Context
import androidx.core.content.edit
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.rt521.schedule.BuildConfig
import app.rt521.schedule.widget.ScheduleWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Background refresh at 18:00 and 20:30. If the app is closed, WorkManager updates the
 * cache; the next launch reads it immediately. If the system defers the job, the launch/resume
 * freshness check in AppState covers it.
 */
class ScheduleRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val prefs = applicationContext.getSharedPreferences("rt521_state", Context.MODE_PRIVATE)
            val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
            val settings = runCatching {
                prefs.getString("settings", null)?.let { json.decodeFromString<AppSettings>(it) }
            }.getOrNull() ?: AppSettings()
            val url = settings.scheduleUrl.ifBlank { BuildConfig.SCHEDULE_URL }

            if (ScheduleRemote.isConfigured(url)) {
                val (raw, _) = ScheduleRemote.fetch(url)
                val fetchedAt = System.currentTimeMillis()
                prefs.edit {
                    putString("cached_schedule_json", raw)
                    putString(
                        "settings",
                        json.encodeToString(settings.copy(lastFetchAtMillis = fetchedAt)),
                    )
                }
                ScheduleWidgetUpdater.updateAll(applicationContext)
            }
            Result.success()
        } catch (_: Exception) {
            // Keep the previous cache and try again at the next scheduled time.
            Result.success()
        } finally {
            ScheduleRefreshScheduler.scheduleNext(applicationContext)
        }
    }
}

object ScheduleRefreshScheduler {
    private const val WORK_NAME = "rt521-auto-refresh"

    fun scheduleNext(context: Context) {
        runCatching {
            val now = ZonedDateTime.now()
            val next = nextRefreshAt(now)
            val delayMs = Duration.between(now, next).toMillis().coerceAtLeast(1_000L)
            val request = OneTimeWorkRequestBuilder<ScheduleRefreshWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                .addTag(WORK_NAME)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
