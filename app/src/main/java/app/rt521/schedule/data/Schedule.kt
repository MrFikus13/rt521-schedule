package app.rt521.schedule.data

import androidx.compose.ui.graphics.Color
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/* ============================================================
 *  Schedule model
 * ============================================================ */

data class Lesson(
    val time: String,
    val subject: String,
    val room: String,
    val teachers: List<String> = emptyList(),
    /** null = every week, [1,3] = odd weeks, [2,4] = even weeks */
    val weeks: List<Int>? = null,
    val change: ChangeKind? = null,
    val note: String = "",
    val cancelled: Boolean = false,
) {
    val startMinutes: Int get() = parseTime(time.substringBefore('-'))
    val endMinutes: Int get() = parseTime(time.substringAfter('-'))
}

data class TimeSlot(val n: Int, val start: Int, val end: Int) {
    val midStart: Int get() = start + 45
    val midEnd: Int get() = start + 55
    val startLabel: String get() = formatTime(start)
    val endLabel: String get() = formatTime(end)
    val rangeLabel: String get() = "$startLabel-$endLabel"
}

data class BreakPeriod(val start: Int, val end: Int, val big: Boolean) {
    val rangeLabel: String get() = "${formatTime(start)}–${formatTime(end)}"
}

enum class WeekParity { ODD, EVEN }

enum class LessonPhase { FIRST_HALF, MID_BREAK, SECOND_HALF }

sealed interface LiveStatus {
    data class BeforeFirst(val lesson: Lesson, val secondsLeft: Int) : LiveStatus
    data class InLesson(
        val lesson: Lesson,
        val phase: LessonPhase,
        val endMinutes: Int,
        val totalMinutes: Int,
        val secondsLeft: Int,
        val progress: Float,
    ) : LiveStatus
    data class OnBreak(
        val next: Lesson?,
        val big: Boolean,
        val endMinutes: Int,
        val totalMinutes: Int,
        val secondsLeft: Int,
        val progress: Float,
    ) : LiveStatus
    data class Finished(val totalToday: Int) : LiveStatus
    data object NoLessons : LiveStatus
}

object ScheduleData {
    val slots: List<TimeSlot> = listOf(
        TimeSlot(1, 8 * 60, 9 * 60 + 40),
        TimeSlot(2, 9 * 60 + 55, 11 * 60 + 35),
        TimeSlot(3, 12 * 60 + 15, 13 * 60 + 55),
        TimeSlot(4, 14 * 60 + 10, 15 * 60 + 50),
        TimeSlot(5, 16 * 60 + 20, 18 * 60),
        TimeSlot(6, 18 * 60 + 15, 19 * 60 + 55),
    )

    val breaks: List<BreakPeriod> = listOf(
        BreakPeriod(9 * 60 + 40, 9 * 60 + 55, big = false),
        BreakPeriod(11 * 60 + 35, 12 * 60 + 15, big = true),
        BreakPeriod(13 * 60 + 55, 14 * 60 + 10, big = false),
        BreakPeriod(15 * 60 + 50, 16 * 60 + 20, big = true),
        BreakPeriod(18 * 60, 18 * 60 + 15, big = false),
    )

    fun slotFor(time: String): TimeSlot? {
        val start = parseTime(time.substringBefore('-'))
        val end = parseTime(time.substringAfter('-'))
        return slots.firstOrNull { it.start == start && it.end == end }
    }

    fun breakBetween(slot: TimeSlot, nextStart: Int): BreakPeriod? =
        breaks.firstOrNull { it.start == slot.end && it.end == nextStart }
}

/* ============================================================
 *  Date / time helpers
 * ============================================================ */

fun parseTime(value: String): Int {
    val parts = value.trim().split(':')
    return parts[0].toInt() * 60 + parts.getOrElse(1) { "0" }.toInt()
}

fun formatTime(minutes: Int): String {
    val h = (minutes / 60).coerceIn(0, 23)
    val m = minutes % 60
    return "%02d:%02d".format(h, m)
}

fun formatCountdown(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
}

fun nowMinutes(dt: LocalDateTime): Float =
    dt.hour * 60f + dt.minute + dt.second / 60f

fun semesterWeek(semesterStart: LocalDate, date: LocalDate): Int {
    val startMonday = semesterStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val days = ChronoUnit.DAYS.between(startMonday, date)
    return Math.floorDiv(days, 7L).toInt() + 1
}

fun autoParityFor(semesterStart: LocalDate, date: LocalDate): WeekParity =
    if (semesterWeek(semesterStart, date) % 2 != 0) WeekParity.ODD else WeekParity.EVEN

fun matchesWeeks(lesson: Lesson, parity: WeekParity): Boolean {
    val weeks = lesson.weeks ?: return true
    return when (parity) {
        WeekParity.ODD -> weeks.any { it % 2 != 0 }
        WeekParity.EVEN -> weeks.any { it % 2 == 0 }
    }
}

/* ============================================================
 *  Applying GitHub changes
 * ============================================================ */

private fun changeOrder(kind: ChangeKind): Int = when (kind) {
    ChangeKind.CANCEL -> 0
    ChangeKind.REPLACE -> 1
    ChangeKind.EXTRA -> 2
    ChangeKind.NOTE -> 3
}

/**
 * Base lessons + changes from the JSON file for a concrete date.
 * Cancelled lessons are returned too, so the UI can show that a pair is gone.
 */
fun dayLessons(
    snapshot: ScheduleSnapshot,
    date: LocalDate,
    parity: WeekParity,
): List<Lesson> {
    val base = snapshot.lessons[date.dayOfWeek].orEmpty()
        .filter { matchesWeeks(it, parity) }
    val list = base.toMutableList()

    snapshot.changes
        .filter { it.date == date }
        .sortedWith(compareBy({ changeOrder(it.kind) }, { parseTime(it.time.substringBefore('-')) }))
        .forEach { change ->
            when (change.kind) {
                ChangeKind.CANCEL -> {
                    val matching = list.indices.filter { index ->
                        val lesson = list[index]
                        lesson.time == change.time && (
                            change.subject.isBlank() ||
                                normalizedSubject(lesson.subject) == normalizedSubject(change.subject)
                            )
                    }
                    if (matching.isEmpty()) {
                        list += Lesson(
                            time = change.time,
                            subject = change.subject.ifBlank { "Пара" },
                            room = change.room,
                            teachers = change.teachers,
                            change = ChangeKind.CANCEL,
                            note = change.note,
                            cancelled = true,
                        )
                    } else {
                        matching.forEach { index ->
                            list[index] = list[index].copy(
                                change = ChangeKind.CANCEL,
                                note = change.note.ifBlank { list[index].note },
                                cancelled = true,
                            )
                        }
                    }
                }

                ChangeKind.REPLACE -> {
                    list.removeAll { it.time == change.time }
                    list += Lesson(
                        time = change.time,
                        subject = change.subject.ifBlank { "Пара" },
                        room = change.room,
                        teachers = change.teachers,
                        change = ChangeKind.REPLACE,
                        note = change.note,
                    )
                }

                ChangeKind.EXTRA -> {
                    list += Lesson(
                        time = change.time,
                        subject = change.subject.ifBlank { "Пара" },
                        room = change.room,
                        teachers = change.teachers,
                        change = ChangeKind.EXTRA,
                        note = change.note,
                    )
                }

                ChangeKind.NOTE -> {
                    val matching = list.indices.filter { index ->
                        val lesson = list[index]
                        lesson.time == change.time && (
                            change.subject.isBlank() ||
                                normalizedSubject(lesson.subject) == normalizedSubject(change.subject)
                            )
                    }
                    if (matching.isEmpty()) {
                        list += Lesson(
                            time = change.time,
                            subject = change.subject.ifBlank { "Примечание" },
                            room = change.room,
                            teachers = change.teachers,
                            change = ChangeKind.NOTE,
                            note = change.note,
                        )
                    } else {
                        matching.forEach { index ->
                            val previous = list[index]
                            list[index] = previous.copy(
                                change = previous.change ?: ChangeKind.NOTE,
                                note = listOf(previous.note, change.note)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" · "),
                            )
                        }
                    }
                }
            }
        }

    return list.sortedWith(compareBy({ it.startMinutes }, { it.endMinutes }))
}

fun activeLessons(lessons: List<Lesson>): List<Lesson> = lessons.filterNot { it.cancelled }

fun sameLesson(a: Lesson, b: Lesson): Boolean =
    a.time == b.time && a.subject == b.subject && a.change == b.change

/* ============================================================
 *  Works
 * ============================================================ */

fun worksFor(
    works: List<WorkItem>,
    lessonDate: LocalDate,
    subject: String,
    today: LocalDate,
): List<WorkItem> {
    val ns = normalizedSubject(subject)
    return works.filter { work ->
        normalizedSubject(work.subject) == ns &&
            work.date >= today &&
            work.date >= lessonDate &&
            ChronoUnit.DAYS.between(lessonDate, work.date) in 0..7
    }.sortedBy { it.date }
}

fun futureWorksFor(
    works: List<WorkItem>,
    lessonDate: LocalDate,
    subject: String,
    today: LocalDate,
): List<WorkItem> {
    val ns = normalizedSubject(subject)
    return works.filter { work ->
        normalizedSubject(work.subject) == ns &&
            work.date >= today &&
            work.date > lessonDate &&
            ChronoUnit.DAYS.between(lessonDate, work.date) > 7
    }.sortedBy { it.date }
}

fun worksForDate(works: List<WorkItem>, date: LocalDate): List<WorkItem> =
    works.filter { it.date == date }.sortedBy { it.subject }

fun noteChangesBetween(
    changes: List<ScheduleChange>,
    from: LocalDate,
    to: LocalDate,
): List<ScheduleChange> =
    changes.filter {
        it.kind == ChangeKind.NOTE && !it.date.isBefore(from) && !it.date.isAfter(to)
    }.sortedWith(compareBy({ it.date }, { it.time }))

fun worksBetween(works: List<WorkItem>, from: LocalDate, to: LocalDate): List<WorkItem> =
    works.filter { !it.date.isBefore(from) && !it.date.isAfter(to) }.sortedBy { it.date }

/* ============================================================
 *  Subject helpers
 * ============================================================ */

private val parenthesis = Regex("\\([^)]*\\)")

private const val PE = "Физическая культура и здоровье"
private const val PROG = "Программирование"
private const val RADIO_RX = "Радиоприёмные устройства"
private const val OPSIST = "Основы построения систем и сетей телекоммуникаций"
private const val TES = "Теория электрической связи"
private const val CIMPU = "Цифровые микропроцессорные устройства"
private const val METRO = "Метрология и специальные измерения"
private const val NST = "Направляющие системы телекоммуникаций"
private const val RADIO_TX = "Радиопередающие устройства"
private const val PROTECTION = "Защита населения и территорий от чрезвычайных ситуаций"

fun baseSubject(subject: String): String =
    parenthesis.replace(subject, "").replace(Regex("\\s+"), " ").trim()

fun normalizedSubject(subject: String): String =
    baseSubject(subject).lowercase().replace('ё', 'е').replace(Regex("\\s+"), " ").trim()

private val subjectColors = mapOf(
    PROG to Color(0xFF4285F4),
    PE to Color(0xFF2E9E5B),
    RADIO_RX to Color(0xFFF9A825),
    OPSIST to Color(0xFF9C4DF4),
    TES to Color(0xFFE0457B),
    CIMPU to Color(0xFF00897B),
    METRO to Color(0xFFF57C00),
    NST to Color(0xFF5C6BC0),
    RADIO_TX to Color(0xFFE64A45),
    PROTECTION to Color(0xFF8D6E63),
)

private val subjectAbbreviations = mapOf(
    PROG to "Пр",
    PE to "ФК",
    RADIO_RX to "РП",
    OPSIST to "ОС",
    TES to "ТЭ",
    CIMPU to "ЦМ",
    METRO to "МС",
    NST to "НС",
    RADIO_TX to "РПд",
    PROTECTION to "ЗН",
)

private val subjectShortNames = mapOf(
    PROG to "Программирование",
    PE to "Физкультура",
    RADIO_RX to "РПУ",
    OPSIST to "ОПСиСТ",
    TES to "ТЭС",
    CIMPU to "ЦиМПУ",
    METRO to "МиСИ",
    NST to "НСТк",
    RADIO_TX to "РПдУ",
    PROTECTION to "ЗНТ ЧС",
)

fun subjectColor(subject: String): Color =
    subjectColors[baseSubject(subject)] ?: Color(0xFF6750A4)

fun subjectAbbreviation(subject: String): String =
    subjectAbbreviations[baseSubject(subject)] ?: baseSubject(subject).take(2)

fun shortSubject(subject: String): String =
    subjectShortNames[baseSubject(subject)] ?: baseSubject(subject).take(18)

fun allSubjects(snapshot: ScheduleSnapshot): List<String> =
    snapshot.lessons.values.flatten()
        .map { baseSubject(it.subject) }
        .distinct()
        .sorted()

/* ============================================================
 *  Text plurals
 * ============================================================ */

fun pluralPairs(n: Int): String {
    val m10 = n % 10
    val m100 = n % 100
    return when {
        m10 == 1 && m100 != 11 -> "$n пара"
        m10 in 2..4 && m100 !in 11..14 -> "$n пары"
        else -> "$n пар"
    }
}

fun pluralWorks(n: Int): String {
    val m10 = n % 10
    val m100 = n % 100
    return when {
        m10 == 1 && m100 != 11 -> "работа"
        m10 in 2..4 && m100 !in 11..14 -> "работы"
        else -> "работ"
    }
}

fun pluralDays(n: Int): String {
    val m10 = n % 10
    val m100 = n % 100
    return when {
        m10 == 1 && m100 != 11 -> "день"
        m10 in 2..4 && m100 !in 11..14 -> "дня"
        else -> "дней"
    }
}

fun weekLabel(parity: WeekParity): String =
    if (parity == WeekParity.ODD) "нечётная" else "чётная"

/* ============================================================
 *  Live status
 * ============================================================ */

fun computeLiveStatus(now: LocalDateTime, lessons: List<Lesson>): LiveStatus {
    val active = activeLessons(lessons)
    if (active.isEmpty()) return LiveStatus.NoLessons
    val t = nowMinutes(now)
    val sorted = active.sortedBy { it.startMinutes }

    for (lesson in sorted) {
        val start = lesson.startMinutes.toFloat()
        val end = lesson.endMinutes.toFloat()
        val midStart = start + 45f
        val midEnd = start + 55f
        if (t >= start && t < end) {
            val (phase, phaseEnd, phaseTotal) = when {
                t < midStart -> Triple(LessonPhase.FIRST_HALF, midStart, 45f)
                t < midEnd -> Triple(LessonPhase.MID_BREAK, midEnd, 10f)
                else -> Triple(LessonPhase.SECOND_HALF, end, 45f)
            }
            val seconds = ((phaseEnd - t) * 60).toInt().coerceAtLeast(0)
            val progress = ((t - start) / (end - start)).coerceIn(0f, 1f)
            return LiveStatus.InLesson(
                lesson = lesson,
                phase = phase,
                endMinutes = phaseEnd.toInt(),
                totalMinutes = phaseTotal.toInt(),
                secondsLeft = seconds,
                progress = progress,
            )
        }
    }

    val first = sorted.first()
    if (t < first.startMinutes) {
        return LiveStatus.BeforeFirst(
            lesson = first,
            secondsLeft = ((first.startMinutes - t) * 60).toInt().coerceAtLeast(0),
        )
    }

    for (i in sorted.indices) {
        val current = sorted[i]
        val next = sorted.getOrNull(i + 1)
        if (t >= current.endMinutes && (next == null || t < next.startMinutes)) {
            if (next == null) {
                return LiveStatus.Finished(totalToday = sorted.size)
            }
            val gap = next.startMinutes - current.endMinutes
            val seconds = ((next.startMinutes - t) * 60).toInt().coerceAtLeast(0)
            val progress = ((t - current.endMinutes) / gap.toFloat()).coerceIn(0f, 1f)
            return LiveStatus.OnBreak(
                next = next,
                big = gap >= 25,
                endMinutes = next.startMinutes,
                totalMinutes = gap,
                secondsLeft = seconds,
                progress = progress,
            )
        }
    }

    return LiveStatus.Finished(totalToday = sorted.size)
}

fun lessonSlot(lesson: Lesson): TimeSlot? = ScheduleData.slotFor(lesson.time)

fun breakAfter(lesson: Lesson, next: Lesson?): BreakPeriod? {
    if (next == null || lesson.cancelled || next.cancelled) return null
    val slot = lessonSlot(lesson) ?: return null
    return ScheduleData.breakBetween(slot, next.startMinutes)
}

/* ============================================================
 *  Automatic GitHub refresh boundaries
 * ============================================================ */

data class RefreshWindow(
    val latest: ZonedDateTime,
    val next: ZonedDateTime,
)

/** The JSON is refreshed daily at 18:00 and 20:30 local time. */
fun refreshWindow(now: ZonedDateTime): RefreshWindow {
    val zone = now.zone
    val today = now.toLocalDate()
    val at18 = today.atTime(18, 0).atZone(zone)
    val at2030 = today.atTime(20, 30).atZone(zone)
    val latest = when {
        !now.isBefore(at2030) -> at2030
        !now.isBefore(at18) -> at18
        else -> today.minusDays(1).atTime(20, 30).atZone(zone)
    }
    val next = when {
        now.isBefore(at18) -> at18
        now.isBefore(at2030) -> at2030
        else -> today.plusDays(1).atTime(18, 0).atZone(zone)
    }
    return RefreshWindow(latest, next)
}

fun nextRefreshAt(now: ZonedDateTime): ZonedDateTime = refreshWindow(now).next

fun latestRefreshAt(now: ZonedDateTime): ZonedDateTime = refreshWindow(now).latest
