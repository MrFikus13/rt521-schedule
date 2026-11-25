package app.rt521.schedule.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.rt521.schedule.data.ChangeKind
import app.rt521.schedule.data.Lesson
import app.rt521.schedule.data.LiveStatus
import app.rt521.schedule.data.ThemeStyle
import app.rt521.schedule.data.WorkItem
import app.rt521.schedule.data.pluralDays
import app.rt521.schedule.data.subjectAbbreviation
import app.rt521.schedule.data.subjectColor
import app.rt521.schedule.ui.components.SubjectGlyph
import app.rt521.schedule.ui.theme.LocalDesign
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun LessonCard(
    entry: Lesson,
    index: Int,
    date: LocalDate,
    today: LocalDate,
    isCurrent: Boolean,
    isDone: Boolean,
    live: LiveStatus?,
    works: List<WorkItem>,
    modifier: Modifier = Modifier,
) {
    val d = LocalDesign.current
    val accent = subjectColor(entry.subject)
    val cancelled = entry.cancelled
    val changed = entry.change != null

    val bg = when {
        cancelled -> d.colors.errorContainer
        isCurrent -> d.colors.primaryContainer
        changed && !d.isMiuix -> d.colors.tertiaryContainer.copy(alpha = 0.35f)
        d.isMiuix -> d.colors.surface
        else -> d.colors.surfaceContainerLow
    }
    val content = when {
        cancelled -> d.colors.onErrorContainer
        isCurrent -> d.colors.onPrimaryContainer
        else -> d.colors.onSurface
    }
    val secondary = when {
        cancelled -> content.copy(alpha = 0.68f)
        isCurrent -> content.copy(alpha = 0.68f)
        else -> d.colors.onSurfaceVariant
    }
    val outline = when {
        cancelled -> d.colors.error.copy(alpha = 0.4f)
        d.style == ThemeStyle.MIUIX && isCurrent -> d.colors.primary.copy(alpha = 0.40f)
        d.style == ThemeStyle.HYBRID -> d.colors.outlineVariant.copy(alpha = 0.55f)
        else -> Color.Transparent
    }
    val shadow = if (isCurrent && d.style != ThemeStyle.MIUIX) 3.dp else 0.dp

    Column(
        modifier
            .fillMaxWidth()
            .alpha(if (isDone && !isCurrent && !cancelled) 0.58f else 1f)
            .shadow(shadow, d.shapes.card, clip = false)
            .clip(d.shapes.card)
            .background(bg)
            .border(if (outline != Color.Transparent) 1.dp else 0.dp, outline, d.shapes.card),
    ) {
        if (d.style == ThemeStyle.HYBRID && !cancelled) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(if (isCurrent) d.colors.primary else accent),
            )
        }
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                SubjectGlyph(
                    abbreviation = subjectAbbreviation(entry.subject),
                    color = if (cancelled) d.colors.error else accent,
                    size = 44.dp,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isCurrent) {
                            LiveDot(d.colors.error)
                            Spacer(Modifier.width(5.dp))
                        }
                        Text(
                            text = "Пара $index",
                            style = d.type.labelSmall.copy(
                                color = if (isCurrent) d.colors.primary else secondary,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        if (entry.weeks != null) {
                            Spacer(Modifier.width(6.dp))
                            WeekBadge(entry.weeks)
                        }
                        entry.change?.let { kind ->
                            Spacer(Modifier.width(5.dp))
                            ChangeBadge(kind)
                        }
                    }
                    Text(
                        text = entry.subject,
                        style = d.type.cardTitle,
                        color = content,
                        textDecoration = if (cancelled) TextDecoration.LineThrough else TextDecoration.None,
                        modifier = Modifier.padding(top = 5.dp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (entry.teachers.isNotEmpty() && !cancelled) {
                        Row(
                            Modifier.padding(top = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.Person,
                                null,
                                tint = secondary,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                entry.teachers.joinToString(", "),
                                style = d.type.bodySmall.copy(color = secondary),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            if (d.isMiuix) {
                MiuixInfoRow(entry, secondary)
            } else {
                ExpressiveInfoRow(entry, secondary, isCurrent)
            }

            if (entry.note.isNotBlank()) {
                Row(
                    Modifier
                        .padding(top = 9.dp)
                        .fillMaxWidth()
                        .clip(d.shapes.smallCard)
                        .background(content.copy(alpha = 0.07f))
                        .padding(horizontal = 9.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        Icons.Rounded.Info,
                        null,
                        tint = secondary,
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .size(13.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        entry.note,
                        style = d.type.bodySmall.copy(color = secondary),
                    )
                }
            }

            works.forEach { work ->
                WorkBanner(
                    work = work,
                    today = today,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            if (isCurrent && live is LiveStatus.InLesson && !cancelled) {
                val animated by animateFloatAsState(
                    targetValue = live.progress.coerceIn(0f, 1f),
                    animationSpec = tween(950, easing = LinearEasing),
                    label = "cardProgress",
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 13.dp)
                        .height(if (d.isMiuix) 3.dp else 6.dp)
                        .clip(CircleShape)
                        .background(content.copy(alpha = 0.14f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(animated)
                            .height(if (d.isMiuix) 3.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (d.isMiuix) d.colors.primary else accent),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChangeBadge(kind: ChangeKind) {
    val d = LocalDesign.current
    val (text, bg, fg) = when (kind) {
        ChangeKind.REPLACE -> Triple(
            "Замена",
            if (d.isMiuix) d.colors.primary.copy(alpha = 0.12f) else d.colors.tertiaryContainer,
            if (d.isMiuix) d.colors.primary else d.colors.onTertiaryContainer,
        )
        ChangeKind.EXTRA -> Triple(
            "Доп. пара",
            if (d.isMiuix) d.colors.primary.copy(alpha = 0.12f) else d.colors.primaryContainer,
            if (d.isMiuix) d.colors.primary else d.colors.onPrimaryContainer,
        )
        ChangeKind.CANCEL -> Triple(
            "Отменена",
            d.colors.errorContainer,
            d.colors.onErrorContainer,
        )
    }
    Text(
        text = text,
        style = d.type.labelSmall.copy(
            color = fg,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
        ),
        modifier = Modifier
            .clip(d.shapes.chip)
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}

@Composable
private fun ExpressiveInfoRow(entry: Lesson, color: Color, isCurrent: Boolean) {
    val d = LocalDesign.current
    val chipBg = if (isCurrent) color.copy(alpha = 0.10f) else d.colors.surfaceContainerHighest.copy(alpha = 0.8f)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        InfoChip(Icons.Rounded.AccessTime, entry.time, chipBg, color)
        InfoChip(Icons.Rounded.Place, "каб. ${entry.room.ifBlank { "—" }}", chipBg, color)
    }
}

@Composable
private fun MiuixInfoRow(entry: Lesson, color: Color) {
    val d = LocalDesign.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.AccessTime, null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(entry.time, style = d.type.bodySmall.copy(color = color, fontWeight = FontWeight.Medium))
        Spacer(Modifier.width(12.dp))
        Icon(Icons.Rounded.Place, null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            "каб. ${entry.room.ifBlank { "—" }}",
            style = d.type.bodySmall.copy(color = color, fontWeight = FontWeight.Medium),
        )
    }
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    background: Color,
    content: Color,
) {
    val d = LocalDesign.current
    Row(
        Modifier
            .clip(d.shapes.chip)
            .background(background)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = content, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = d.type.labelSmall.copy(color = content), maxLines = 1)
    }
}

@Composable
private fun WeekBadge(weeks: List<Int>) {
    val d = LocalDesign.current
    val bg = if (d.isMiuix) {
        d.miuix?.accentSoft ?: d.colors.primaryContainer
    } else {
        d.colors.tertiaryContainer
    }
    val fg = if (d.isMiuix) d.colors.primary else d.colors.onTertiaryContainer
    Text(
        text = "${weeks.joinToString(",")} нед.",
        style = d.type.labelSmall.copy(
            color = fg,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
        ),
        modifier = Modifier
            .clip(d.shapes.chip)
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}

@Composable
fun BreakRow(
    range: String,
    label: String,
    isCurrent: Boolean,
    secondsLeft: Int?,
    modifier: Modifier = Modifier,
) {
    val d = LocalDesign.current
    val color = if (isCurrent) d.colors.primary else d.colors.onSurfaceVariant
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            range,
            style = d.type.labelSmall.copy(color = color.copy(alpha = 0.75f)),
        )
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .height(if (isCurrent) 2.dp else 1.dp)
                .weight(1f)
                .background(if (isCurrent) color.copy(alpha = 0.55f) else d.colors.outlineVariant),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            if (isCurrent && secondsLeft != null) "$label · ${app.rt521.schedule.data.formatCountdown(secondsLeft)}" else label,
            style = d.type.labelSmall.copy(
                color = color,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
            ),
        )
    }
}

@Composable
private fun WorkBanner(
    work: WorkItem,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val d = LocalDesign.current
    val days = ChronoUnit.DAYS.between(today, work.date).toInt()
    val urgent = days <= 0
    val tomorrow = days == 1
    val bg = when {
        urgent -> d.colors.errorContainer
        tomorrow -> if (d.isMiuix) {
            (d.miuix?.warning ?: d.colors.tertiary).copy(alpha = 0.14f)
        } else {
            d.colors.tertiaryContainer
        }
        else -> if (d.isMiuix) {
            (d.miuix?.warning ?: d.colors.tertiary).copy(alpha = 0.09f)
        } else {
            d.colors.surfaceContainerHighest
        }
    }
    val fg = when {
        urgent -> d.colors.onErrorContainer
        tomorrow && d.isMiuix -> d.miuix?.warning ?: d.colors.tertiary
        tomorrow -> d.colors.onTertiaryContainer
        else -> d.colors.onSurfaceVariant
    }
    val whenText = when {
        days == 0 -> "Сегодня"
        days == 1 -> "Завтра"
        days > 1 -> "Через $days ${pluralDays(days)}"
        else -> "Просрочено"
    }
    Row(
        modifier
            .fillMaxWidth()
            .clip(d.shapes.smallCard)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (urgent || tomorrow) Icons.Rounded.WarningAmber else Icons.Rounded.EventNote,
            null,
            tint = fg,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(7.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "$whenText: ${work.displayTitle}",
                style = d.type.bodySmall.copy(color = fg, fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                work.type,
                style = d.type.labelSmall.copy(color = fg.copy(alpha = 0.7f), fontSize = 10.sp),
                maxLines = 1,
            )
            if (work.note.isNotBlank()) {
                Text(
                    work.note,
                    style = d.type.labelSmall.copy(color = fg.copy(alpha = 0.8f), fontSize = 10.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
