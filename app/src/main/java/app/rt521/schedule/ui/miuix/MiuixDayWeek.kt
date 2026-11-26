package app.rt521.schedule.ui.miuix

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.rt521.schedule.data.AppState
import app.rt521.schedule.data.ChangeKind
import app.rt521.schedule.data.Lesson
import app.rt521.schedule.data.LessonPhase
import app.rt521.schedule.data.LiveStatus
import app.rt521.schedule.data.ScheduleChange
import app.rt521.schedule.data.WorkItem
import app.rt521.schedule.data.activeLessons
import app.rt521.schedule.data.breakAfter
import app.rt521.schedule.data.computeLiveStatus
import app.rt521.schedule.data.dayLessons
import app.rt521.schedule.data.dayName
import app.rt521.schedule.data.dayShort
import app.rt521.schedule.data.formatCountdown
import app.rt521.schedule.data.formatDateMedium
import app.rt521.schedule.data.formatDateTime
import app.rt521.schedule.data.futureWorksFor
import app.rt521.schedule.data.noteChangesBetween
import app.rt521.schedule.data.monthGenitive
import app.rt521.schedule.data.pluralDays
import app.rt521.schedule.data.pluralPairs
import app.rt521.schedule.data.pluralWorks
import app.rt521.schedule.data.relativeDayLabel
import app.rt521.schedule.data.semesterWeek
import app.rt521.schedule.data.weekLabel
import app.rt521.schedule.data.worksBetween
import app.rt521.schedule.data.worksFor
import app.rt521.schedule.data.worksForDate
import app.rt521.schedule.ui.screens.mondayOf
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Location
import top.yukonga.miuix.kmp.icon.extended.Timer
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/* ============================================================
 *  Day
 * ============================================================ */

@Composable
fun MiuixDayScreen(
    state: AppState,
    today: LocalDate,
    innerPadding: PaddingValues,
    scrollBehavior: ScrollBehavior,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    onToast: (String) -> Unit,
) {
    val snapshot = state.schedule
    val selected = state.selectedDate
    val parity = state.parityFor(selected)
    val lessons = dayLessons(snapshot, selected, parity)
    val active = activeLessons(lessons)
    val live = if (selected == today) computeLiveStatus(state.now, lessons) else null
    val monday = mondayOf(selected)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(
            start = 12.dp,
            top = innerPadding.calculateTopPadding() + 8.dp,
            end = 12.dp,
            bottom = innerPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "weekHeader") {
            MiuixWeekHeader(
                monday = monday,
                semesterStart = snapshot.semesterStart,
                parityText = "${semesterWeek(snapshot.semesterStart, monday)}-я неделя · ${weekLabel(parity)}",
                onPrev = { state.selectDate(selected.minusWeeks(1)) },
                onNext = { state.selectDate(selected.plusWeeks(1)) },
                onToday = if (selected != today) ({ state.selectDate(today) }) else null,
            )
        }

        item(key = "dateTabs") {
            MiuixDateTabs(
                selected = selected,
                today = today,
                works = snapshot.works,
                onSelect = { state.selectDate(it) },
            )
        }

        item(key = "hero") {
            MiuixStatusCard(
                state = state,
                date = selected,
                today = today,
                lessons = lessons,
                live = live,
                works = worksForDate(snapshot.works, selected),
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        if (state.lastError != null) {
            item(key = "error") {
                MiuixErrorCard(text = state.lastError.orEmpty(), onRetry = onRefresh)
            }
        }

        item(key = "section") {
            SmallTitle(
                text = if (selected == today) "Расписание на сегодня" else "Расписание дня",
                insideMargin = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            )
        }

        if (active.isEmpty()) {
            item(key = "empty") {
                MiuixEmptyCard(
                    title = if (selected.dayOfWeek == DayOfWeek.SUNDAY) "Воскресенье" else "Пар нет",
                    summary = if (selected.dayOfWeek == DayOfWeek.SUNDAY) {
                        "Выходной — отдыхаем и набираемся сил"
                    } else {
                        "По текущей чётности недели занятий в этот день нет"
                    },
                )
            }
        } else {
            lessons.forEachIndexed { index, entry ->
                if (entry.cancelled) {
                    item(key = "lesson-cancel-$index") {
                        MiuixLessonCard(
                            entry = entry,
                            index = lessons.take(index).count { !it.cancelled } + 1,
                            today = today,
                            isCurrent = false,
                            live = null,
                            works = emptyList(),
                            futureWorks = emptyList(),
                            date = selected,
                        )
                    }
                } else {
                    val isCurrent = live is LiveStatus.InLesson &&
                        live.lesson.time == entry.time && live.lesson.subject == entry.subject
                    item(key = "lesson-$index") {
                        MiuixLessonCard(
                            entry = entry,
                            index = active.indexOf(entry) + 1,
                            today = today,
                            isCurrent = isCurrent,
                            live = if (isCurrent) live else null,
                            works = worksFor(snapshot.works, selected, entry.subject, today),
                            futureWorks = futureWorksFor(snapshot.works, selected, entry.subject, today),
                            date = selected,
                        )
                    }
                    val nextActive = lessons.drop(index + 1).firstOrNull { !it.cancelled }
                    val breakPeriod = breakAfter(entry, nextActive)
                    if (breakPeriod != null) {
                        item(key = "break-$index") {
                            MiuixBreakRow(
                                range = breakPeriod.rangeLabel,
                                big = breakPeriod.big,
                                isCurrent = selected == today &&
                                    state.now.toLocalTime().toSecondOfDay() in
                                    breakPeriod.start * 60 until breakPeriod.end * 60,
                            )
                        }
                    }
                }
            }
        }

        if (selected.dayOfWeek == DayOfWeek.SUNDAY) {
            val nextMonday = selected.plusDays(1)
            val nextSunday = selected.plusDays(7)
            val nextWorks = worksBetween(snapshot.works, nextMonday, nextSunday)
            val nextNotes = noteChangesBetween(snapshot.changes, nextMonday, nextSunday)
            item(key = "nextWeekTitle") {
                SmallTitle(
                    text = "На следующую неделю",
                    insideMargin = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                )
            }
            item(key = "nextWeek") {
                MiuixNextWeekCard(
                    works = nextWorks,
                    notes = nextNotes,
                    today = today,
                )
            }
        }

        item(key = "footer") {
            MiuixFooter(
                text = "Обновлено ${formatDateTime(snapshot.updatedAt ?: state.lastFetchAt)}",
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun MiuixWeekHeader(
    monday: LocalDate,
    semesterStart: LocalDate,
    parityText: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: (() -> Unit)?,
) {
    val colors = MiuixTheme.colorScheme
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrev) {
                Icon(
                    MiuixIcons.ChevronBackward,
                    contentDescription = "Предыдущая неделя",
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    weekRangeLabelMiuix(monday),
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Medium,
                    color = colors.onBackground,
                )
                Text(
                    parityText,
                    style = MiuixTheme.textStyles.footnote1,
                    color = colors.onBackgroundVariant,
                )
            }
            IconButton(onClick = onNext) {
                Icon(
                    MiuixIcons.ChevronForward,
                    contentDescription = "Следующая неделя",
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        if (onToday != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(text = "Сегодня", onClick = onToday)
            }
        }
    }
}

private fun weekRangeLabelMiuix(monday: LocalDate): String {
    val sunday = monday.plusDays(6)
    return if (monday.month == sunday.month) {
        "${monday.dayOfMonth} – ${sunday.dayOfMonth} ${monthGenitive(monday.month)}"
    } else {
        "${formatDateMedium(monday)} – ${formatDateMedium(sunday)}"
    }
}

@Composable
private fun MiuixDateTabs(
    selected: LocalDate,
    today: LocalDate,
    works: List<WorkItem>,
    onSelect: (LocalDate) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val monday = mondayOf(selected)
    val dates = (0L..6L).map { monday.plusDays(it) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        dates.forEach { date ->
            val isSelected = date == selected
            val isToday = date == today
            val hasWork = works.any { it.date == date }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isSelected) colors.surfaceContainer else Color.Transparent)
                    .clickable { onSelect(date) }
                    .padding(vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    dayShort(date.dayOfWeek),
                    style = MiuixTheme.textStyles.footnote2,
                    color = if (isSelected) colors.onSurfaceContainer else colors.onSurfaceVariantSummary,
                )
                Text(
                    date.dayOfMonth.toString(),
                    style = MiuixTheme.textStyles.body2,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) colors.onSurfaceContainer else colors.onBackground,
                )
                when {
                    hasWork -> Box(
                        Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(colors.error),
                    )
                    isToday -> Box(
                        Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Color(0xFFFF9500)),
                    )
                    else -> Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

/* ============================================================
 *  Status card
 * ============================================================ */

private data class MiuixHero(
    val label: String,
    val title: String,
    val meta: String,
    val countdown: String? = null,
    val suffix: String? = null,
    val progress: Float? = null,
    val startLabel: String? = null,
    val endLabel: String? = null,
    val live: Boolean = false,
)

@Composable
private fun MiuixStatusCard(
    state: AppState,
    date: LocalDate,
    today: LocalDate,
    lessons: List<Lesson>,
    live: LiveStatus?,
    works: List<WorkItem>,
    modifier: Modifier = Modifier,
) {
    val isToday = date == today
    val tomorrow = date.plusDays(1)
    val tomorrowFirst = dayLessons(state.schedule, tomorrow, state.parityFor(tomorrow))
        .firstOrNull { !it.cancelled }

    val hero = when {
        !isToday -> {
            val active = lessons.filterNot { it.cancelled }
            MiuixHero(
                label = relativeDayLabel(date, today),
                title = if (active.isEmpty()) "Пар нет" else pluralPairs(active.size),
                meta = if (active.isEmpty()) {
                    if (date.dayOfWeek == DayOfWeek.SUNDAY) "Выходной" else "Свободный день"
                } else {
                    "с ${active.first().time.substringBefore('-')} до ${active.last().time.substringAfter('-')}" +
                        if (works.isNotEmpty()) " · ${works.size} ${pluralWorks(works.size)}" else ""
                },
            )
        }

        live == null -> MiuixHero(
            label = "Сегодня",
            title = "Пар нет",
            meta = tomorrowFirst?.let { "Завтра: ${it.subject} в ${it.time.substringBefore('-')}" }
                ?: "Свободный день",
        )

        live is LiveStatus.NoLessons -> MiuixHero(
            label = relativeDayLabel(date, today),
            title = if (date.dayOfWeek == DayOfWeek.SUNDAY) "Воскресенье" else "Пар нет",
            meta = tomorrowFirst?.let { "Завтра: ${it.subject} в ${it.time.substringBefore('-')}" }
                ?: "Пар нет — отдыхаем",
        )

        live is LiveStatus.BeforeFirst -> MiuixHero(
            label = "До первой пары",
            title = live.lesson.subject,
            meta = "каб. ${live.lesson.room} · ${live.lesson.time}",
            countdown = formatCountdown(live.secondsLeft),
            suffix = "до начала",
            startLabel = live.lesson.time.substringBefore('-'),
            endLabel = live.lesson.time.substringAfter('-'),
            live = true,
        )

        live is LiveStatus.InLesson -> MiuixHero(
            label = when (live.phase) {
                LessonPhase.FIRST_HALF -> "Идёт пара · 1-я половина"
                LessonPhase.MID_BREAK -> "Перерыв внутри пары"
                LessonPhase.SECOND_HALF -> "Идёт пара · 2-я половина"
            },
            title = live.lesson.subject,
            meta = buildString {
                append("каб. ${live.lesson.room} · ${live.lesson.time}")
                if (live.lesson.note.isNotBlank()) append(" · ${live.lesson.note}")
            },
            countdown = formatCountdown(live.secondsLeft),
            suffix = when (live.phase) {
                LessonPhase.FIRST_HALF -> "до перерыва"
                LessonPhase.MID_BREAK -> "до конца перерыва"
                LessonPhase.SECOND_HALF -> "до конца пары"
            },
            progress = live.progress,
            startLabel = live.lesson.time.substringBefore('-'),
            endLabel = live.lesson.time.substringAfter('-'),
            live = true,
        )

        live is LiveStatus.OnBreak -> MiuixHero(
            label = if (live.big) "Большой перерыв" else "Перерыв",
            title = live.next?.let { "Далее: ${it.subject}" } ?: "Перерыв",
            meta = live.next?.let { "каб. ${it.room} · ${it.time}" } ?: "Отдыхаем",
            countdown = formatCountdown(live.secondsLeft),
            suffix = "до начала",
            progress = live.progress,
            endLabel = app.rt521.schedule.data.formatTime(live.endMinutes),
            live = true,
        )

        live is LiveStatus.Finished -> MiuixHero(
            label = "Сегодня",
            title = "Пары закончились",
            meta = buildString {
                append("Было ${pluralPairs(live.totalToday)}")
                if (tomorrowFirst != null) {
                    append(" · завтра ${tomorrowFirst.subject} в ${tomorrowFirst.time.substringBefore('-')}")
                }
            },
        )

        else -> MiuixHero("Сегодня", "Пар нет", "")
    }

    val colors = MiuixTheme.colorScheme
    val contentColor = if (hero.live) colors.onTertiaryContainer else colors.onSurfaceContainer
    val cardColors = if (hero.live) {
        CardDefaults.defaultColors(
            color = colors.tertiaryContainer,
            contentColor = colors.onTertiaryContainer,
        )
    } else {
        CardDefaults.defaultColors()
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = cardColors,
        insideMargin = PaddingValues(18.dp),
        cornerRadius = 20.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (hero.live) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(androidx.compose.ui.graphics.Color(0xFFFF9500)),
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                hero.label,
                style = MiuixTheme.textStyles.footnote1,
                color = contentColor.copy(alpha = 0.75f),
            )
        }
        Text(
            hero.title,
            style = MiuixTheme.textStyles.title3,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            modifier = Modifier.padding(top = 7.dp),
        )
        Text(
            hero.meta,
            style = MiuixTheme.textStyles.body2,
            color = contentColor.copy(alpha = 0.72f),
            modifier = Modifier.padding(top = 3.dp),
        )
        if (hero.countdown != null) {
            Row(
                Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    hero.countdown,
                    style = MiuixTheme.textStyles.title1,
                    fontWeight = FontWeight.Medium,
                    color = contentColor,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    hero.suffix.orEmpty(),
                    style = MiuixTheme.textStyles.footnote1,
                    color = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
        if (hero.progress != null) {
            LinearProgressIndicator(
                progress = hero.progress,
                height = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
        }
        if (hero.startLabel != null || hero.endLabel != null) {
            Row(Modifier.fillMaxWidth().padding(top = 5.dp)) {
                Text(
                    hero.startLabel.orEmpty(),
                    style = MiuixTheme.textStyles.footnote2,
                    color = contentColor.copy(alpha = 0.62f),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    hero.endLabel.orEmpty(),
                    style = MiuixTheme.textStyles.footnote2,
                    color = contentColor.copy(alpha = 0.62f),
                )
            }
        }
        if (works.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                works.take(2).forEach { work ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (hero.live) contentColor.copy(alpha = 0.10f) else colors.tertiaryContainer,
                        contentColor = if (hero.live) contentColor else colors.onTertiaryContainer,
                    ) {
                        Text(
                            work.displayTitle,
                            style = MiuixTheme.textStyles.footnote2,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (works.size > 2) {
                    Text(
                        "+${works.size - 2}",
                        style = MiuixTheme.textStyles.footnote2,
                        color = contentColor.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

/* ============================================================
 *  Lesson cards
 * ============================================================ */

@Composable
private fun MiuixLessonCard(
    entry: Lesson,
    index: Int,
    date: LocalDate,
    today: LocalDate,
    isCurrent: Boolean,
    live: LiveStatus?,
    works: List<WorkItem>,
    futureWorks: List<WorkItem> = emptyList(),
) {
    val colors = MiuixTheme.colorScheme
    var worksExpanded by remember(date, entry.time, entry.subject) { mutableStateOf(false) }
    val cancelled = entry.cancelled
    val cardColors = when {
        cancelled -> CardDefaults.defaultColors(
            color = colors.errorContainer,
            contentColor = colors.onErrorContainer,
        )
        isCurrent -> CardDefaults.defaultColors(
            color = colors.tertiaryContainer,
            contentColor = colors.onTertiaryContainer,
        )
        else -> CardDefaults.defaultColors()
    }
    val content = cardColors.contentColor

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (futureWorks.isNotEmpty()) {
                    Modifier.clickable { worksExpanded = !worksExpanded }
                } else {
                    Modifier
                },
            ),
        colors = cardColors,
        insideMargin = PaddingValues(16.dp),
        cornerRadius = 16.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Пара $index",
                style = MiuixTheme.textStyles.footnote1,
                color = if (isCurrent) colors.primary else content.copy(alpha = 0.6f),
            )
            entry.change?.let { kind ->
                Spacer(Modifier.width(6.dp))
                MiuixBadge(
                    text = when (kind) {
                        ChangeKind.REPLACE -> "Замена"
                        ChangeKind.EXTRA -> "Доп. пара"
                        ChangeKind.CANCEL -> "Отменена"
                        ChangeKind.NOTE -> "Примечание"
                    },
                    error = kind == ChangeKind.CANCEL,
                )
            }
            if (entry.weeks != null) {
                Spacer(Modifier.width(6.dp))
                MiuixBadge("${entry.weeks.joinToString(",")} нед.")
            }
            if (isCurrent) {
                Spacer(Modifier.weight(1f))
                Text(
                    "Сейчас",
                    style = MiuixTheme.textStyles.footnote1,
                    color = colors.primary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Row(
            Modifier.padding(top = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                entry.subject,
                style = MiuixTheme.textStyles.title4,
                fontWeight = FontWeight.Medium,
                color = content,
                textDecoration = if (cancelled) TextDecoration.LineThrough else TextDecoration.None,
                modifier = Modifier.weight(1f),
            )
            if (futureWorks.isNotEmpty()) {
                Spacer(Modifier.width(7.dp))
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(androidx.compose.ui.graphics.Color(0xFFFF9500)),
                )
            }
        }
        if (entry.teachers.isNotEmpty() && !cancelled) {
            Text(
                entry.teachers.joinToString(", "),
                style = MiuixTheme.textStyles.body2,
                color = content.copy(alpha = 0.65f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Row(
            Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                MiuixIcons.Timer,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = content.copy(alpha = 0.55f),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                entry.time,
                style = MiuixTheme.textStyles.body2,
                color = content.copy(alpha = 0.75f),
            )
            Spacer(Modifier.width(12.dp))
            Icon(
                MiuixIcons.Location,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = content.copy(alpha = 0.55f),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "каб. ${entry.room.ifBlank { "—" }}",
                style = MiuixTheme.textStyles.body2,
                color = content.copy(alpha = 0.75f),
            )
        }
        if (entry.note.isNotBlank()) {
            Text(
                entry.note,
                style = MiuixTheme.textStyles.footnote1,
                color = content.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = 7.dp),
            )
        }
        works.forEach { work ->
            MiuixWorkCard(
                work = work,
                reference = date,
                today = today,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (worksExpanded) {
            futureWorks.forEach { work ->
                MiuixWorkCard(
                    work = work,
                    reference = date,
                    today = today,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        if (isCurrent && live is LiveStatus.InLesson && !cancelled) {
            LinearProgressIndicator(
                progress = live.progress,
                height = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun MiuixBadge(text: String, error: Boolean = false) {
    val colors = MiuixTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (error) colors.errorContainer else colors.tertiaryContainer,
        contentColor = if (error) colors.onErrorContainer else colors.onTertiaryContainer,
    ) {
        Text(
            text,
            style = MiuixTheme.textStyles.footnote2,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            maxLines = 1,
        )
    }
}

@Composable
private fun MiuixWorkCard(
    work: WorkItem,
    reference: LocalDate,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    val days = ChronoUnit.DAYS.between(reference, work.date).toInt()
    val urgent = days <= 1
    val bg = if (urgent) colors.errorContainer else colors.tertiaryContainer
    val fg = if (urgent) colors.onErrorContainer else colors.onTertiaryContainer
    val whenText = when {
        days == 0 && reference == today -> "Сегодня"
        days == 0 -> "В этот день"
        days == 1 && reference == today -> "Завтра"
        days == 1 -> "На следующий день"
        else -> "Через $days ${pluralDays(days)}"
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = bg,
        contentColor = fg,
    ) {
        BasicComponent(
            title = "$whenText: ${work.displayTitle}",
            titleColor = BasicComponentDefaults.titleColor(color = fg),
            summary = listOf(work.type, work.note).filter { it.isNotBlank() }.joinToString(" · "),
            summaryColor = BasicComponentDefaults.summaryColor(color = fg.copy(alpha = 0.72f)),
            startAction = {
                Icon(
                    MiuixIcons.Info,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(18.dp),
                )
            },
            insideMargin = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun MiuixBreakRow(range: String, big: Boolean, isCurrent: Boolean) {
    val colors = MiuixTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            range,
            style = MiuixTheme.textStyles.footnote2,
            color = if (isCurrent) colors.primary else colors.onBackgroundVariant,
        )
        HorizontalDivider(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            color = colors.dividerLine,
        )
        Text(
            if (big) "Большой перерыв" else "Перерыв",
            style = MiuixTheme.textStyles.footnote2,
            color = if (isCurrent) colors.primary else colors.onBackgroundVariant,
        )
    }
}

@Composable
internal fun MiuixEmptyCard(title: String, summary: String) {
    val colors = MiuixTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(20.dp),
    ) {
        Text(
            title,
            style = MiuixTheme.textStyles.title4,
            fontWeight = FontWeight.Medium,
            color = colors.onSurfaceContainer,
        )
        Text(
            summary,
            style = MiuixTheme.textStyles.body2,
            color = colors.onSurfaceContainerVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
internal fun MiuixErrorCard(text: String, onRetry: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.errorContainer,
        contentColor = colors.onErrorContainer,
    ) {
        BasicComponent(
            title = "Не удалось обновить",
            titleColor = BasicComponentDefaults.titleColor(color = colors.onErrorContainer),
            summary = text,
            summaryColor = BasicComponentDefaults.summaryColor(color = colors.onErrorContainer.copy(alpha = 0.75f)),
            startAction = {
                Icon(
                    MiuixIcons.Info,
                    contentDescription = null,
                    tint = colors.error,
                    modifier = Modifier.size(18.dp),
                )
            },
            endActions = {
                TextButton(text = "Повторить", onClick = onRetry)
            },
            insideMargin = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun MiuixFooter(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MiuixTheme.textStyles.footnote2,
        color = MiuixTheme.colorScheme.onBackgroundVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

/* ============================================================
 *  Week
 * ============================================================ */

@Composable
fun MiuixWeekScreen(
    state: AppState,
    today: LocalDate,
    innerPadding: PaddingValues,
    scrollBehavior: ScrollBehavior,
    onOpenDay: (LocalDate) -> Unit,
    onRefresh: () -> Unit,
    onToast: (String) -> Unit,
) {
    val snapshot = state.schedule
    var weekBaseEpoch by rememberSaveable {
        mutableLongStateOf(mondayOf(state.selectedDate).toEpochDay())
    }
    val weekBase = LocalDate.ofEpochDay(weekBaseEpoch)
    val parity = state.parityFor(weekBase)
    val days = (0L..6L).map { weekBase.plusDays(it) }
    val weekWorks = worksBetween(snapshot.works, weekBase, weekBase.plusDays(6))
        .filter { !it.date.isBefore(today) }
    val currentMonday = mondayOf(today)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(
            start = 12.dp,
            top = innerPadding.calculateTopPadding() + 8.dp,
            end = 12.dp,
            bottom = innerPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "weekHeader") {
            MiuixWeekHeader(
                monday = weekBase,
                semesterStart = snapshot.semesterStart,
                parityText = "${semesterWeek(snapshot.semesterStart, weekBase)}-я неделя · ${weekLabel(parity)}",
                onPrev = { weekBaseEpoch -= 7 },
                onNext = { weekBaseEpoch += 7 },
                onToday = if (weekBase != currentMonday) ({
                    weekBaseEpoch = currentMonday.toEpochDay()
                    state.selectDate(today)
                }) else null,
            )
        }

        if (state.lastError != null) {
            item(key = "error") {
                MiuixErrorCard(text = state.lastError.orEmpty(), onRetry = onRefresh)
            }
        }

        if (weekWorks.isNotEmpty()) {
            item(key = "worksTitle") {
                SmallTitle(
                    text = "Работы на неделе",
                    insideMargin = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                )
            }
            item(key = "works") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    weekWorks.forEachIndexed { index, work ->
                        if (index > 0) HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        MiuixWorkRow(work = work, today = today)
                    }
                }
            }
        }

        item(key = "daysTitle") {
            SmallTitle(
                text = "Дни недели",
                insideMargin = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            )
        }

        days.forEachIndexed { index, date ->
            val lessons = dayLessons(snapshot, date, parity)
            item(key = "day-$date") {
                MiuixWeekDayCard(
                    date = date,
                    today = today,
                    lessons = lessons,
                    initiallyExpanded = date == today || (today !in days && index == 0),
                    onOpenDay = { onOpenDay(date) },
                )
            }
        }

        item(key = "footer") {
            MiuixFooter(
                text = "Обновлено ${formatDateTime(snapshot.updatedAt ?: state.lastFetchAt)}",
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun MiuixWorkRow(work: WorkItem, today: LocalDate) {
    val colors = MiuixTheme.colorScheme
    val days = ChronoUnit.DAYS.between(today, work.date).toInt()
    BasicComponent(
        title = work.displayTitle,
        summary = "${formatDateMedium(work.date)} · ${work.type}" +
            if (work.note.isNotBlank()) " · ${work.note}" else "",
        startAction = {
            Icon(
                MiuixIcons.Info,
                contentDescription = null,
                tint = if (days <= 1) colors.error else colors.primary,
                modifier = Modifier.size(18.dp),
            )
        },
        endActions = {
            if (days == 0) {
                Text("сегодня", style = MiuixTheme.textStyles.footnote2, color = colors.error)
            } else if (days == 1) {
                Text("завтра", style = MiuixTheme.textStyles.footnote2, color = colors.error)
            }
        },
    )
}

@Composable
private fun MiuixWeekDayCard(
    date: LocalDate,
    today: LocalDate,
    lessons: List<Lesson>,
    initiallyExpanded: Boolean,
    onOpenDay: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    var expanded by rememberSaveable(date.toEpochDay()) { mutableStateOf(initiallyExpanded) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    val activeCount = lessons.count { !it.cancelled }
    val isToday = date == today

    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(0.dp),
    ) {
        BasicComponent(
            title = dayName(date.dayOfWeek),
            summary = "${date.dayOfMonth} ${monthGenitive(date.month)}" + if (isToday) " · сегодня" else "",
            onClick = { expanded = !expanded },
            endActions = {
                Text(
                    if (activeCount == 0) "нет пар" else pluralPairs(activeCount),
                    style = MiuixTheme.textStyles.footnote1,
                    color = colors.onBackgroundVariant,
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    MiuixIcons.ChevronForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp).rotate(90f + rotation),
                    tint = colors.onBackgroundVariant,
                )
            },
        )
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column {
                HorizontalDivider()
                if (lessons.isEmpty()) {
                    BasicComponent(
                        title = if (date.dayOfWeek == DayOfWeek.SUNDAY) "Выходной" else "Пар нет",
                        summary = if (date.dayOfWeek == DayOfWeek.SUNDAY) "Отдыхаем" else "Занятий нет",
                        startAction = {
                            Icon(
                                MiuixIcons.Info,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = colors.onBackgroundVariant,
                            )
                        },
                    )
                } else {
                    lessons.forEachIndexed { lessonIndex, lesson ->
                        if (lessonIndex > 0) HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        MiuixWeekLessonRow(lesson = lesson, onOpen = onOpenDay)
                    }
                }
            }
        }
    }
}

@Composable
private fun MiuixWeekLessonRow(lesson: Lesson, onOpen: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    val accent = if (lesson.cancelled) colors.error else colors.primary
    BasicComponent(
        title = lesson.subject,
        titleColor = BasicComponentDefaults.titleColor(
            color = if (lesson.cancelled) colors.error else colors.onBackground,
        ),
        summary = buildString {
            append("каб. ${lesson.room} · ${lesson.time}")
            if (lesson.weeks != null) append(" · ${lesson.weeks.joinToString(",")} нед.")
            if (lesson.note.isNotBlank()) append(" · ${lesson.note}")
        },
        summaryColor = BasicComponentDefaults.summaryColor(color = colors.onBackgroundVariant),
        startAction = {
            Box(
                Modifier
                    .width(3.dp)
                    .height(30.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
        },
        endActions = {
            when (lesson.change) {
                ChangeKind.REPLACE -> MiuixBadge("Замена")
                ChangeKind.EXTRA -> MiuixBadge("Доп.")
                ChangeKind.CANCEL -> MiuixBadge("Отмена", error = true)
                ChangeKind.NOTE -> MiuixBadge("Прим.")
                null -> Unit
            }
        },
        onClick = onOpen,
    )
}


@Composable
private fun MiuixNextWeekCard(
    works: List<WorkItem>,
    notes: List<ScheduleChange>,
    today: LocalDate,
) {
    val colors = MiuixTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(0.dp),
        cornerRadius = 16.dp,
    ) {
        if (works.isEmpty() && notes.isEmpty()) {
            Text(
                "Работ и заметок пока нет",
                style = MiuixTheme.textStyles.footnote1,
                color = colors.onBackgroundVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
        works.forEachIndexed { index, work ->
            if (index > 0) HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
            MiuixWorkRow(work = work, today = today)
        }
        notes.forEach { note ->
            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
            BasicComponent(
                title = "${note.subject.ifBlank { "Примечание" }} · ${formatDateMedium(note.date)}",
                summary = note.note,
                startAction = {
                    Icon(
                        MiuixIcons.Info,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }
    }
}
