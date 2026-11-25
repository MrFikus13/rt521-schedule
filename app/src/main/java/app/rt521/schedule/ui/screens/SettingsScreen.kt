package app.rt521.schedule.ui.screens

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.rt521.schedule.BuildConfig
import app.rt521.schedule.data.AppState
import app.rt521.schedule.data.ScheduleData
import app.rt521.schedule.data.ThemeMode
import app.rt521.schedule.data.ThemeStyle
import app.rt521.schedule.data.WeekParity
import app.rt521.schedule.data.formatDateTime
import app.rt521.schedule.data.isAllowedScheduleUrl
import app.rt521.schedule.data.semesterWeek
import app.rt521.schedule.data.weekLabel
import app.rt521.schedule.ui.BottomMetrics
import app.rt521.schedule.ui.components.DesignButton
import app.rt521.schedule.ui.components.DesignSegmented
import app.rt521.schedule.ui.components.DesignSwitch
import app.rt521.schedule.ui.components.DesignTextField
import app.rt521.schedule.ui.components.DesignTextButton
import app.rt521.schedule.ui.components.SectionLabel
import app.rt521.schedule.ui.components.SettingsGroup
import app.rt521.schedule.ui.components.SettingsRow
import app.rt521.schedule.ui.theme.LocalDesign
import app.rt521.schedule.ui.theme.Palettes

@Composable
fun SettingsScreen(
    state: AppState,
    metric: BottomMetrics,
    onToast: (String) -> Unit,
    onRefresh: () -> Unit,
    onThemeApplied: (ThemeStyle) -> Unit,
) {
    val d = LocalDesign.current
    val today = state.now.toLocalDate()
    val snapshot = state.schedule
    val week = semesterWeek(snapshot.semesterStart, today)
    val parity = state.parityFor(today)
    val palettes = Palettes.forStyle(state.settings.themeStyle)
    val currentPalette = Palettes.resolvedId(state.settings.themeStyle, state.settings.palette)
    var showUrlDialog by remember { mutableStateOf(false) }
    var urlDraft by remember { mutableStateOf("") }
    var urlError by remember { mutableStateOf("") }

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
            title = "Настройки",
            subtitle = "Оформление и обновления",
            onTheme = null,
            onRefresh = onRefresh,
            isRefreshing = state.isRefreshing,
        )

        Spacer(Modifier.height(16.dp))
        SectionLabel("Тема приложения")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ThemeStyle.entries.forEach { style ->
                ThemeStyleCard(
                    style = style,
                    selected = state.settings.themeStyle == style,
                    onClick = {
                        state.setThemeStyle(style)
                        onThemeApplied(style)
                    },
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        SettingsGroup(title = "Режим") {
            Column(Modifier.padding(14.dp)) {
                DesignSegmented(
                    options = ThemeMode.entries,
                    selected = state.settings.themeMode,
                    label = {
                        when (it) {
                            ThemeMode.LIGHT -> "Светлая"
                            ThemeMode.DARK -> "Тёмная"
                            ThemeMode.AUTO -> "Системная"
                        }
                    },
                    onSelect = {
                        state.setThemeMode(it)
                        onToast(
                            when (it) {
                                ThemeMode.LIGHT -> "Светлый режим"
                                ThemeMode.DARK -> "Тёмный режим"
                                ThemeMode.AUTO -> "Как в системе"
                            }
                        )
                    },
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        SettingsGroup(title = "Цветовая схема") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                palettes.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { palette ->
                            PaletteSwatch(
                                color = palette.color,
                                name = palette.name,
                                system = palette.system,
                                selected = palette.id == currentPalette,
                                onClick = {
                                    state.setPalette(palette.id)
                                    onToast("Цвет: ${palette.name}")
                                },
                            )
                        }
                    }
                }
                Text(
                    "Системная схема берёт цвета обоев (Android 12+)",
                    style = d.type.labelSmall,
                    color = d.colors.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        SettingsGroup(title = "Обновления") {
            SettingsRow(
                title = "Последнее обновление",
                subtitle = formatDateTime(state.lastFetchAt),
                leading = Icons.Rounded.Update,
                trailing = {
                    if (state.isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = d.colors.primary,
                        )
                    } else {
                        Icon(Icons.Rounded.Sync, null, tint = d.colors.onSurfaceVariant)
                    }
                },
                onClick = onRefresh,
            )
            SettingsRow(
                title = "Источник расписания",
                subtitle = if (!state.scheduleConfigured) {
                    "Не настроено · укажите raw-ссылку на data/schedule.json"
                } else if (state.usesDefaultUrl) {
                    "${state.scheduleUrl} (по умолчанию)"
                } else {
                    state.scheduleUrl
                },
                leading = Icons.Rounded.Cloud,
                trailing = { Icon(Icons.Rounded.ChevronRight, null, tint = d.colors.onSurfaceVariant) },
                onClick = {
                    urlDraft = if (state.scheduleConfigured) state.scheduleUrl else ""
                    urlError = ""
                    showUrlDialog = true
                },
            )
            SettingsRow(
                title = "Обновить сейчас",
                subtitle = "Скачать актуальный JSON с GitHub",
                leading = Icons.Rounded.Refresh,
                onClick = onRefresh,
            )
            SettingsRow(
                title = "Автообновление",
                subtitle = "Ежедневно в 18:00 и 20:30, при запуске и возврате в приложение",
                leading = Icons.Rounded.Sync,
                showDivider = false,
            )
        }
        if (state.lastError != null) {
            UpdateErrorBanner(
                text = state.lastError.orEmpty(),
                onRetry = onRefresh,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Spacer(Modifier.height(22.dp))
        SettingsGroup(title = "Чётность недели") {
            SettingsRow(
                title = "Определять автоматически",
                subtitle = if (state.parityIsManual) {
                    "Сейчас $week-я неделя · ${weekLabel(parity)} (вручную)"
                } else {
                    "$week-я неделя семестра · ${weekLabel(parity)}"
                },
                leading = Icons.Rounded.Sync,
                trailing = {
                    DesignSwitch(
                        checked = !state.parityIsManual,
                        onCheckedChange = { auto ->
                            if (auto) {
                                state.setParityAuto()
                                onToast("Чётность определяется автоматически")
                            } else {
                                state.setParityOverride(if (parity == WeekParity.ODD) 1 else 2)
                                onToast("Теперь чётность можно выбрать вручную")
                            }
                        },
                    )
                },
                showDivider = state.parityIsManual,
            )
            if (state.parityIsManual) {
                Column(Modifier.padding(14.dp)) {
                    DesignSegmented(
                        options = WeekParity.entries,
                        selected = parity,
                        label = { weekLabel(it).replaceFirstChar { c -> c.uppercase() } },
                        onSelect = {
                            state.setParityOverride(if (it == WeekParity.ODD) 1 else 2)
                            onToast("Выбрана ${weekLabel(it)} неделя")
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        SettingsGroup(title = "О приложении") {
            SettingsRow(
                title = "Группа",
                subtitle = "Расписание на ${snapshot.semester} семестр",
                leading = Icons.Rounded.Groups,
                trailing = {
                    Text(
                        snapshot.group,
                        style = d.type.bodySmall.copy(
                            color = d.colors.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                },
            )
            SettingsRow(
                title = "Расписание в JSON",
                subtitle = "Обновляется из GitHub: data/schedule.json",
                leading = Icons.Rounded.AutoAwesome,
                trailing = {
                    Text(
                        formatDateTime(snapshot.updatedAt),
                        style = d.type.labelSmall.copy(color = d.colors.primary),
                    )
                },
            )
            SettingsRow(
                title = "Версия",
                leading = Icons.Rounded.Info,
                trailing = {
                    Text(
                        BuildConfig.VERSION_NAME,
                        style = d.type.bodySmall,
                        color = d.colors.onSurfaceVariant,
                    )
                },
                showDivider = false,
            )
        }

        Spacer(Modifier.height(22.dp))
        Text(
            "${snapshot.group} · ${snapshot.academicYear}\nMaterial You 3 Expressive × MIUIX",
            style = d.type.labelSmall,
            color = d.colors.onSurfaceVariant.copy(alpha = 0.65f),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(10.dp))
    }

    if (showUrlDialog) {
        ScheduleUrlDialog(
            value = urlDraft,
            error = urlError,
            onValueChange = {
                urlDraft = it
                urlError = ""
            },
            onReset = {
                state.resetScheduleUrl()
                showUrlDialog = false
                onToast("Используется URL по умолчанию")
            },
            onDismiss = { showUrlDialog = false },
            onSave = {
                val url = urlDraft.trim()
                if (!isAllowedScheduleUrl(url)) {
                    urlError = "Нужна HTTPS-ссылка (HTTP разрешён только для localhost)"
                } else {
                    state.setScheduleUrl(url)
                    showUrlDialog = false
                    onToast("Источник сохранён")
                }
            },
        )
    }
}

@Composable
private fun ScheduleUrlDialog(
    value: String,
    error: String,
    onValueChange: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val d = LocalDesign.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .clip(d.shapes.card)
                .background(d.colors.surfaceContainerHigh)
                .padding(20.dp),
        ) {
            Text(
                "Источник расписания",
                style = d.type.title,
                color = d.colors.onSurface,
            )
            Text(
                "Прямая ссылка на JSON в репозитории GitHub (raw.githubusercontent.com).",
                style = d.type.bodySmall,
                color = d.colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(14.dp))
            DesignTextField(
                value = value,
                onValueChange = onValueChange,
                label = "URL файла",
                placeholder = "https://raw.githubusercontent.com/…/data/schedule.json",
                singleLine = false,
                minLines = 2,
            )
            if (error.isNotBlank()) {
                Text(
                    error,
                    style = d.type.labelSmall.copy(color = d.colors.error),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DesignTextButton("Сбросить", onReset, color = d.colors.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                DesignTextButton("Отмена", onDismiss, color = d.colors.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                DesignTextButton("Сохранить", onSave, color = d.colors.primary)
            }
        }
    }
}

/* ============================================================
 *  Theme picker
 * ============================================================ */

@Composable
private fun ThemeStyleCard(
    style: ThemeStyle,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val d = LocalDesign.current
    val shape = d.shapes.card
    val bg = if (d.isMiuix) d.colors.surface else d.colors.surfaceContainerLow
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .border(
                width = if (selected) 2.dp else if (d.style == ThemeStyle.HYBRID) 1.dp else 0.dp,
                color = when {
                    selected -> d.colors.primary
                    d.style == ThemeStyle.HYBRID -> d.colors.outlineVariant.copy(alpha = 0.5f)
                    else -> Color.Transparent
                },
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ThemePreview(style, Modifier.size(width = 104.dp, height = 78.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = when (style) {
                        ThemeStyle.MATERIAL -> "Material 3 Expressive"
                        ThemeStyle.MIUIX -> "MIUIX"
                        ThemeStyle.HYBRID -> "Miuix × Expressive"
                    },
                    style = d.type.cardTitle.copy(fontSize = 15.sp),
                    color = d.colors.onSurface,
                )
                Text(
                    text = when (style) {
                        ThemeStyle.MATERIAL -> "Динамический цвет, крупные формы, пружинная анимация"
                        ThemeStyle.MIUIX -> "HyperOS + официальная библиотека Miuix, Material You (Monet)"
                        ThemeStyle.HYBRID -> "Пластика Material 3 и спокойный характер MIUIX"
                    },
                    style = d.type.bodySmall.copy(
                        color = d.colors.onSurfaceVariant,
                        lineHeight = 16.sp,
                        letterSpacing = 0.sp,
                    ),
                    modifier = Modifier.padding(top = 3.dp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(Modifier.padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    val chipColor = when (style) {
                        ThemeStyle.MATERIAL -> Color(0xFF6750A4)
                        ThemeStyle.MIUIX -> Color(0xFF3482FF)
                        ThemeStyle.HYBRID -> Color(0xFF8A4F7D)
                    }
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(chipColor),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        when (style) {
                            ThemeStyle.MATERIAL -> "Material You"
                            ThemeStyle.MIUIX -> "Miuix + Monet"
                            ThemeStyle.HYBRID -> "Hybrid"
                        },
                        style = d.type.labelSmall.copy(
                            color = d.colors.onSurfaceVariant,
                            letterSpacing = 0.sp,
                        ),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (selected) d.colors.primary else Color.Transparent)
                    .border(2.dp, if (selected) d.colors.primary else d.colors.outlineVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(Icons.Rounded.Check, null, tint = d.colors.onPrimary, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
private fun ThemePreview(style: ThemeStyle, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(15.dp)
    when (style) {
        ThemeStyle.MATERIAL -> {
            Box(
                modifier
                    .clip(shape)
                    .background(Brush.linearGradient(listOf(Color(0xFF6750A4), Color(0xFFB69DF8)))),
            ) {
                Box(
                    Modifier
                        .offset(x = 66.dp, y = (-18).dp)
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f)),
                )
                Column(
                    Modifier.align(Alignment.BottomStart).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        Modifier.width(58.dp).height(7.dp).clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.85f)),
                    )
                    Box(
                        Modifier.width(42.dp).height(5.dp).clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.45f)),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(2) {
                            Box(
                                Modifier.width(26.dp).height(14.dp).clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.22f)),
                            )
                        }
                    }
                }
            }
        }

        ThemeStyle.MIUIX -> {
            Column(
                modifier
                    .clip(shape)
                    .background(Color(0xFFF7F7F7))
                    .padding(7.dp),
            ) {
                Box(
                    Modifier.fillMaxWidth().height(25.dp).clip(RoundedCornerShape(9.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF3482FF), Color(0xFF6AA5FF)))),
                ) {
                    Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(
                            Modifier.width(31.dp).height(4.dp).clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.9f)),
                        )
                        Box(
                            Modifier.width(18.dp).height(3.dp).clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.5f)),
                        )
                    }
                }
                Spacer(Modifier.height(5.dp))
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(Color.White)
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    repeat(2) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(8.dp).clip(RoundedCornerShape(3.dp))
                                    .background(if (it == 0) Color(0xFF3482FF) else Color(0xFF34C759)),
                            )
                            Spacer(Modifier.width(4.dp))
                            Box(
                                Modifier.width(if (it == 0) 31.dp else 24.dp).height(3.dp).clip(CircleShape)
                                    .background(Color(0xFFD9D9DE)),
                            )
                        }
                    }
                }
            }
        }

        ThemeStyle.HYBRID -> {
            Box(
                modifier
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 9.dp, bottomEnd = 20.dp, bottomStart = 7.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF6750A4), Color(0xFF984061)))),
            ) {
                Box(
                    Modifier.offset(x = 58.dp, y = 8.dp).size(50.dp).clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f)),
                )
                Column(
                    Modifier.padding(8.dp).fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Box(
                        Modifier.width(44.dp).height(10.dp).clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.25f)),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            Modifier.fillMaxWidth().height(22.dp)
                                .clip(RoundedCornerShape(topStart = 9.dp, topEnd = 4.dp, bottomEnd = 9.dp, bottomStart = 3.dp))
                                .background(Color.White.copy(alpha = 0.20f)),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                Modifier.width(30.dp).height(12.dp).clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.85f)),
                            )
                            Box(
                                Modifier.width(20.dp).height(12.dp).clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.30f)),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteSwatch(
    color: Color,
    name: String,
    system: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val d = LocalDesign.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(58.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .border(
                    if (selected) 3.dp else 1.dp,
                    if (selected) d.colors.primary else d.colors.outlineVariant,
                    CircleShape,
                )
                .padding(if (selected) 4.dp else 0.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(if (selected) 34.dp else 42.dp)
                    .clip(CircleShape)
                    .background(
                        if (system) {
                            Brush.sweepGradient(
                                listOf(
                                    Color(0xFF4285F4), Color(0xFF34A853),
                                    Color(0xFFFBBC04), Color(0xFFEA4335), Color(0xFF4285F4),
                                )
                            )
                        } else {
                            Brush.linearGradient(listOf(color, color.copy(alpha = 0.85f)))
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
        Text(
            name,
            style = d.type.labelSmall.copy(
                color = if (selected) d.colors.primary else d.colors.onSurfaceVariant,
                fontSize = 9.sp,
                letterSpacing = 0.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            ),
            modifier = Modifier.padding(top = 5.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
