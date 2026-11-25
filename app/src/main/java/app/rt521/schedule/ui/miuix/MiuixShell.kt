package app.rt521.schedule.ui.miuix

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.rt521.schedule.data.AppState
import app.rt521.schedule.ui.RootTab
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarDuration
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Months
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.icon.extended.Weeks
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MiuixShell(
    state: AppState,
    selectedTab: RootTab,
    onSelectTab: (RootTab) -> Unit,
    today: java.time.LocalDate,
    message: String?,
    onMessageShown: () -> Unit,
    onToast: (String) -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
            )
            onMessageShown()
        }
    }

    val title = when (selectedTab) {
        RootTab.DAY -> "Расписание"
        RootTab.WEEK -> "Неделя"
        RootTab.SETTINGS -> "Настройки"
    }
    val subtitle = when (selectedTab) {
        RootTab.DAY -> "${state.schedule.group} · ${state.schedule.semester} семестр"
        RootTab.WEEK -> "Обзор недели · ${state.schedule.academicYear}"
        RootTab.SETTINGS -> "Тема, цвета и обновления"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = title,
                largeTitle = title,
                subtitle = subtitle,
                actions = {
                    MiuixRefreshAction(
                        isRefreshing = state.isRefreshing,
                        onClick = { state.refreshAsync(manual = true) },
                    )
                    if (selectedTab != RootTab.SETTINGS) {
                        IconButton(onClick = { onSelectTab(RootTab.SETTINGS) }) {
                            Icon(
                                MiuixIcons.Theme,
                                contentDescription = "Оформление",
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            NavigationBar {
                MiuixNavItem(
                    selected = selectedTab == RootTab.DAY,
                    onClick = { onSelectTab(RootTab.DAY) },
                    icon = MiuixIcons.Weeks,
                    label = "Сегодня",
                )
                MiuixNavItem(
                    selected = selectedTab == RootTab.WEEK,
                    onClick = { onSelectTab(RootTab.WEEK) },
                    icon = MiuixIcons.Months,
                    label = "Неделя",
                )
                MiuixNavItem(
                    selected = selectedTab == RootTab.SETTINGS,
                    onClick = { onSelectTab(RootTab.SETTINGS) },
                    icon = MiuixIcons.Settings,
                    label = "Настройки",
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (selectedTab) {
            RootTab.DAY -> MiuixDayScreen(
                state = state,
                today = today,
                innerPadding = innerPadding,
                scrollBehavior = scrollBehavior,
                onOpenSettings = { onSelectTab(RootTab.SETTINGS) },
                onRefresh = { state.refreshAsync(manual = true) },
                onToast = onToast,
            )

            RootTab.WEEK -> MiuixWeekScreen(
                state = state,
                today = today,
                innerPadding = innerPadding,
                scrollBehavior = scrollBehavior,
                onOpenDay = { date ->
                    state.selectDate(date)
                    onSelectTab(RootTab.DAY)
                },
                onRefresh = { state.refreshAsync(manual = true) },
                onToast = onToast,
            )

            RootTab.SETTINGS -> MiuixSettingsScreen(
                state = state,
                innerPadding = innerPadding,
                scrollBehavior = scrollBehavior,
                onRefresh = { state.refreshAsync(manual = true) },
                onToast = onToast,
            )
        }
    }
}

@Composable
private fun MiuixRefreshAction(isRefreshing: Boolean, onClick: () -> Unit) {
    if (isRefreshing) {
        IconButton(onClick = {}) {
            CircularProgressIndicator(
                size = 20.dp,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
            )
        }
    } else {
        IconButton(onClick = onClick) {
            Icon(
                MiuixIcons.Refresh,
                contentDescription = "Обновить расписание",
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun RowScope.MiuixNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = icon,
        label = label,
    )
}
