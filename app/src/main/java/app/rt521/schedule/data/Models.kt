package app.rt521.schedule.data

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

/* ============================================================
 *  Persisted settings
 * ============================================================ */

@Serializable
enum class ThemeStyle { MATERIAL, MIUIX, HYBRID }

@Serializable
enum class ThemeMode { LIGHT, DARK, AUTO }

@Serializable
data class AppSettings(
    val themeStyle: ThemeStyle = ThemeStyle.MATERIAL,
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val palette: String = "violet",
    val parityOverride: Int = 0, // 0 = auto, 1 = odd, 2 = even
    /** Empty means "use the URL baked into the build". */
    val scheduleUrl: String = "",
    val lastFetchAtMillis: Long = 0L,
)

/* ============================================================
 *  Remote schedule domain
 * ============================================================ */

enum class ChangeKind { REPLACE, EXTRA, CANCEL }

data class WorkItem(
    val date: LocalDate,
    val subject: String,
    val type: String,
    val title: String,
    val note: String = "",
) {
    val displayTitle: String get() = title.ifBlank { type }
}

data class ScheduleChange(
    val date: LocalDate,
    val kind: ChangeKind,
    val time: String,
    val subject: String = "",
    val room: String = "",
    val teachers: List<String> = emptyList(),
    val note: String = "",
)

data class ScheduleSnapshot(
    val group: String,
    val semester: Int,
    val academicYear: String,
    val semesterStart: LocalDate,
    val updatedAt: Instant?,
    val lessons: Map<DayOfWeek, List<Lesson>>,
    val changes: List<ScheduleChange>,
    val works: List<WorkItem>,
)

/* ============================================================
 *  Russian date formatting
 * ============================================================ */

private val RU = Locale("ru", "RU")

private val monthsGenitive = arrayOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря",
)

private val monthsShort = arrayOf(
    "янв", "фев", "мар", "апр", "мая", "июн",
    "июл", "авг", "сен", "окт", "ноя", "дек",
)

fun dayName(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "Понедельник"
    DayOfWeek.TUESDAY -> "Вторник"
    DayOfWeek.WEDNESDAY -> "Среда"
    DayOfWeek.THURSDAY -> "Четверг"
    DayOfWeek.FRIDAY -> "Пятница"
    DayOfWeek.SATURDAY -> "Суббота"
    DayOfWeek.SUNDAY -> "Воскресенье"
}

fun dayShort(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "Пн"
    DayOfWeek.TUESDAY -> "Вт"
    DayOfWeek.WEDNESDAY -> "Ср"
    DayOfWeek.THURSDAY -> "Чт"
    DayOfWeek.FRIDAY -> "Пт"
    DayOfWeek.SATURDAY -> "Сб"
    DayOfWeek.SUNDAY -> "Вс"
}

fun monthGenitive(month: Month): String = monthsGenitive[month.value - 1]

fun monthShort(month: Month): String = monthsShort[month.value - 1]

fun formatDateLong(date: LocalDate): String =
    "${dayName(date.dayOfWeek)}, ${date.dayOfMonth} ${monthGenitive(date.month)}"

fun formatDateMedium(date: LocalDate): String =
    "${date.dayOfMonth} ${monthShort(date.month)}"

fun formatDateWithYear(date: LocalDate): String =
    "${date.dayOfMonth} ${monthGenitive(date.month)} ${date.year}"

fun relativeDayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Сегодня"
    today.plusDays(1) -> "Завтра"
    today.minusDays(1) -> "Вчера"
    else -> dayName(date.dayOfWeek)
}

fun formatDateTime(instant: Instant?): String {
    if (instant == null) return "—"
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", RU)
    return formatter.format(instant.atZone(ZoneId.systemDefault()))
}

fun formatWeekdayShortRu(date: LocalDate): String =
    date.dayOfWeek.getDisplayName(TextStyle.SHORT, RU).let {
        it.removeSuffix(".").replaceFirstChar { c -> c.uppercase() }
    }

/** HTTPS anywhere; plain HTTP only for local development hosts. */
fun isAllowedScheduleUrl(url: String): Boolean {
    val value = url.trim()
    if (value.startsWith("https://")) return true
    return Regex("""^http://(10\.0\.2\.2|localhost|127\.0\.0\.1)(:\d+)?(/.*)?$""")
        .matches(value)
}
