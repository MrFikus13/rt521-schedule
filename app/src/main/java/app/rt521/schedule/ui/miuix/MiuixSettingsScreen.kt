package app.rt521.schedule.ui.miuix

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.rt521.schedule.BuildConfig
import app.rt521.schedule.data.AppState
import app.rt521.schedule.data.ThemeMode
import app.rt521.schedule.data.ThemeStyle
import app.rt521.schedule.data.WeekParity
import app.rt521.schedule.data.formatDateTime
import app.rt521.schedule.data.isAllowedScheduleUrl
import app.rt521.schedule.data.semesterWeek
import app.rt521.schedule.data.weekLabel
import app.rt521.schedule.ui.theme.Palettes
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SpinnerEntry
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.icon.extended.Update
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlaySpinnerPreference
import top.yukonga.miuix.kmp.preference.RadioButtonLocation
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MiuixSettingsScreen(
    state: AppState,
    innerPadding: PaddingValues,
    scrollBehavior: ScrollBehavior,
    onRefresh: () -> Unit,
    onToast: (String) -> Unit,
) {
    val snapshot = state.schedule
    val palettes = Palettes.forStyle(ThemeStyle.MIUIX)
    val currentPaletteId = Palettes.resolvedId(ThemeStyle.MIUIX, state.settings.palette)
    val currentPaletteIndex = palettes.indexOfFirst { it.id == currentPaletteId }.coerceAtLeast(0)
    val week = semesterWeek(snapshot.semesterStart, state.now.toLocalDate())
    val parity = state.parityFor(state.now.toLocalDate())
    var showUrlDialog by remember { mutableStateOf(false) }
    var urlDraft by remember { mutableStateOf(TextFieldValue("")) }
    var urlError by remember { mutableStateOf("") }

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
        item(key = "themeTitle") { MiuixSectionTitle("Тема приложения") }
        item(key = "theme") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(0.dp),
            ) {
                ThemeStyle.entries.forEachIndexed { index, style ->
                    if (index > 0) HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                    RadioButtonPreference(
                        title = when (style) {
                            ThemeStyle.MATERIAL -> "Material 3 Expressive"
                            ThemeStyle.MIUIX -> "MIUIX"
                            ThemeStyle.HYBRID -> "Miuix × Expressive"
                        },
                        summary = when (style) {
                            ThemeStyle.MATERIAL -> "Material You, крупные формы и пружины"
                            ThemeStyle.MIUIX -> "Официальная Miuix + Monet Material You"
                            ThemeStyle.HYBRID -> "Структура Miuix и пластика Material 3"
                        },
                        selected = state.settings.themeStyle == style,
                        onClick = { state.setThemeStyle(style) },
                        radioButtonLocation = RadioButtonLocation.End,
                    )
                }
            }
        }

        item(key = "colorsTitle") { MiuixSectionTitle("Цвета MIUIX") }
        item(key = "colors") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(0.dp),
            ) {
                OverlaySpinnerPreference(
                    items = palettes.map { palette ->
                        SpinnerEntry(
                            title = palette.name,
                            summary = when (palette.id) {
                                "system" -> "Палитра обоев через Material You (Monet)"
                                "miui" -> "Оригинальная синяя схема Miuix"
                                else -> "Monet Material You из этого акцента"
                            },
                        )
                    },
                    selectedIndex = currentPaletteIndex,
                    title = "Цветовая схема",
                    summary = palettes[currentPaletteIndex].name,
                    startAction = {
                        Icon(
                            MiuixIcons.Theme,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    onSelectedIndexChange = { index ->
                        val palette = palettes[index]
                        state.setPalette(palette.id)
                        onToast("Цвет: ${palette.name}")
                    },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                BasicComponent(
                    title = "Material You",
                    summary = "Monet строит палитру Material You из акцента или обоев — прямо как в системной теме Android.",
                    startAction = {
                        Icon(
                            MiuixIcons.Theme,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                )
            }
        }

        item(key = "modeTitle") { MiuixSectionTitle("Режим") }
        item(key = "mode") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(12.dp),
            ) {
                TabRow(
                    tabs = listOf("Светлая", "Тёмная", "Системная"),
                    selectedTabIndex = ThemeMode.entries.indexOf(state.settings.themeMode),
                    onTabSelected = { state.setThemeMode(ThemeMode.entries[it]) },
                )
            }
        }

        item(key = "updatesTitle") { MiuixSectionTitle("Обновления") }
        item(key = "updates") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(0.dp),
            ) {
                ArrowPreference(
                    title = "Обновить сейчас",
                    summary = if (state.isRefreshing) "Загрузка…" else "Скачать актуальный JSON с GitHub",
                    startAction = {
                        Icon(
                            MiuixIcons.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    onClick = onRefresh,
                )
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                ArrowPreference(
                    title = "Источник расписания",
                    summary = if (!state.scheduleConfigured) {
                        "Не настроено · укажите raw-ссылку на data/schedule.json"
                    } else {
                        state.scheduleUrl
                    },
                    startAction = {
                        Icon(
                            MiuixIcons.Update,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    onClick = {
                        urlDraft = TextFieldValue(if (state.scheduleConfigured) state.scheduleUrl else "")
                        urlError = ""
                        showUrlDialog = true
                    },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                BasicComponent(
                    title = "Последнее обновление",
                    summary = formatDateTime(state.lastFetchAt),
                    startAction = {
                        Icon(
                            MiuixIcons.Update,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                BasicComponent(
                    title = "Автообновление",
                    summary = "Ежедневно в 18:00 и 20:30, а также при запуске и возврате в приложение",
                    startAction = {
                        Icon(
                            MiuixIcons.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                )
                val error = state.lastError
                if (error != null) {
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                    BasicComponent(
                        title = "Ошибка обновления",
                        summary = error,
                        startAction = {
                            Icon(
                                MiuixIcons.Info,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MiuixTheme.colorScheme.error,
                            )
                        },
                    )
                }
            }
        }

        item(key = "parityTitle") { MiuixSectionTitle("Чётность недели") }
        item(key = "parity") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(0.dp),
            ) {
                SwitchPreference(
                    title = "Определять автоматически",
                    summary = if (state.parityIsManual) {
                        "$week-я неделя · ${weekLabel(parity)} (вручную)"
                    } else {
                        "$week-я неделя семестра · ${weekLabel(parity)}"
                    },
                    checked = !state.parityIsManual,
                    onCheckedChange = { auto ->
                        if (auto) state.setParityAuto() else state.setParityOverride(
                            if (parity == WeekParity.ODD) 1 else 2,
                        )
                    },
                )
                if (state.parityIsManual) {
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        TextButton(
                            text = "Нечётная",
                            onClick = { state.setParityOverride(1) },
                        )
                        Spacer(Modifier.width(8.dp))
                        TextButton(
                            text = "Чётная",
                            onClick = { state.setParityOverride(2) },
                        )
                    }
                }
            }
        }

        item(key = "aboutTitle") { MiuixSectionTitle("О приложении") }
        item(key = "about") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(0.dp),
            ) {
                BasicComponent(
                    title = "Группа",
                    summary = "${snapshot.group} · ${snapshot.semester} семестр · ${snapshot.academicYear}",
                    startAction = {
                        Icon(
                            MiuixIcons.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                BasicComponent(
                    title = "Расписание в JSON",
                    summary = "data/schedule.json · обновлено ${formatDateTime(snapshot.updatedAt)}",
                    startAction = {
                        Icon(
                            MiuixIcons.Info,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                BasicComponent(
                    title = "Версия",
                    summary = BuildConfig.VERSION_NAME,
                    startAction = {
                        Icon(
                            MiuixIcons.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                )
            }
        }

        item(key = "footer") {
            Text(
                "РТ-521 · ${snapshot.academicYear} · Miuix + Material You",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
    }

    OverlayDialog(
        show = showUrlDialog,
        title = "Источник расписания",
        summary = "Прямая ссылка на JSON в репозитории GitHub (raw.githubusercontent.com).",
        onDismissRequest = { showUrlDialog = false },
    ) {
        Column(Modifier.fillMaxWidth()) {
            TextField(
                value = urlDraft,
                onValueChange = {
                    urlDraft = it
                    urlError = ""
                },
                label = "URL файла",
                singleLine = false,
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            if (urlError.isNotBlank()) {
                Text(
                    urlError,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    text = "Сбросить",
                    onClick = {
                        state.resetScheduleUrl()
                        showUrlDialog = false
                        onToast("Используется URL по умолчанию")
                    },
                )
                Spacer(Modifier.weight(1f))
                TextButton(text = "Отмена", onClick = { showUrlDialog = false })
                Spacer(Modifier.width(4.dp))
                TextButton(
                    text = "Сохранить",
                    onClick = {
                        val url = urlDraft.text.trim()
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
    }
}

@Composable
private fun MiuixSectionTitle(text: String) {
    SmallTitle(
        text = text,
        insideMargin = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
    )
}

@Composable
private fun ThemeDot(style: ThemeStyle) {
    val brush = when (style) {
        ThemeStyle.MATERIAL -> Brush.linearGradient(listOf(Color(0xFF6750A4), Color(0xFFB69DF8)))
        ThemeStyle.MIUIX -> Brush.linearGradient(listOf(Color(0xFF3482FF), Color(0xFF6AA5FF)))
        ThemeStyle.HYBRID -> Brush.linearGradient(listOf(Color(0xFF6750A4), Color(0xFF984061)))
    }
    Box(
        Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(brush),
    )
}
