package app.rt521.schedule.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime

class ScheduleTest {

    private val snapshot: ScheduleSnapshot by lazy {
        val file = sequenceOf(
            File("../data/schedule.json"),
            File("data/schedule.json"),
            File("ScheduleRT521/data/schedule.json"),
        ).first { it.exists() }
        ScheduleJsonParser.parse(file.readText())
    }

    @Test
    fun `github json parses and keeps metadata`() {
        assertEquals("РТ-521", snapshot.group)
        assertEquals(5, snapshot.semester)
        assertEquals(LocalDate.of(2026, 9, 28), snapshot.semesterStart)
    }

    @Test
    fun `semester week and parity`() {
        assertEquals(1, semesterWeek(snapshot.semesterStart, LocalDate.of(2026, 9, 28)))
        assertEquals(9, semesterWeek(snapshot.semesterStart, LocalDate.of(2026, 11, 25)))
        assertEquals(
            WeekParity.ODD,
            autoParityFor(snapshot.semesterStart, LocalDate.of(2026, 11, 25)),
        )
    }

    @Test
    fun `new thursday lesson is present`() {
        val lessons = dayLessons(snapshot, LocalDate.of(2026, 11, 26), WeekParity.ODD)
        assertEquals(4, lessons.size)
        assertEquals("09:55-11:35", lessons.first().time)
        assertEquals("Цифровые микропроцессорные устройства", lessons.first().subject)
        assertEquals("155", lessons.first().room)
    }

    @Test
    fun `tsimpu teacher and attendance notes come from github data`() {
        val tuesday = dayLessons(snapshot, LocalDate.of(2026, 12, 1), WeekParity.EVEN)
        val cimpu = tuesday.first { it.subject == "Цифровые микропроцессорные устройства" }
        assertEquals(listOf("Бродунов П.А."), cimpu.teachers)
        assertEquals(ChangeKind.NOTE, cimpu.change)
        assertTrue(cimpu.note.contains("только 1 группа"))

        val thursday = dayLessons(snapshot, LocalDate.of(2026, 12, 3), WeekParity.EVEN)
        val cimpuThursday = thursday.first { it.subject == "Цифровые микропроцессорные устройства" }
        assertTrue(cimpuThursday.note.contains("все группы"))
    }

    @Test
    fun `saturday december twelfth is replaced by kabak rpdu`() {
        val saturday = dayLessons(snapshot, LocalDate.of(2026, 12, 12), WeekParity.ODD)
        val replaced = saturday.first { it.time == "12:15-13:55" }
        assertEquals("Радиопередающие устройства", replaced.subject)
        assertEquals(ChangeKind.REPLACE, replaced.change)
        assertEquals(listOf("Кабак Н.И."), replaced.teachers)
    }

    @Test
    fun `wednesday evening pair was removed`() {
        val lessons = dayLessons(snapshot, LocalDate.of(2026, 11, 25), WeekParity.ODD)
        assertEquals(2, lessons.size)
        assertTrue(lessons.none { it.time == "18:15-19:55" })
        assertTrue(lessons.any { it.subject == "Метрология и специальные измерения" })
        assertTrue(lessons.any { it.subject == "Радиопередающие устройства" })
    }

    @Test
    fun `odd and even parity select correct replacements`() {
        val day = LocalDate.of(2026, 11, 25)
        val odd = dayLessons(snapshot, day, WeekParity.ODD)
        val even = dayLessons(snapshot, day, WeekParity.EVEN)

        assertEquals(2, odd.size)
        assertEquals(2, even.size)
        assertTrue(odd.any { it.subject == "Метрология и специальные измерения" })
        assertTrue(even.any { it.subject == "Направляющие системы телекоммуникаций" })
        assertTrue(odd.none { it.subject == "Направляющие системы телекоммуникаций" })
    }

    @Test
    fun `tuesday and saturday parity split`() {
        assertEquals(4, dayLessons(snapshot, LocalDate.of(2026, 11, 24), WeekParity.ODD).size)
        assertEquals(3, dayLessons(snapshot, LocalDate.of(2026, 11, 24), WeekParity.EVEN).size)
        assertEquals(3, dayLessons(snapshot, LocalDate.of(2026, 11, 28), WeekParity.ODD).size)
        assertEquals(2, dayLessons(snapshot, LocalDate.of(2026, 11, 28), WeekParity.EVEN).size)
    }

    @Test
    fun `friday physical education is odd-week only`() {
        val friday = LocalDate.of(2026, 11, 27)
        val odd = dayLessons(snapshot, friday, WeekParity.ODD)
        val even = dayLessons(snapshot, friday, WeekParity.EVEN)
        assertTrue(odd.any { it.subject == "Физическая культура и здоровье" })
        assertFalse(odd.any { it.subject.startsWith("Основы построения") })
        assertTrue(even.any { it.subject.startsWith("Основы построения") })
        assertFalse(even.any { it.subject == "Физическая культура и здоровье" })
    }

    @Test
    fun `github changes replace cancel and add lessons`() {
        val date = LocalDate.of(2026, 12, 2)
        val custom = snapshot.copy(
            changes = listOf(
                ScheduleChange(
                    date = date,
                    kind = ChangeKind.REPLACE,
                    time = "14:10-15:50",
                    subject = "Практика по телекоммуникациям",
                    room = "101",
                    note = "Замена преподавателя",
                ),
                ScheduleChange(
                    date = date,
                    kind = ChangeKind.EXTRA,
                    time = "18:15-19:55",
                    subject = "Консультация",
                    room = "205",
                ),
                ScheduleChange(
                    date = date,
                    kind = ChangeKind.CANCEL,
                    time = "16:20-18:00",
                    note = "Пара отменена",
                ),
            ),
        )
        val lessons = dayLessons(custom, date, WeekParity.EVEN)
        assertTrue(lessons.any { it.subject == "Практика по телекоммуникациям" && it.change == ChangeKind.REPLACE })
        assertTrue(lessons.any { it.subject == "Консультация" && it.change == ChangeKind.EXTRA })
        assertTrue(lessons.any { it.time == "16:20-18:00" && it.cancelled })
        assertTrue(lessons.any { it.subject == "Радиопередающие устройства" && it.cancelled })
    }

    @Test
    fun `works are shown only near matching lesson`() {
        val today = LocalDate.of(2026, 11, 26)
        val works = listOf(
            WorkItem(today, "Цифровые микропроцессорные устройства", "Контрольная работа", "Двоичные коды"),
            WorkItem(today.plusDays(20), "Цифровые микропроцессорные устройства", "Экзамен", ""),
            WorkItem(today, "Программирование", "Лабораторная", ""),
        )
        val found = worksFor(works, today, "Цифровые микропроцессорные устройства", today)
        assertEquals(1, found.size)
        assertEquals("Двоичные коды", found.first().title)
    }

    @Test
    fun `refresh timer hits 18 and 20 30`() {
        val zone = ZonedDateTime.now().zone
        val morning = ZonedDateTime.of(2026, 11, 26, 9, 0, 0, 0, zone)
        assertEquals(18, nextRefreshAt(morning).hour)
        assertEquals(0, nextRefreshAt(morning).minute)
        assertEquals(20, latestRefreshAt(morning).hour)
        assertEquals(30, latestRefreshAt(morning).minute)

        val evening = ZonedDateTime.of(2026, 11, 26, 19, 0, 0, 0, zone)
        assertEquals(20, nextRefreshAt(evening).hour)
        assertEquals(30, nextRefreshAt(evening).minute)
        assertEquals(18, latestRefreshAt(evening).hour)

        val night = ZonedDateTime.of(2026, 11, 26, 21, 0, 0, 0, zone)
        assertEquals(2026, nextRefreshAt(night).year)
        assertEquals(11, nextRefreshAt(night).monthValue)
        assertEquals(27, nextRefreshAt(night).dayOfMonth)
        assertEquals(18, nextRefreshAt(night).hour)
    }

    @Test
    fun `empty snapshot has no lessons`() {
        val empty = ScheduleJsonParser.emptySnapshot()
        assertTrue(empty.lessons.isEmpty())
        assertNull(empty.updatedAt)
        assertEquals(empty, empty.copy())
    }

    @Test
    fun `plural pairs and countdown`() {
        assertEquals("1 пара", pluralPairs(1))
        assertEquals("2 пары", pluralPairs(2))
        assertEquals("5 пар", pluralPairs(5))
        assertEquals("21 пара", pluralPairs(21))
        assertEquals("02:03", formatCountdown(123))
        assertEquals("1:02:03", formatCountdown(3723))
        assertEquals("00:00", formatCountdown(-5))
    }
}
