package app.rt521.schedule.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.rt521.schedule.data.AppState
import app.rt521.schedule.data.ChangeKind
import app.rt521.schedule.data.ThemeStyle
import app.rt521.schedule.data.WorkItem
import app.rt521.schedule.data.dayLessons
import app.rt521.schedule.data.dayName
import app.rt521.schedule.data.formatDateMedium
import app.rt521.schedule.data.formatDateTime
import app.rt521.schedule.data.monthGenitive
import app.rt521.schedule.data.pluralPairs
import app.rt521.schedule.data.subjectColor
import app.rt521.schedule.data.semesterWeek
import app.rt521.schedule.data.worksBetween
import app.rt521.schedule.ui.BottomMetrics
import app.rt521.schedule.ui.components.SectionLabel
import app.rt521.schedule.ui.components.SubjectGlyph
import app.rt521.schedule.ui.theme.LocalDesign
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun WeekScreen(
    state: AppState,
    today: LocalDate,
    metric: BottomMetrics,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onToast: (String) -> Unit,
) {
    val d = LocalDesign.current
    val snapshot = state.schedule
    var weekBaseEpoch by rememberSaveable {
        mutableLongStateOf(mondayOf(state.selectedDate).toEpochDay())
    }
    val weekBase = LocalDate.ofEpochDay(weekBaseEpoch)

    LaunchedEffect(state.selectedDate) {
        val selectedMonday = mondayOf(state.selectedDate)
        if (selectedMonday.toEpochDay() != weekBaseEpoch) {
            weekBaseEpoch = selectedMonday.toEpochDay()
        }
    }

    val parity = state.parityFor(weekBase)
    val days = (0L..6L).map { weekBase.plusDays(it) }
    val weekWorks = worksBetween(snapshot.works, weekBase, weekBase.plusDays(6))
    val currentMonday = mondayOf(today)

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = metric.contentPadding),
    ) {
        ScreenHeader(
            group = snapshot.group,
            semester = snapshot.semester,
            title = "Неделя",
            subtitle = "Обзор недели · ${snapshot.academicYear}",
            onTheme = onOpenSettings,
            onRefresh = onRefresh,
            isRefreshing = state.isRefreshing,
        )

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ParityPill(state, weekBase, onToast)
            Spacer(Modifier.weight(1f))
            if (state.parityIsManual) {
                Text("вручную", style = d.type.labelSmall, color = d.colors.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(10.dp))
        WeekNavigator(
            monday = weekBase,
            semesterStart = snapshot.semesterStart,
            onPrev = { weekBaseEpoch -= 7 },
            onNext = { weekBaseEpoch += 7 },
            onToday = if (weekBase != currentMonday) {
                {
                    weekBaseEpoch = currentMonday.toEpochDay()
                    state.selectDate(today)
                }
            } else null,
        )

        if (state.lastError != null) {
            UpdateErrorBanner(
                text = state.lastError.orEmpty(),
                onRetry = onRefresh,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Spacer(Modifier.height(18.dp))
        if (weekWorks.isNotEmpty()) {
            WeekWorksCard(weekWorks, today)
            Spacer(Modifier.height(18.dp))
        }

        SectionLabel("Дни недели")
        Spacer(Modifier.height(2.dp))

        days.forEachIndexed { index, date ->
            val lessons = dayLessons(snapshot, date, parity)
            WeekDayCard(
                date = date,
                today = today,
                lessons = lessons,
                initiallyExpanded = date == today || (today !in days && index == 0),
                onOpenDay = { onOpenDay(date) },
            )
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Обновлено ${formatDateTime(snapshot.updatedAt ?: state.lastFetchAt)}",
            style = d.type.labelSmall,
            color = d.colors.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun WeekWorksCard(works: List<WorkItem>, today: LocalDate) {
    val d = LocalDesign.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(d.shapes.card)
            .background(if (d.isMiuix) d.colors.surface else d.colors.surfaceContainerLow)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.EventNote, null, tint = d.colors.primary, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
            Text(
                "Работы на неделе",
                style = d.type.cardTitle.copy(fontSize = 15.sp),
                color = d.colors.onSurface,
            )
            Spacer(Modifier.weight(1f))
            Text(works.size.toString(), style = d.type.label, color = d.colors.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        works.take(4).forEach { work ->
            val days = ChronoUnit.DAYS.between(today, work.date).toInt()
            val urgent = days <= 1
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (urgent) Icons.Rounded.WarningAmber else Icons.Rounded.EventNote,
                    null,
                    tint = if (urgent) d.colors.error else d.colors.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(7.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        work.displayTitle,
                        style = d.type.bodySmall.copy(color = d.colors.onSurface, fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${formatDateMedium(work.date)} · ${work.type}",
                        style = d.type.labelSmall.copy(color = d.colors.onSurfaceVariant),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (urgent) {
                    Text(
                        if (days == 0) "сегодня" else "завтра",
                        style = d.type.labelSmall.copy(color = d.colors.error),
                    )
                }
            }
        }
        if (works.size > 4) {
            Text(
                "и ещё ${works.size - 4}",
                style = d.type.labelSmall,
                color = d.colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

@Composable
private fun WeekDayCard(
    date: LocalDate,
    today: LocalDate,
    lessons: List<app.rt521.schedule.data.Lesson>,
    initiallyExpanded: Boolean,
    onOpenDay: () -> Unit,
) {
    val d = LocalDesign.current
    var expanded by rememberSaveable(date.toEpochDay()) { mutableStateOf(initiallyExpanded) }
    val isToday = date == today
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "weekChevron")
    val bg = if (d.isMiuix) d.colors.surface else d.colors.surfaceContainerLow
    val borderColor = when {
        isToday -> d.colors.primary.copy(alpha = 0.65f)
        d.style == ThemeStyle.HYBRID -> d.colors.outlineVariant.copy(alpha = 0.5f)
        else -> Color.Transparent
    }
    val activeCount = lessons.count { !it.cancelled }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(d.shapes.card)
            .background(bg)
            .border(if (borderColor != Color.Transparent) 1.5.dp else 0.dp, borderColor, d.shapes.card),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier
                    .size(42.dp)
                    .clip(if (d.isMiuix) androidx.compose.foundation.shape.RoundedCornerShape(12.dp) else CircleShape)
                    .background(if (isToday) d.colors.primary else d.colors.surfaceContainerHigh),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    date.dayOfMonth.toString(),
                    style = d.type.body.copy(fontWeight = FontWeight.ExtraBold, fontSize = 17.sp),
                    color = if (isToday) d.colors.onPrimary else d.colors.onSurface,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        dayName(date.dayOfWeek),
                        style = d.type.body.copy(fontWeight = FontWeight.SemiBold),
                        color = d.colors.onSurface,
                    )
                    if (isToday) {
                        Spacer(Modifier.width(7.dp))
                        Text(
                            "сегодня",
                            style = d.type.labelSmall.copy(
                                color = d.colors.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp,
                            ),
                            modifier = Modifier
                                .clip(d.shapes.chip)
                                .background(d.colors.primary.copy(alpha = 0.10f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(
                    "${date.dayOfMonth} ${monthGenitive(date.month)}",
                    style = d.type.labelSmall.copy(color = d.colors.onSurfaceVariant),
                )
            }
            Text(
                if (activeCount == 0) "нет пар" else pluralPairs(activeCount),
                style = d.type.labelSmall.copy(color = d.colors.onSurfaceVariant),
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Rounded.ExpandMore,
                null,
                tint = d.colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp).rotate(rotation),
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(Modifier.padding(bottom = 10.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(d.colors.outlineVariant.copy(alpha = 0.5f)),
                )
                if (lessons.isEmpty()) {
                    Text(
                        if (date.dayOfWeek == java.time.DayOfWeek.SUNDAY) "Выходной — пар нет" else "Пар нет",
                        style = d.type.bodySmall,
                        color = d.colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    )
                } else {
                    lessons.forEach { lesson ->
                        CompactLessonRow(lesson = lesson, onOpen = onOpenDay)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactLessonRow(
    lesson: app.rt521.schedule.data.Lesson,
    onOpen: () -> Unit,
) {
    val d = LocalDesign.current
    val accent = subjectColor(lesson.subject)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(48.dp)) {
            Text(
                lesson.time.substringBefore('-'),
                style = d.type.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = d.colors.onSurface,
            )
            Text(
                lesson.time.substringAfter('-'),
                style = d.type.labelSmall.copy(color = d.colors.onSurfaceVariant, fontSize = 10.sp),
            )
        }
        Box(
            Modifier
                .padding(horizontal = 8.dp)
                .width(3.dp)
                .height(34.dp)
                .clip(CircleShape)
                .background(if (lesson.cancelled) d.colors.error else accent),
        )
        Column(Modifier.weight(1f)) {
            Text(
                lesson.subject,
                style = d.type.bodySmall.copy(
                    color = d.colors.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (lesson.cancelled) TextDecoration.LineThrough else TextDecoration.None,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "каб. ${lesson.room}",
                    style = d.type.labelSmall.copy(color = d.colors.onSurfaceVariant),
                )
                if (lesson.weeks != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${lesson.weeks.joinToString(",")} нед.",
                        style = d.type.labelSmall.copy(
                            color = d.colors.primary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        modifier = Modifier
                            .clip(d.shapes.chip)
                            .background(d.colors.primary.copy(alpha = 0.09f))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
                lesson.change?.let { kind ->
                    Spacer(Modifier.width(5.dp))
                    Text(
                        when (kind) {
                            ChangeKind.REPLACE -> "замена"
                            ChangeKind.EXTRA -> "доп."
                            ChangeKind.CANCEL -> "отмена"
                        },
                        style = d.type.labelSmall.copy(
                            color = if (kind == ChangeKind.CANCEL) d.colors.error else d.colors.tertiary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }
        Icon(
            Icons.Rounded.ChevronRight,
            null,
            tint = d.colors.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp),
        )
    }
}
