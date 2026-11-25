package app.rt521.schedule.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Accepts the convenient GitHub form:
 *   "weeks": "all"
 *   "weeks": [1, 3]
 */
@OptIn(kotlinx.serialization.InternalSerializationApi::class)
object WeeksSerializer : KSerializer<List<Int>?> {
    override val descriptor = buildSerialDescriptor("Weeks", SerialKind.CONTEXTUAL)

    override fun deserialize(decoder: Decoder): List<Int>? {
        val input = decoder as? JsonDecoder
            ?: throw SerializationException("Weeks supports only JSON")
        return when (val element = input.decodeJsonElement()) {
            is JsonNull -> null
            is JsonPrimitive -> {
                if (element.isString && element.content.equals("all", ignoreCase = true)) {
                    null
                } else {
                    throw SerializationException("weeks must be \"all\" or an array of week numbers")
                }
            }
            is JsonArray -> element.map { it.jsonPrimitive.int }
            else -> throw SerializationException("weeks must be \"all\" or an array of week numbers")
        }
    }

    override fun serialize(encoder: Encoder, value: List<Int>?) {
        val output = encoder as? JsonEncoder
            ?: throw SerializationException("Weeks supports only JSON")
        val element = if (value == null) {
            JsonPrimitive("all")
        } else {
            JsonArray(value.map { JsonPrimitive(it) })
        }
        output.encodeJsonElement(element)
    }
}

@Serializable
private data class RemoteScheduleFile(
    val version: Int = 1,
    val group: String = "РТ-521",
    val semester: Int = 5,
    @SerialName("academic_year") val academicYear: String = "2026/2027",
    @SerialName("semester_start") val semesterStart: String,
    @SerialName("updated_at") val updatedAt: String? = null,
    val schedule: Map<String, List<RemoteLesson>> = emptyMap(),
    val changes: List<RemoteChange> = emptyList(),
    val works: List<RemoteWork> = emptyList(),
)

@Serializable
private data class RemoteLesson(
    val time: String,
    val subject: String,
    val room: String = "",
    val teachers: List<String> = emptyList(),
    @Serializable(with = WeeksSerializer::class)
    val weeks: List<Int>? = null,
)

@Serializable
private data class RemoteChange(
    val date: String,
    val kind: String = "replace",
    val time: String,
    val subject: String = "",
    val room: String = "",
    val teachers: List<String> = emptyList(),
    val note: String = "",
)

@Serializable
private data class RemoteWork(
    val date: String,
    val subject: String,
    val type: String = "Работа",
    val title: String = "",
    val note: String = "",
)

object ScheduleJsonParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
        encodeDefaults = true
    }

    private val timeRegex = Regex("""\d{1,2}:\d{2}-\d{1,2}:\d{2}""")

    fun parse(raw: String): ScheduleSnapshot {
        val dto = try {
            json.decodeFromString<RemoteScheduleFile>(raw)
        } catch (e: Exception) {
            throw IllegalArgumentException("Некорректный JSON расписания: ${e.message}", e)
        }

        val semesterStart = try {
            LocalDate.parse(dto.semesterStart)
        } catch (e: Exception) {
            throw IllegalArgumentException("semester_start должен быть датой вида 2026-09-28", e)
        }

        val lessons = mutableMapOf<DayOfWeek, List<Lesson>>()
        dto.schedule.forEach { (dayName, entries) ->
            val day = parseDay(dayName)
            val parsed = entries.map { entry ->
                validateTime(entry.time)
                if (entry.subject.isBlank()) {
                    throw IllegalArgumentException("У пары $dayName ${entry.time} не указан subject")
                }
                if (entry.weeks != null && entry.weeks.isEmpty()) {
                    throw IllegalArgumentException("weeks не может быть пустым массивом")
                }
                Lesson(
                    time = entry.time,
                    subject = entry.subject,
                    room = entry.room,
                    teachers = entry.teachers,
                    weeks = entry.weeks,
                )
            }
            lessons[day] = parsed
        }

        val changes = dto.changes.map { change ->
            validateTime(change.time)
            ScheduleChange(
                date = parseDate(change.date, "changes.date"),
                kind = parseKind(change.kind),
                time = change.time,
                subject = change.subject,
                room = change.room,
                teachers = change.teachers,
                note = change.note,
            )
        }

        val works = dto.works.map { work ->
            if (work.subject.isBlank()) {
                throw IllegalArgumentException("У работы ${work.date} не указан subject")
            }
            WorkItem(
                date = parseDate(work.date, "works.date"),
                subject = work.subject,
                type = work.type,
                title = work.title,
                note = work.note,
            )
        }

        return ScheduleSnapshot(
            group = dto.group.ifBlank { "РТ-521" },
            semester = dto.semester,
            academicYear = dto.academicYear,
            semesterStart = semesterStart,
            updatedAt = parseUpdatedAt(dto.updatedAt),
            lessons = lessons,
            changes = changes,
            works = works,
        )
    }

    fun emptySnapshot(): ScheduleSnapshot = ScheduleSnapshot(
        group = "РТ-521",
        semester = 5,
        academicYear = "2026/2027",
        semesterStart = LocalDate.of(2026, 9, 28),
        updatedAt = null,
        lessons = emptyMap(),
        changes = emptyList(),
        works = emptyList(),
    )

    private fun parseDay(value: String): DayOfWeek = try {
        DayOfWeek.valueOf(value.trim().uppercase())
    } catch (e: Exception) {
        throw IllegalArgumentException("Неизвестный день недели: $value. Используйте monday…sunday", e)
    }

    private fun parseDate(value: String, field: String): LocalDate = try {
        LocalDate.parse(value)
    } catch (e: Exception) {
        throw IllegalArgumentException("$field должен быть датой вида 2026-12-01", e)
    }

    private fun parseKind(value: String): ChangeKind = when (value.trim().lowercase()) {
        "replace", "replacement", "замена" -> ChangeKind.REPLACE
        "extra", "add", "доп", "дополнительная" -> ChangeKind.EXTRA
        "cancel", "cancelled", "отмена" -> ChangeKind.CANCEL
        else -> throw IllegalArgumentException(
            "Неизвестный kind изменения: $value. Допустимо: replace, extra, cancel",
        )
    }

    private fun validateTime(value: String) {
        if (!timeRegex.matches(value)) {
            throw IllegalArgumentException("Время должно быть в формате 14:10-15:50, получено: $value")
        }
        val start = parseTime(value.substringBefore('-'))
        val end = parseTime(value.substringAfter('-'))
        if (end <= start) {
            throw IllegalArgumentException("Конец пары должен быть позже начала: $value")
        }
    }

    private fun parseUpdatedAt(value: String?): Instant? {
        if (value.isNullOrBlank()) return null
        return runCatching { Instant.parse(value) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
            ?: runCatching {
                ZonedDateTime.of(
                    java.time.LocalDateTime.parse(value.replace(' ', 'T')),
                    ZoneId.systemDefault(),
                ).toInstant()
            }.getOrNull()
    }
}
