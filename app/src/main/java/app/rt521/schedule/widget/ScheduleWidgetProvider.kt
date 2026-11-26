package app.rt521.schedule.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import app.rt521.schedule.MainActivity
import app.rt521.schedule.R
import app.rt521.schedule.data.AppSettings
import app.rt521.schedule.data.ChangeKind
import app.rt521.schedule.data.Lesson
import app.rt521.schedule.data.LiveStatus
import app.rt521.schedule.data.ScheduleJsonParser
import app.rt521.schedule.data.ScheduleSnapshot
import app.rt521.schedule.data.WeekParity
import app.rt521.schedule.data.activeLessons
import app.rt521.schedule.data.autoParityFor
import app.rt521.schedule.data.computeLiveStatus
import app.rt521.schedule.data.dayShort
import app.rt521.schedule.data.dayLessons
import app.rt521.schedule.data.monthShort
import app.rt521.schedule.data.worksForDate
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/* ============================================================
 *  Providers
 * ============================================================ */

enum class WidgetSize { SMALL, MEDIUM, LARGE }

abstract class BaseScheduleWidgetProvider(
    private val size: WidgetSize,
) : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        ScheduleWidgetUpdater.update(context, appWidgetManager, appWidgetIds, size)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle,
    ) {
        ScheduleWidgetUpdater.update(context, appWidgetManager, intArrayOf(appWidgetId), size)
    }

    override fun onEnabled(context: Context) {
        ScheduleWidgetUpdater.updateAll(context)
    }
}

class SmallScheduleWidget : BaseScheduleWidgetProvider(WidgetSize.SMALL)
class MediumScheduleWidget : BaseScheduleWidgetProvider(WidgetSize.MEDIUM)
class LargeScheduleWidget : BaseScheduleWidgetProvider(WidgetSize.LARGE)

/* ============================================================
 *  Updater
 * ============================================================ */

data class WidgetRow(
    val time: String,
    val subject: String,
    val room: String,
    val current: Boolean,
)

data class WidgetContent(
    val date: String,
    val status: String,
    val subject: String,
    val meta: String,
    val next: String,
    val countdownSeconds: Int?,
    val rows: List<WidgetRow>,
    val work: String?,
)

object ScheduleWidgetUpdater {
    private val providers = listOf(
        SmallScheduleWidget::class.java to WidgetSize.SMALL,
        MediumScheduleWidget::class.java to WidgetSize.MEDIUM,
        LargeScheduleWidget::class.java to WidgetSize.LARGE,
    )

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        providers.forEach { (provider, size) ->
            val ids = manager.getAppWidgetIds(ComponentName(context, provider))
            if (ids.isNotEmpty()) {
                update(context, manager, ids, size)
            }
        }
    }

    fun update(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray,
        size: WidgetSize,
    ) {
        val content = buildContent(context) ?: return
        ids.forEach { id ->
            manager.updateAppWidget(id, buildViews(context, size, content))
        }
    }

    private fun buildContent(context: Context): WidgetContent? {
        val snapshot = loadSnapshot(context) ?: return null
        val settings = loadSettings(context)
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val parity = when (settings.parityOverride) {
            1 -> WeekParity.ODD
            2 -> WeekParity.EVEN
            else -> autoParityFor(snapshot.semesterStart, today)
        }
        val lessons = dayLessons(snapshot, today, parity)
        val active = activeLessons(lessons)
        val live = computeLiveStatus(now, lessons)
        val tomorrow = today.plusDays(1)
        val tomorrowParity = when (settings.parityOverride) {
            1 -> WeekParity.ODD
            2 -> WeekParity.EVEN
            else -> autoParityFor(snapshot.semesterStart, tomorrow)
        }
        val tomorrowFirst = activeLessons(dayLessons(snapshot, tomorrow, tomorrowParity)).firstOrNull()

        val currentLesson = when (live) {
            is LiveStatus.InLesson -> live.lesson
            is LiveStatus.BeforeFirst -> live.lesson
            is LiveStatus.OnBreak -> live.next
            else -> null
        }
        val currentIndex = active.indexOfFirst {
            it.time == currentLesson?.time && it.subject == currentLesson?.subject
        }

        val status = when (live) {
            is LiveStatus.InLesson -> "СЕЙЧАС"
            is LiveStatus.BeforeFirst -> "ДО ПЕРВОЙ ПАРЫ"
            is LiveStatus.OnBreak -> if (live.big) "БОЛЬШОЙ ПЕРЕРЫВ" else "ПЕРЕРЫВ"
            is LiveStatus.Finished -> "ПАРЫ ЗАКОНЧИЛИСЬ"
            is LiveStatus.NoLessons -> "ПАР НЕТ"
            null -> "ПАР НЕТ"
        }

        val subject = when (live) {
            is LiveStatus.InLesson -> live.lesson.subject
            is LiveStatus.BeforeFirst -> live.lesson.subject
            is LiveStatus.OnBreak -> live.next?.let { "Далее: ${it.subject}" } ?: "Перерыв"
            is LiveStatus.Finished -> tomorrowFirst?.subject ?: "До завтра"
            is LiveStatus.NoLessons -> if (today.dayOfWeek == java.time.DayOfWeek.SUNDAY) "Выходной" else "Свободный день"
            null -> "Пар нет"
        }

        val meta = when (live) {
            is LiveStatus.InLesson -> formatMeta(live.lesson)
            is LiveStatus.BeforeFirst -> formatMeta(live.lesson)
            is LiveStatus.OnBreak -> live.next?.let { formatMeta(it) } ?: ""
            is LiveStatus.Finished -> tomorrowFirst?.let { "Завтра: ${it.time} · каб. ${it.room}" } ?: ""
            is LiveStatus.NoLessons -> ""
            null -> ""
        }

        val countdown = when (live) {
            is LiveStatus.InLesson -> live.secondsLeft
            is LiveStatus.BeforeFirst -> live.secondsLeft
            is LiveStatus.OnBreak -> live.secondsLeft
            else -> null
        }

        val next = when (live) {
            is LiveStatus.InLesson -> active.getOrNull(currentIndex + 1)?.let {
                "Далее: ${it.time} · ${shortSubject(it.subject)}"
            }
            is LiveStatus.BeforeFirst -> active.getOrNull(1)?.let {
                "Далее: ${it.time} · ${shortSubject(it.subject)}"
            }
            is LiveStatus.OnBreak -> {
                val nextIndex = active.indexOfFirst {
                    it.time == live.next?.time && it.subject == live.next?.subject
                }
                active.getOrNull(nextIndex + 1)?.let {
                    "Потом: ${it.time} · ${shortSubject(it.subject)}"
                }
            }
            is LiveStatus.Finished -> tomorrowFirst?.let {
                "Завтра: ${it.time} · ${shortSubject(it.subject)}"
            }
            else -> tomorrowFirst?.let {
                "Завтра: ${it.time} · ${shortSubject(it.subject)}"
            }
        }.orEmpty()

        val rows = active.take(5).mapIndexed { index, lesson ->
            WidgetRow(
                time = lesson.time.substringBefore('-'),
                subject = if (lesson.change == ChangeKind.CANCEL) "${lesson.subject} (отмена)" else lesson.subject,
                room = lesson.room,
                current = index == currentIndex,
            )
        }

        val work = worksForDate(snapshot.works, today).firstOrNull()?.let {
            "Работа: ${it.displayTitle}"
        }

        return WidgetContent(
            date = "СЕГОДНЯ · ${dayShort(today.dayOfWeek).uppercase(Locale("ru"))} ${today.dayOfMonth} ${monthShort(today.month).uppercase(Locale("ru"))}",
            status = status,
            subject = subject,
            meta = meta,
            next = next,
            countdownSeconds = countdown,
            rows = rows,
            work = work,
        )
    }

    private fun formatMeta(lesson: Lesson): String =
        "${lesson.time} · каб. ${lesson.room.ifBlank { "—" }}"

    private fun shortSubject(subject: String): String {
        val clean = subject
            .replace("Цифровые микропроцессорные устройства", "ЦиМПУ")
            .replace("Основы построения систем и сетей телекоммуникаций", "ОПСиСТ")
            .replace("Метрология и специальные измерения", "Метрология")
            .replace("Направляющие системы телекоммуникаций", "НСТк")
            .replace("Радиоприёмные устройства", "РПУ")
            .replace("Радиопередающие устройства", "РПдУ")
        return if (clean.length > 42) clean.take(40) + "…" else clean
    }

    private fun loadSnapshot(context: Context): ScheduleSnapshot? {
        val prefs = context.getSharedPreferences("rt521_state", Context.MODE_PRIVATE)
        val cached = prefs.getString("cached_schedule_json", null)
        if (cached != null) {
            runCatching { ScheduleJsonParser.parse(cached) }.getOrNull()?.let { return it }
        }
        return runCatching {
            context.assets.open("schedule.json")
                .bufferedReader(Charsets.UTF_8)
                .use { ScheduleJsonParser.parse(it.readText()) }
        }.getOrNull()
    }

    private fun loadSettings(context: Context): AppSettings {
        val prefs = context.getSharedPreferences("rt521_state", Context.MODE_PRIVATE)
        return runCatching {
            prefs.getString("settings", null)?.let { json.decodeFromString<AppSettings>(it) }
        }.getOrNull() ?: AppSettings()
    }

    private fun buildViews(context: Context, size: WidgetSize, content: WidgetContent): RemoteViews {
        val layout = when (size) {
            WidgetSize.SMALL -> R.layout.widget_small
            WidgetSize.MEDIUM -> R.layout.widget_medium
            WidgetSize.LARGE -> R.layout.widget_large
        }
        val views = RemoteViews(context.packageName, layout)
        views.setTextViewText(R.id.widget_date, content.date)
        views.setTextViewText(R.id.widget_status, content.status)
        views.setTextViewText(R.id.widget_subject, content.subject)
        views.setTextViewText(R.id.widget_meta, content.meta)

        if (size != WidgetSize.SMALL) {
            views.setTextViewText(R.id.widget_next, content.next)
            views.setViewVisibility(
                R.id.widget_next,
                if (content.next.isBlank()) View.GONE else View.VISIBLE,
            )
        }

        if (content.countdownSeconds != null) {
            views.setViewVisibility(R.id.widget_timer, View.VISIBLE)
            views.setChronometer(
                R.id.widget_timer,
                SystemClock.elapsedRealtime() + content.countdownSeconds * 1000L,
                null,
                true,
            )
            views.setChronometerCountDown(R.id.widget_timer, true)
        } else {
            views.setViewVisibility(R.id.widget_timer, View.GONE)
        }

        if (size == WidgetSize.LARGE) {
            bindRows(context, views, content.rows)
            views.setTextViewText(R.id.widget_work, content.work.orEmpty())
            views.setViewVisibility(
                R.id.widget_work,
                if (content.work.isNullOrBlank()) View.GONE else View.VISIBLE,
            )
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
        return views
    }

    private fun bindRows(context: Context, views: RemoteViews, rows: List<WidgetRow>) {
        val ids = listOf(
            Triple(R.id.widget_row1, R.id.widget_row1_time, R.id.widget_row1_title),
            Triple(R.id.widget_row2, R.id.widget_row2_time, R.id.widget_row2_title),
            Triple(R.id.widget_row3, R.id.widget_row3_time, R.id.widget_row3_title),
            Triple(R.id.widget_row4, R.id.widget_row4_time, R.id.widget_row4_title),
            Triple(R.id.widget_row5, R.id.widget_row5_time, R.id.widget_row5_title),
        )
        val rooms = listOf(
            R.id.widget_row1_room, R.id.widget_row2_room, R.id.widget_row3_room,
            R.id.widget_row4_room, R.id.widget_row5_room,
        )
        val accent = ContextCompat.getColor(context, R.color.widget_accent)
        val primary = ContextCompat.getColor(context, R.color.widget_text)
        val secondary = ContextCompat.getColor(context, R.color.widget_secondary)

        ids.forEachIndexed { index, (rowId, timeId, titleId) ->
            val row = rows.getOrNull(index)
            if (row == null) {
                views.setViewVisibility(rowId, View.GONE)
            } else {
                views.setViewVisibility(rowId, View.VISIBLE)
                views.setTextViewText(timeId, row.time)
                views.setTextViewText(titleId, row.subject)
                views.setTextViewText(rooms[index], "каб. ${row.room}")
                views.setTextColor(timeId, if (row.current) accent else secondary)
                views.setTextColor(titleId, if (row.current) accent else primary)
                views.setTextColor(rooms[index], secondary)
            }
        }
    }
}
