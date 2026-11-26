package app.rt521.schedule.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.rt521.schedule.data.AppState
import app.rt521.schedule.data.WorkItem
import app.rt521.schedule.data.dayShort
import app.rt521.schedule.data.monthShort
import app.rt521.schedule.data.pluralDays
import app.rt521.schedule.data.pluralWorks
import app.rt521.schedule.data.subjectColor
import app.rt521.schedule.ui.BottomMetrics
import app.rt521.schedule.ui.components.EmptyState
import app.rt521.schedule.ui.components.SectionLabel
import app.rt521.schedule.ui.theme.LocalDesign
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun WorksScreen(
    state: AppState,
    today: LocalDate,
    metric: BottomMetrics,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    onOpenDay: (LocalDate) -> Unit,
) {
    val snapshot = state.schedule
    val all = snapshot.works.sortedBy { it.date }
    val upcoming = all.filter { !it.date.isBefore(today) }
    val past = all.filter { it.date.isBefore(today) }.sortedByDescending { it.date }

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
            title = "Работы",
            subtitle = "Все работы · ${snapshot.academicYear}",
            onTheme = onOpenSettings,
            onRefresh = onRefresh,
            isRefreshing = state.isRefreshing,
        )

        if (state.lastError != null) {
            UpdateErrorBanner(
                text = state.lastError.orEmpty(),
                onRetry = onRefresh,
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        if (all.isEmpty()) {
            Spacer(Modifier.height(20.dp))
            EmptyState(
                icon = Icons.Rounded.EventNote,
                title = "Работ пока нет",
                subtitle = "Как только появятся — они будут здесь",
            )
        } else {
            if (upcoming.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                SectionLabel("Ближайшие работы")
                Spacer(Modifier.height(6.dp))
                upcoming.forEach { work ->
                    WorkListCard(
                        work = work,
                        today = today,
                        onClick = { onOpenDay(work.date) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            if (past.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                SectionLabel("Прошедшие работы")
                Spacer(Modifier.height(6.dp))
                past.forEach { work ->
                    WorkListCard(
                        work = work,
                        today = today,
                        past = true,
                        onClick = { onOpenDay(work.date) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            "Всего ${all.size} ${pluralWorks(all.size)} · работы обновляются вместе с расписанием",
            style = LocalDesign.current.type.labelSmall,
            color = LocalDesign.current.colors.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun WorkListCard(
    work: WorkItem,
    today: LocalDate,
    past: Boolean = false,
    onClick: () -> Unit,
) {
    val d = LocalDesign.current
    val accent = subjectColor(work.subject)
    val days = ChronoUnit.DAYS.between(today, work.date).toInt()
    val dayLabel = when {
        past -> "${-days} ${pluralDays(-days)} назад"
        days == 0 -> "сегодня"
        days == 1 -> "завтра"
        else -> "через $days ${pluralDays(days)}"
    }
    val bg = if (d.isMiuix) d.colors.surface else d.colors.surfaceContainerLow
    Row(
        Modifier
            .fillMaxWidth()
            .alpha(if (past) 0.6f else 1f)
            .clip(d.shapes.card)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            Modifier
                .width(48.dp)
                .clip(d.shapes.smallCard)
                .background(accent.copy(alpha = 0.12f))
                .padding(vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                dayShort(work.date.dayOfWeek),
                style = d.type.labelSmall.copy(color = accent, fontWeight = FontWeight.Bold),
            )
            Text(
                work.date.dayOfMonth.toString(),
                style = d.type.cardTitle.copy(color = accent, fontWeight = FontWeight.Bold),
            )
            Text(
                monthShort(work.date.month),
                style = d.type.labelSmall.copy(color = accent.copy(alpha = 0.8f), fontSize = 9.sp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "${work.subject} · ${work.type}",
                style = d.type.labelSmall.copy(color = d.colors.onSurfaceVariant),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                work.displayTitle,
                style = d.type.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(top = 2.dp),
            )
            if (work.note.isNotBlank()) {
                Text(
                    work.note,
                    style = d.type.labelSmall.copy(color = d.colors.onSurfaceVariant),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Row(
            Modifier
                .clip(d.shapes.chip)
                .background(
                    if (past) d.colors.surfaceContainerHigh
                    else androidx.compose.ui.graphics.Color(0xFFFF9500).copy(alpha = 0.13f),
                )
                .padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (past) Icons.Rounded.EventNote else Icons.Rounded.WarningAmber,
                null,
                tint = if (past) d.colors.onSurfaceVariant else androidx.compose.ui.graphics.Color(0xFFFF9500),
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(3.dp))
            Text(
                dayLabel,
                style = d.type.labelSmall.copy(
                    color = if (past) d.colors.onSurfaceVariant else androidx.compose.ui.graphics.Color(0xFFFF9500),
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.5.sp,
                ),
                maxLines = 1,
            )
        }
    }
}
