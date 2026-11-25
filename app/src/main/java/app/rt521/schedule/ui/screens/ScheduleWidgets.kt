package app.rt521.schedule.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.rt521.schedule.data.AppState
import app.rt521.schedule.data.Lesson
import app.rt521.schedule.data.LessonPhase
import app.rt521.schedule.data.LiveStatus
import app.rt521.schedule.data.WorkItem
import app.rt521.schedule.data.dayLessons
import app.rt521.schedule.data.dayShort
import app.rt521.schedule.data.formatCountdown
import app.rt521.schedule.data.formatDateMedium
import app.rt521.schedule.data.formatTime
import app.rt521.schedule.data.monthGenitive
import app.rt521.schedule.data.pluralPairs
import app.rt521.schedule.data.pluralWorks
import app.rt521.schedule.data.relativeDayLabel
import app.rt521.schedule.data.semesterWeek
import app.rt521.schedule.data.weekLabel
import app.rt521.schedule.ui.components.DesignIconButton
import app.rt521.schedule.ui.components.DesignPill
import app.rt521.schedule.ui.components.DesignTextButton
import app.rt521.schedule.ui.components.TinyDot
import app.rt521.schedule.ui.theme.LocalDesign
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/* ============================================================
 *  Header
 * ============================================================ */

@Composable
fun ScreenHeader(
    group: String,
    semester: Int,
    title: String,
    subtitle: String,
    onTheme: (() -> Unit)?,
    onRefresh: (() -> Unit)?,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    val d = LocalDesign.current
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TinyDot(d.colors.primary, 6.dp)
                Spacer(Modifier.width(7.dp))
                Text(
                    text = "$group · $semester СЕМЕСТР",
                    style = d.type.labelSmall.copy(
                        color = d.colors.primary,
                        letterSpacing = 1.1.sp,
                    ),
                    maxLines = 1,
                )
            }
            Text(
                title,
                style = d.type.headline,
                color = d.colors.onBackground,
                modifier = Modifier.padding(top = 3.dp),
            )
            Text(
                subtitle,
                style = d.type.bodySmall,
                color = d.colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (onRefresh != null) {
                RefreshAction(isRefreshing = isRefreshing, onRefresh = onRefresh)
            }
            if (onTheme != null) {
                DesignIconButton(
                    icon = Icons.Rounded.Palette,
                    contentDescription = "Оформление",
                    onClick = onTheme,
                )
            }
        }
    }
}

@Composable
private fun RefreshAction(isRefreshing: Boolean, onRefresh: () -> Unit) {
    val d = LocalDesign.current
    if (isRefreshing) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(d.colors.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(19.dp),
                strokeWidth = 2.dp,
                color = d.colors.primary,
            )
        }
    } else {
        DesignIconButton(
            icon = Icons.Rounded.Refresh,
            contentDescription = "Обновить расписание",
            onClick = onRefresh,
        )
    }
}

/* ============================================================
 *  Parity pill
 * ============================================================ */

@Composable
fun ParityPill(
    state: AppState,
    date: LocalDate,
    onToast: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = LocalDesign.current
    val week = semesterWeek(state.schedule.semesterStart, date)
    val parity = state.parityFor(date)
    val manual = state.parityIsManual
    val container = when {
        manual -> d.colors.primaryContainer
        d.isMiuix -> d.miuix?.surfaceSecondary ?: d.colors.surfaceVariant
        else -> d.colors.secondaryContainer
    }
    val content = when {
        manual -> d.colors.onPrimaryContainer
        d.isMiuix -> d.miuix?.textSecondary ?: d.colors.onSurfaceVariant
        else -> d.colors.onSecondaryContainer
    }
    DesignPill(
        text = "$week-я неделя · ${weekLabel(parity)}",
        modifier = modifier,
        icon = if (manual) Icons.Rounded.Edit else Icons.Rounded.SwapHoriz,
        container = container,
        content = content,
        small = true,
        onClick = {
            state.toggleParityManually()
            onToast("Чётность: ${weekLabel(state.parityFor(date))} · вручную")
        },
    )
}

/* ============================================================
 *  Week navigation + date strip
 * ============================================================ */

fun mondayOf(date: LocalDate): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

fun weekRangeLabel(monday: LocalDate): String {
    val sunday = monday.plusDays(6)
    return if (monday.month == sunday.month) {
        "${monday.dayOfMonth} – ${sunday.dayOfMonth} ${monthGenitive(monday.month)}"
    } else {
        "${formatDateMedium(monday)} – ${formatDateMedium(sunday)}"
    }
}

@Composable
fun WeekNavigator(
    monday: LocalDate,
    semesterStart: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val d = LocalDesign.current
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmallNavButton(Icons.Rounded.ArrowBackIosNew, "Предыдущая неделя", onPrev)
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                weekRangeLabel(monday),
                style = d.type.body.copy(fontWeight = FontWeight.SemiBold),
                color = d.colors.onBackground,
            )
            Text(
                "${semesterWeek(semesterStart, monday)}-я неделя семестра",
                style = d.type.labelSmall,
                color = d.colors.onSurfaceVariant,
            )
        }
        if (onToday != null) {
            DesignTextButton("Сегодня", onToday, color = d.colors.primary)
        } else {
            Spacer(Modifier.width(4.dp))
        }
        SmallNavButton(Icons.Rounded.ArrowForwardIos, "Следующая неделя", onNext)
    }
}

@Composable
private fun SmallNavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    val d = LocalDesign.current
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (d.isMiuix) d.miuix?.surfaceSecondary ?: d.colors.surfaceVariant else d.colors.surfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = d.colors.onSurfaceVariant, modifier = Modifier.size(14.dp))
    }
}

@Composable
fun DateStrip(
    selected: LocalDate,
    today: LocalDate,
    works: List<WorkItem>,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = LocalDesign.current
    val monday = mondayOf(selected)
    val dates = (0L..6L).map { monday.plusDays(it) }
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        dates.forEach { date ->
            val isSelected = date == selected
            val isToday = date == today
            val hasWork = works.any { it.date == date }
            val shape = when (d.style) {
                app.rt521.schedule.data.ThemeStyle.MATERIAL -> RoundedCornerShape(18.dp)
                app.rt521.schedule.data.ThemeStyle.MIUIX -> RoundedCornerShape(13.dp)
                app.rt521.schedule.data.ThemeStyle.HYBRID -> RoundedCornerShape(
                    topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 6.dp,
                )
            }
            val bg = when {
                isSelected -> d.colors.primary
                d.isMiuix -> d.colors.surface
                else -> d.colors.surfaceContainerLow
            }
            val border = when {
                isSelected -> Color.Transparent
                isToday -> d.colors.primary.copy(alpha = 0.4f)
                else -> d.colors.outlineVariant.copy(alpha = 0.35f)
            }
            val fg = if (isSelected) d.colors.onPrimary else d.colors.onSurface
            val secondary = if (isSelected) {
                d.colors.onPrimary.copy(alpha = 0.75f)
            } else {
                d.colors.onSurfaceVariant
            }
            Column(
                Modifier
                    .weight(1f)
                    .height(64.dp)
                    .clip(shape)
                    .background(bg)
                    .border(1.dp, border, shape)
                    .clickable { onSelect(date) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    dayShort(date.dayOfWeek),
                    style = d.type.labelSmall.copy(
                        color = secondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 10.sp,
                    ),
                )
                Text(
                    date.dayOfMonth.toString(),
                    style = d.type.body.copy(fontWeight = FontWeight.ExtraBold, fontSize = 17.sp),
                    color = fg,
                )
                if (hasWork) {
                    TinyDot(d.colors.error, 5.dp)
                } else if (isToday) {
                    TinyDot(if (isSelected) d.colors.onPrimary else d.colors.primary, 4.dp)
                } else {
                    Spacer(Modifier.height(5.dp))
                }
            }
        }
    }
}

/* ============================================================
 *  Live status hero
 * ============================================================ */

private data class HeroContent(
    val label: String,
    val title: String,
    val meta: String,
    val countdown: String? = null,
    val suffix: String? = null,
    val progress: Float? = null,
    val startLabel: String? = null,
    val endLabel: String? = null,
    val live: Boolean = false,
    val celebration: Boolean = false,
)

@Composable
fun StatusHero(
    state: AppState,
    date: LocalDate,
    today: LocalDate,
    lessons: List<Lesson>,
    live: LiveStatus?,
    works: List<WorkItem>,
    modifier: Modifier = Modifier,
) {
    val d = LocalDesign.current
    val isToday = date == today
    val tomorrow = date.plusDays(1)
    val tomorrowLessons = dayLessons(
        state.schedule,
        tomorrow,
        state.parityFor(tomorrow),
    )
    val tomorrowFirst = tomorrowLessons.firstOrNull { !it.cancelled }

    val content = when {
        !isToday -> {
            val active = lessons.filterNot { it.cancelled }
            val first = active.firstOrNull()
            val last = active.lastOrNull()
            HeroContent(
                label = relativeDayLabel(date, today).uppercase(),
                title = if (active.isEmpty()) "Пар нет" else pluralPairs(active.size),
                meta = if (active.isEmpty()) {
                    if (date.dayOfWeek == DayOfWeek.SUNDAY) "Выходной — отдыхаем" else "Свободный день"
                } else {
                    "с ${first!!.time.substringBefore('-')} до ${last!!.time.substringAfter('-')}" +
                        if (works.isNotEmpty()) " · ${works.size} ${pluralWorks(works.size)}" else ""
                },
                celebration = active.isEmpty(),
            )
        }

        live == null -> HeroContent(
            label = "Сегодня",
            title = "Пар нет",
            meta = if (tomorrowFirst != null) {
                "Завтра: ${tomorrowFirst.subject} в ${tomorrowFirst.time.substringBefore('-')}"
            } else "Свободный день",
            celebration = true,
        )

        live is LiveStatus.NoLessons -> HeroContent(
            label = relativeDayLabel(date, today).uppercase(),
            title = if (date.dayOfWeek == DayOfWeek.SUNDAY) "Воскресенье" else "Пар нет",
            meta = if (tomorrowFirst != null) {
                "Завтра: ${tomorrowFirst.subject} в ${tomorrowFirst.time.substringBefore('-')}"
            } else "Пар нет — отдыхаем",
            celebration = true,
        )

        live is LiveStatus.BeforeFirst -> HeroContent(
            label = "До первой пары",
            title = live.lesson.subject,
            meta = buildString {
                append("каб. ${live.lesson.room} · ${live.lesson.time}")
                if (live.lesson.change != null) append(" · изменение")
            },
            countdown = formatCountdown(live.secondsLeft),
            suffix = "до начала",
            startLabel = live.lesson.time.substringBefore('-'),
            endLabel = live.lesson.time.substringAfter('-'),
            live = true,
        )

        live is LiveStatus.InLesson -> {
            val phaseLabel = when (live.phase) {
                LessonPhase.FIRST_HALF -> "Идёт пара · 1-я половина"
                LessonPhase.MID_BREAK -> "Перерыв внутри пары"
                LessonPhase.SECOND_HALF -> "Идёт пара · 2-я половина"
            }
            val suffix = when (live.phase) {
                LessonPhase.FIRST_HALF -> "до перерыва"
                LessonPhase.MID_BREAK -> "до конца перерыва"
                LessonPhase.SECOND_HALF -> "до конца пары"
            }
            HeroContent(
                label = phaseLabel,
                title = live.lesson.subject,
                meta = buildString {
                    append("каб. ${live.lesson.room} · ${live.lesson.time}")
                    if (!live.lesson.note.isBlank()) append(" · ${live.lesson.note}")
                },
                countdown = formatCountdown(live.secondsLeft),
                suffix = suffix,
                progress = live.progress,
                startLabel = live.lesson.time.substringBefore('-'),
                endLabel = live.lesson.time.substringAfter('-'),
                live = true,
            )
        }

        live is LiveStatus.OnBreak -> HeroContent(
            label = if (live.big) "Большой перерыв" else "Перерыв",
            title = live.next?.let { "Далее: ${it.subject}" } ?: "Перерыв",
            meta = live.next?.let { "каб. ${it.room} · ${it.time}" } ?: "Отдыхаем",
            countdown = formatCountdown(live.secondsLeft),
            suffix = "до начала",
            progress = live.progress,
            endLabel = formatTime(live.endMinutes),
            live = true,
        )

        live is LiveStatus.Finished -> HeroContent(
            label = "Сегодня",
            title = "Пары закончились",
            meta = buildString {
                append("Было ${pluralPairs(live.totalToday)}")
                if (tomorrowFirst != null) {
                    append(" · завтра ${tomorrowFirst.subject} в ${tomorrowFirst.time.substringBefore('-')}")
                }
            },
            celebration = true,
        )

        else -> HeroContent(label = "Сегодня", title = "Пар нет", meta = "")
    }

    val primaryFill = if (d.style == app.rt521.schedule.data.ThemeStyle.MATERIAL) d.colors.primary else Color.White
    val heroShadow = when (d.style) {
        app.rt521.schedule.data.ThemeStyle.MATERIAL -> 4.dp
        app.rt521.schedule.data.ThemeStyle.MIUIX -> 10.dp
        app.rt521.schedule.data.ThemeStyle.HYBRID -> 12.dp
    }
    Box(
        modifier
            .fillMaxWidth()
            .shadow(heroShadow, d.shapes.hero, clip = false)
            .clip(d.shapes.hero)
            .background(Brush.linearGradient(d.heroGradient)),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                color = if (d.isMiuix) Color.White.copy(alpha = 0.10f) else d.heroContent.copy(alpha = 0.07f),
                radius = h * 0.9f,
                center = Offset(w * 1.02f, -h * 0.18f),
            )
            drawCircle(
                color = if (d.isMiuix) Color.White.copy(alpha = 0.06f) else d.heroContent.copy(alpha = 0.05f),
                radius = w * 0.28f,
                center = Offset(w * 0.04f, h * 1.06f),
            )
        }
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (content.live) {
                    LiveDot(if (d.style == app.rt521.schedule.data.ThemeStyle.MATERIAL) d.colors.error else Color.White)
                    Spacer(Modifier.width(7.dp))
                } else if (content.celebration) {
                    Icon(Icons.Rounded.Celebration, null, tint = d.heroContent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(7.dp))
                }
                Text(
                    text = content.label,
                    style = d.type.labelSmall.copy(
                        color = d.heroContent.copy(alpha = 0.8f),
                        letterSpacing = 1.1.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(d.heroContent.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Today,
                        null,
                        tint = d.heroContent.copy(alpha = 0.85f),
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
            Text(
                content.title,
                style = d.type.title.copy(
                    fontSize = if (content.title.length > 34) 20.sp else 23.sp,
                    lineHeight = 28.sp,
                ),
                color = d.heroContent,
                modifier = Modifier.padding(top = 9.dp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                content.meta,
                style = d.type.bodySmall.copy(color = d.heroContent.copy(alpha = 0.78f)),
                modifier = Modifier.padding(top = 3.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (content.countdown != null) {
                Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.Bottom) {
                    Text(
                        content.countdown,
                        style = d.type.countdown.copy(
                            fontSize = 44.sp,
                            color = d.heroContent,
                        ),
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(
                        content.suffix.orEmpty(),
                        style = d.type.bodySmall.copy(
                            color = d.heroContent.copy(alpha = 0.72f),
                            fontWeight = FontWeight.Medium,
                        ),
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
                }
            }
            if (content.progress != null) {
                HeroProgress(
                    progress = content.progress,
                    color = primaryFill,
                    track = d.heroContent.copy(alpha = 0.16f),
                    height = if (d.isMiuix) 4.dp else 7.dp,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
            if (content.startLabel != null || content.endLabel != null) {
                Row(Modifier.fillMaxWidth().padding(top = 7.dp)) {
                    Text(
                        content.startLabel.orEmpty(),
                        style = d.type.labelSmall.copy(color = d.heroContent.copy(alpha = 0.65f)),
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        content.endLabel.orEmpty(),
                        style = d.type.labelSmall.copy(color = d.heroContent.copy(alpha = 0.65f)),
                    )
                }
            }
            if (works.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    works.take(2).forEach { work ->
                        Row(
                            Modifier
                                .clip(d.shapes.chip)
                                .background(d.heroContent.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.WarningAmber,
                                null,
                                tint = if (d.style == app.rt521.schedule.data.ThemeStyle.MATERIAL) d.colors.error else Color.White,
                                modifier = Modifier.size(12.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                work.displayTitle,
                                style = d.type.labelSmall.copy(color = d.heroContent),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (works.size > 2) {
                        Text(
                            "+${works.size - 2}",
                            style = d.type.labelSmall.copy(color = d.heroContent.copy(alpha = 0.8f)),
                            modifier = Modifier.padding(top = 5.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroProgress(
    progress: Float,
    color: Color,
    track: Color,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 950, easing = LinearEasing),
        label = "heroProgress",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(track),
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(height)
                .clip(CircleShape)
                .background(color),
        )
    }
}

@Composable
fun LiveDot(color: Color) {
    val transition = rememberInfiniteTransition(label = "liveDot")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "liveDotAlpha",
    )
    Box(
        Modifier
            .size(9.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha)),
    )
}
