package app.rt521.schedule.ui.miuix

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.rt521.schedule.data.AppState
import app.rt521.schedule.data.WorkItem
import app.rt521.schedule.data.dayShort
import app.rt521.schedule.data.pluralDays
import app.rt521.schedule.data.pluralWorks
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun MiuixWorksScreen(
    state: AppState,
    today: LocalDate,
    innerPadding: PaddingValues,
    scrollBehavior: ScrollBehavior,
    onOpenDay: (LocalDate) -> Unit,
    onRefresh: () -> Unit,
) {
    val snapshot = state.schedule
    val all = snapshot.works.sortedBy { it.date }
    val upcoming = all.filter { !it.date.isBefore(today) }
    val past = all.filter { it.date.isBefore(today) }.sortedByDescending { it.date }

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
        if (state.lastError != null) {
            item(key = "error") {
                MiuixErrorCard(text = state.lastError.orEmpty(), onRetry = onRefresh)
            }
        }

        if (all.isEmpty()) {
            item(key = "empty") {
                MiuixEmptyCard(
                    title = "Работ пока нет",
                    summary = "Как только появятся — они будут здесь",
                )
            }
        }

        if (upcoming.isNotEmpty()) {
            item(key = "upcomingTitle") {
                SmallTitle(
                    text = "Ближайшие работы",
                    insideMargin = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                )
            }
            item(key = "upcoming") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    upcoming.forEachIndexed { index, work ->
                        if (index > 0) HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        MiuixWorkListRow(work = work, today = today, onClick = { onOpenDay(work.date) })
                    }
                }
            }
        }

        if (past.isNotEmpty()) {
            item(key = "pastTitle") {
                SmallTitle(
                    text = "Прошедшие работы",
                    insideMargin = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                )
            }
            item(key = "past") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    past.forEachIndexed { index, work ->
                        if (index > 0) HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        MiuixWorkListRow(
                            work = work,
                            today = today,
                            past = true,
                            onClick = { onOpenDay(work.date) },
                        )
                    }
                }
            }
        }

        item(key = "footer") {
            Text(
                "Всего ${all.size} ${pluralWorks(all.size)} · работы обновляются вместе с расписанием",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun MiuixWorkListRow(
    work: WorkItem,
    today: LocalDate,
    past: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val days = ChronoUnit.DAYS.between(today, work.date).toInt()
    val dayLabel = when {
        past -> "${-days} ${pluralDays(-days)} назад"
        days == 0 -> "сегодня"
        days == 1 -> "завтра"
        else -> "через $days ${pluralDays(days)}"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(42.dp)) {
            Text(
                dayShort(work.date.dayOfWeek),
                style = MiuixTheme.textStyles.footnote2,
                color = if (past) colors.onBackgroundVariant else androidx.compose.ui.graphics.Color(0xFFFF9500),
                fontWeight = FontWeight.Medium,
            )
            Text(
                work.date.dayOfMonth.toString(),
                style = MiuixTheme.textStyles.title4,
                color = if (past) colors.onBackgroundVariant else colors.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                work.displayTitle,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.Medium,
                color = if (past) colors.onBackgroundVariant else colors.onBackground,
            )
            Text(
                "${work.subject} · ${work.type}" + if (work.note.isNotBlank()) " · ${work.note}" else "",
                style = MiuixTheme.textStyles.footnote1,
                color = colors.onBackgroundVariant,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            dayLabel,
            style = MiuixTheme.textStyles.footnote2,
            color = if (past) colors.onBackgroundVariant else androidx.compose.ui.graphics.Color(0xFFFF9500),
            fontWeight = FontWeight.Medium,
        )
    }
}
