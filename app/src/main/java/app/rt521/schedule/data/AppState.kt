package app.rt521.schedule.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicBoolean

class AppState(
    context: Context,
    val defaultScheduleUrl: String,
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("rt521_state", Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshGuard = AtomicBoolean(false)

    var settings: AppSettings by mutableStateOf(loadSettings())
        private set

    var schedule: ScheduleSnapshot by mutableStateOf(loadInitialSchedule())
        private set

    var now: LocalDateTime by mutableStateOf(LocalDateTime.now())
        private set

    var selectedDate: LocalDate by mutableStateOf(LocalDate.now())
        private set

    var isRefreshing: Boolean by mutableStateOf(false)
        private set

    var lastError: String? by mutableStateOf(null)
        private set

    var refreshMessage: String? by mutableStateOf(null)
        private set

    var lastFetchAtMillis: Long by mutableLongStateOf(settings.lastFetchAtMillis)
        private set

    val scheduleUrl: String
        get() = settings.scheduleUrl.ifBlank { defaultScheduleUrl }

    val usesDefaultUrl: Boolean
        get() = settings.scheduleUrl.isBlank()

    val scheduleConfigured: Boolean
        get() = scheduleUrl.isNotBlank() && !scheduleUrl.contains("OWNER", ignoreCase = true)

    init {
        scope.launch { autoRefreshLoop() }
        ScheduleRefreshScheduler.scheduleNext(appContext)
    }

    /* ---------- Persistence ---------- */

    private fun loadSettings(): AppSettings = runCatching {
        prefs.getString("settings", null)?.let { json.decodeFromString<AppSettings>(it) }
    }.getOrNull() ?: AppSettings()

    private fun persistSettings() {
        prefs.edit { putString("settings", json.encodeToString(settings)) }
    }

    private fun loadInitialSchedule(): ScheduleSnapshot {
        prefs.getString("cached_schedule_json", null)?.let { raw ->
            runCatching { ScheduleJsonParser.parse(raw) }.getOrNull()?.let { return it }
        }
        return loadBundledSchedule() ?: ScheduleJsonParser.emptySnapshot()
    }

    private fun loadBundledSchedule(): ScheduleSnapshot? = runCatching {
        appContext.assets.open("schedule.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            .let { ScheduleJsonParser.parse(it) }
    }.getOrNull()

    /* ---------- Clock / selection ---------- */

    fun tick() {
        val previousDay = now.toLocalDate()
        val updated = LocalDateTime.now()
        now = updated
        if (selectedDate == previousDay && updated.toLocalDate() != previousDay) {
            selectedDate = updated.toLocalDate()
        }
    }

    fun selectDate(date: LocalDate) {
        selectedDate = date
    }

    fun goToToday() {
        selectedDate = LocalDate.now()
    }

    /* ---------- Settings ---------- */

    fun setThemeStyle(style: ThemeStyle) {
        settings = settings.copy(themeStyle = style)
        persistSettings()
    }

    fun setThemeMode(mode: ThemeMode) {
        settings = settings.copy(themeMode = mode)
        persistSettings()
    }

    fun setPalette(id: String) {
        settings = settings.copy(palette = id)
        persistSettings()
    }

    fun setScheduleUrl(url: String) {
        settings = settings.copy(scheduleUrl = url.trim())
        persistSettings()
        refreshAsync(manual = true)
    }

    fun resetScheduleUrl() {
        settings = settings.copy(scheduleUrl = "")
        persistSettings()
        if (scheduleConfigured) {
            refreshAsync(manual = true)
        } else {
            lastError = null
        }
    }

    fun clearRefreshMessage() {
        refreshMessage = null
    }

    /* ---------- Parity ---------- */

    fun parityFor(date: LocalDate): WeekParity = when (settings.parityOverride) {
        1 -> WeekParity.ODD
        2 -> WeekParity.EVEN
        else -> autoParityFor(schedule.semesterStart, date)
    }

    val parityIsManual: Boolean get() = settings.parityOverride != 0

    fun toggleParityManually() {
        val current = parityFor(selectedDate)
        settings = settings.copy(
            parityOverride = if (current == WeekParity.ODD) 2 else 1,
        )
        persistSettings()
    }

    fun setParityAuto() {
        settings = settings.copy(parityOverride = 0)
        persistSettings()
    }

    fun setParityOverride(value: Int) {
        settings = settings.copy(parityOverride = value.coerceIn(0, 2))
        persistSettings()
    }

    /* ---------- GitHub refresh ---------- */

    fun refreshAsync(manual: Boolean = true) {
        if (isRefreshing) return
        scope.launch { refresh(manual) }
    }

    /** Fetch if the last successful update is older than the latest 18:00/20:30 boundary. */
    fun refreshIfStale() {
        if (!scheduleConfigured) return
        val nowZ = ZonedDateTime.now()
        val latest = latestRefreshAt(nowZ).toInstant().toEpochMilli()
        val last = if (lastFetchAtMillis > 0) lastFetchAtMillis else 0L
        if (last == 0L || last < latest) {
            refreshAsync(manual = false)
        }
    }

    private suspend fun refresh(manual: Boolean) {
        if (!refreshGuard.compareAndSet(false, true)) return
        try {
            refreshInternal(manual)
        } finally {
            refreshGuard.set(false)
        }
    }

    private suspend fun refreshInternal(manual: Boolean) {
        if (isRefreshing) return
        val url = scheduleUrl.trim()
        if (!scheduleConfigured) {
            withContext(Dispatchers.Main.immediate) {
                if (manual) {
                    lastError = "Укажите ссылку на data/schedule.json в настройках"
                    refreshMessage = lastError
                } else {
                    lastError = null
                }
            }
            return
        }

        withContext(Dispatchers.Main.immediate) {
            isRefreshing = true
            lastError = null
        }

        val result = runCatching { ScheduleRemote.fetch(url) }

        withContext(Dispatchers.Main.immediate) {
            result.onSuccess { (raw, snapshot) ->
                schedule = snapshot
                val fetchedAt = System.currentTimeMillis()
                lastFetchAtMillis = fetchedAt
                settings = settings.copy(lastFetchAtMillis = fetchedAt)
                persistSettings()
                prefs.edit { putString("cached_schedule_json", raw) }
                lastError = null
                if (manual) refreshMessage = "Расписание обновлено"
            }.onFailure { error ->
                val reason = error.message?.takeIf { it.isNotBlank() } ?: "неизвестная ошибка"
                lastError = reason
                if (manual) refreshMessage = "Не удалось обновить: $reason"
            }
            isRefreshing = false
        }
    }

    private suspend fun autoRefreshLoop() {
        while (scope.isActive) {
            val nowZ = ZonedDateTime.now()
            val next = nextRefreshAt(nowZ)
            val waitMs = Duration.between(nowZ, next).toMillis().coerceAtLeast(1_000L)
            delay(waitMs)
            if (scheduleConfigured) {
                refresh(manual = false)
            }
        }
    }

    /* ---------- Convenience ---------- */

    val updatedAt: Instant?
        get() = schedule.updatedAt

    val lastFetchAt: Instant?
        get() = lastFetchAtMillis.takeIf { it > 0 }?.let(Instant::ofEpochMilli)
}
