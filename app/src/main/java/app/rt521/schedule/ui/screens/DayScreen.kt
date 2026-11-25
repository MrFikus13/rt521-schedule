package app.rt521.schedule.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.rt521.schedule.data.AppState
import app.rt521.schedule.data.LiveStatus
import app.rt521.schedule.data.activeLessons
import app.rt521.schedule.data.breakAfter
import app.rt521.schedule.data.computeLiveStatus
import app.rt521.schedule.data.dayLessons
import app.rt521.schedule.data.formatDateLong
import app.rt521.schedule.data.formatDateTime
import app.rt521.schedule.data.nowMinutes
import app.rt521.schedule.data.pluralPairs
import app.rt521.schedule.data.sameLesson
import app.rt521.schedule.data.semesterWeek
import app.rt521.schedule.data.worksFor
import app.rt521.schedule.data.worksForDate
import app.rt521.schedule.ui.BottomMetrics
import app.rt521.schedule.ui.components.DesignTextButton
import app.rt521.schedule.ui.components.EmptyState
import app.rt521.schedule.ui.components.SectionLabel
import app.rt521.schedule.ui.theme.LocalDesign
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun DayScreen(
    state: AppState,
    today: LocalDate,
    metric: BottomMetrics,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    onToast: (String) -> Unit,
) {
    val d = LocalDesign.current
    val snapshot = state.schedule
    val selected = state.selectedDate
    val parity = state.parityFor(selected)
    val lessons = dayLessons(snapshot, selected, parity)
    val active = activeLessons(lessons)
    val live = if (selected == today) computeLiveStatus(state.now, lessons) else null
    val selectedWorks = worksForDate(snapshot.works, selected)
    val monday = mondayOf(selected)
    val nowMin = nowMinutes(state.now)

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
            title = "Расписание",
            subtitle = "${formatDateLong(selected)} · ${snapshot.academicYear}",
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
            ParityPill(state, selected, onToast)
            Spacer(Modifier.weight(1f))
            if (state.parityIsManual) {
                Text("вручную", style = d.type.labelSmall, color = d.colors.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(10.dp))
        WeekNavigator(
            monday = monday,
            semesterStart = snapshot.semesterStart,
            onPrev = { state.selectDate(selected.minusWeeks(1)) },
            onNext = { state.selectDate(selected.plusWeeks(1)) },
            onToday = if (selected != today) {
                { state.selectDate(today) }
            } else null,
        )
        Spacer(Modifier.height(10.dp))
        DateStrip(
            selected = selected,
            today = today,
            works = snapshot.works,
            onSelect = { state.selectDate(it) },
        )

        if (state.lastError != null) {
            UpdateErrorBanner(
                text = state.lastError.orEmpty(),
                onRetry = onRefresh,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Spacer(Modifier.height(16.dp))
        StatusHero(
            state = state,
            date = selected,
            today = today,
            lessons = lessons,
            live = live,
            works = selectedWorks,
        )

        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(
                if (selected == today) "Расписание на сегодня" else "Расписание дня",
                Modifier.weight(1f),
            )
            Text(
                pluralPairs(active.size),
                style = d.type.labelSmall,
                color = d.colors.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))

        if (active.isEmpty()) {
            EmptyState(
                icon = if (selected.dayOfWeek == DayOfWeek.SUNDAY) Icons.Rounded.Celebration else Icons.Rounded.EventBusy,
                title = if (selected.dayOfWeek == DayOfWeek.SUNDAY) "Воскресенье" else "Пар нет",
                subtitle = if (selected.dayOfWeek == DayOfWeek.SUNDAY) {
                    "Выходной — отдыхаем и набираемся сил"
                } else {
                    "По текущей чётности недели занятий в этот день нет"
                },
            )
        } else {
            lessons.forEachIndexed { index, entry ->
                if (entry.cancelled && entry.change == app.rt521.schedule.data.ChangeKind.CANCEL) {
                    LessonCard(
                        entry = entry,
                        index = lessons.take(index).count { !it.cancelled } + 1,
                        date = selected,
                        today = today,
                        isCurrent = false,
                        isDone = true,
                        live = null,
                        works = emptyList(),
                    )
                } else {
                    if (entry.cancelled) return@forEachIndexed
                    val isCurrent = live is LiveStatus.InLesson && sameLesson(live.lesson, entry)
                    val isDone = selected == today && !isCurrent && nowMin >= entry.endMinutes
                    LessonCard(
                        entry = entry,
                        index = lessons.take(index).count { !it.cancelled } + 1,
                        date = selected,
                        today = today,
                        isCurrent = isCurrent,
                        isDone = isDone,
                        live = if (isCurrent) live else null,
                        works = worksFor(snapshot.works, selected, entry.subject, today),
                    )
                }

                val nextActive = lessons.drop(index + 1).firstOrNull { !it.cancelled }
                val breakPeriod = breakAfter(entry, nextActive)
                if (breakPeriod != null) {
                    val brCurrent = selected == today && nowMin >= breakPeriod.start && nowMin < breakPeriod.end
                    val seconds = if (brCurrent) ((breakPeriod.end - nowMin) * 60).toInt() else null
                    BreakRow(
                        range = breakPeriod.rangeLabel,
                        label = if (breakPeriod.big) "Большой перерыв" else "Перерыв",
                        isCurrent = brCurrent,
                        secondsLeft = seconds,
                    )
                } else if (nextActive != null && !entry.cancelled) {
                    Spacer(Modifier.height(10.dp))
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        Text(
            "Обновлено ${formatDateTime(snapshot.updatedAt ?: state.lastFetchAt)} · ${semesterWeek(snapshot.semesterStart, today)}-я неделя",
            style = d.type.labelSmall,
            color = d.colors.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun UpdateErrorBanner(text: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val d = LocalDesign.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(d.shapes.smallCard)
            .background(d.colors.errorContainer)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.WarningAmber,
            null,
            tint = d.colors.error,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.size(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Не удалось обновить",
                style = d.type.bodySmall.copy(
                    color = d.colors.onErrorContainer,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Text(
                text,
                style = d.type.labelSmall.copy(color = d.colors.onErrorContainer.copy(alpha = 0.8f)),
                maxLines = 2,
            )
        }
        DesignTextButton("Повторить", onRetry, color = d.colors.onErrorContainer)
    }
}
