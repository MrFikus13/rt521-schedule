package app.rt521.schedule.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.rt521.schedule.data.AppState
import app.rt521.schedule.data.ThemeStyle
import app.rt521.schedule.findActivity
import app.rt521.schedule.ui.components.AppBackground
import app.rt521.schedule.ui.components.AppToast
import app.rt521.schedule.ui.screens.DayScreen
import app.rt521.schedule.ui.screens.SettingsScreen
import app.rt521.schedule.ui.screens.WeekScreen
import app.rt521.schedule.ui.miuix.MiuixShell
import app.rt521.schedule.ui.theme.LocalDesign
import kotlinx.coroutines.delay

@Composable
fun ScheduleRoot(state: AppState) {
    val d = LocalDesign.current
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tab = RootTab.entries[tabIndex]
    var message by remember { mutableStateOf<String?>(null) }
    val today = state.now.toLocalDate()

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            state.tick()
        }
    }

    LaunchedEffect(Unit) {
        state.refreshIfStale()
    }

    LaunchedEffect(state.refreshMessage) {
        state.refreshMessage?.let {
            message = it
            state.clearRefreshMessage()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                state.refreshIfStale()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val view = LocalView.current
    SideEffect {
        if (!view.isInEditMode) {
            view.context.findActivity()?.window?.let { window ->
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !d.dark
                controller.isAppearanceLightNavigationBars = !d.dark
            }
        }
    }

    if (d.style == ThemeStyle.MIUIX) {
        MiuixShell(
            state = state,
            selectedTab = tab,
            onSelectTab = { tabIndex = it.ordinal },
            today = today,
            message = message,
            onMessageShown = { message = null },
            onToast = { message = it },
        )
    } else {
        MaterialShell(
            state = state,
            selectedTab = tab,
            onSelectTab = { tabIndex = it.ordinal },
            today = today,
            message = message,
            onMessageShown = { message = null },
            onToast = { message = it },
        )
    }
}

@Composable
private fun MaterialShell(
    state: AppState,
    selectedTab: RootTab,
    onSelectTab: (RootTab) -> Unit,
    today: java.time.LocalDate,
    message: String?,
    onMessageShown: () -> Unit,
    onToast: (String) -> Unit,
) {
    val metrics = rememberBottomMetrics()

    Box(Modifier.fillMaxSize()) {
        AppBackground()

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) },
            label = "rootTab",
            modifier = Modifier.fillMaxSize(),
        ) { target ->
            when (target) {
                RootTab.DAY -> DayScreen(
                    state = state,
                    today = today,
                    metric = metrics,
                    onOpenSettings = { onSelectTab(RootTab.SETTINGS) },
                    onRefresh = { state.refreshAsync(manual = true) },
                    onToast = onToast,
                )

                RootTab.WEEK -> WeekScreen(
                    state = state,
                    today = today,
                    metric = metrics,
                    onOpenSettings = { onSelectTab(RootTab.SETTINGS) },
                    onRefresh = { state.refreshAsync(manual = true) },
                    onOpenDay = { date ->
                        state.selectDate(date)
                        onSelectTab(RootTab.DAY)
                    },
                    onToast = onToast,
                )

                RootTab.SETTINGS -> SettingsScreen(
                    state = state,
                    metric = metrics,
                    onToast = onToast,
                    onRefresh = { state.refreshAsync(manual = true) },
                    onThemeApplied = { style ->
                        onToast(
                            when (style) {
                                ThemeStyle.MATERIAL -> "Material 3 Expressive включена"
                                ThemeStyle.MIUIX -> "MIUIX включена"
                                ThemeStyle.HYBRID -> "Гибридная тема включена"
                            }
                        )
                    },
                )
            }
        }

        AppBottomBar(
            selected = selectedTab,
            onSelect = onSelectTab,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        AppToast(
            message = message,
            onDismiss = onMessageShown,
            bottomPadding = metrics.contentPadding + 12.dp,
        )
    }
}
