package app.rt521.schedule.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.rt521.schedule.data.ThemeStyle
import app.rt521.schedule.ui.theme.LocalDesign

enum class RootTab(val label: String, val icon: ImageVector) {
    DAY("День", Icons.Rounded.CalendarMonth),
    WEEK("Неделя", Icons.Rounded.DateRange),
    WORKS("Работы", Icons.Rounded.Assignment),
    SETTINGS("Ещё", Icons.Rounded.Tune),
}

@Composable
fun AppBottomBar(
    selected: RootTab,
    onSelect: (RootTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = LocalDesign.current
    when (d.style) {
        ThemeStyle.MIUIX -> {
            Column(
                modifier
                    .fillMaxWidth()
                    .background(d.colors.surface)
                    .navigationBarsPadding(),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(d.colors.outlineVariant.copy(alpha = 0.7f)),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RootTab.entries.forEach { tab ->
                        MiuixTabItem(
                            tab = tab,
                            selected = tab == selected,
                            onClick = { onSelect(tab) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        ThemeStyle.HYBRID -> {
            Box(
                modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(30.dp),
                    color = d.colors.surfaceContainerHigh,
                    tonalElevation = 0.dp,
                    shadowElevation = 10.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(62.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RootTab.entries.forEach { tab ->
                            HybridTabItem(
                                tab = tab,
                                selected = tab == selected,
                                onClick = { onSelect(tab) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }

        ThemeStyle.MATERIAL -> {
            Column(
                modifier
                    .fillMaxWidth()
                    .background(d.colors.surfaceContainer)
                    .navigationBarsPadding(),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(76.dp)
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    RootTab.entries.forEach { tab ->
                        MaterialTabItem(
                            tab = tab,
                            selected = tab == selected,
                            onClick = { onSelect(tab) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialTabItem(
    tab: RootTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = LocalDesign.current
    val indicator by animateColorAsState(
        if (selected) d.colors.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent,
        label = "materialTabIndicator",
    )
    val tint by animateColorAsState(
        if (selected) d.colors.onSecondaryContainer else d.colors.onSurfaceVariant,
        label = "materialTabTint",
    )
    Column(
        modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .width(56.dp)
                .height(32.dp)
                .clip(CircleShape)
                .background(indicator),
            contentAlignment = Alignment.Center,
        ) {
            Icon(tab.icon, tab.label, tint = tint, modifier = Modifier.size(21.dp))
        }
        Text(
            tab.label,
            color = tint,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun MiuixTabItem(
    tab: RootTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = LocalDesign.current
    val tint by animateColorAsState(
        if (selected) d.colors.primary else d.miuix?.textTertiary ?: d.colors.onSurfaceVariant,
        label = "miuixTabTint",
    )
    Column(
        modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            tab.icon,
            tab.label,
            tint = tint,
            modifier = Modifier.size(23.dp),
        )
        Text(
            tab.label,
            color = tint,
            fontSize = 10.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun HybridTabItem(
    tab: RootTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = LocalDesign.current
    val tint by animateColorAsState(
        if (selected) d.colors.onPrimaryContainer else d.colors.onSurfaceVariant,
        label = "hybridTabTint",
    )
    Row(
        modifier
            .padding(horizontal = 4.dp, vertical = 7.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (selected) d.colors.primaryContainer else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .height(48.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(tab.icon, tab.label, tint = tint, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            tab.label,
            color = tint,
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
fun ScheduleFab(
    onClick: () -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val d = LocalDesign.current
    when (d.style) {
        ThemeStyle.MIUIX -> {
            Box(
                modifier
                    .padding(end = 20.dp, bottom = bottomPadding)
                    .size(56.dp)
                    .shadow(12.dp, CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(d.colors.primary)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Add, "Добавить пару", tint = d.colors.onPrimary, modifier = Modifier.size(26.dp))
            }
        }

        ThemeStyle.HYBRID -> {
            Row(
                modifier
                    .padding(end = 20.dp, bottom = bottomPadding)
                    .shadow(12.dp, RoundedCornerShape(22.dp), clip = false)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.horizontalGradient(listOf(d.colors.primary, d.colors.tertiary)))
                    .clickable(onClick = onClick)
                    .height(56.dp)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Add, "Добавить пару", tint = d.colors.onPrimary, modifier = Modifier.size(21.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Добавить",
                    color = d.colors.onPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        ThemeStyle.MATERIAL -> {
            Row(
                modifier
                    .padding(end = 20.dp, bottom = bottomPadding)
                    .shadow(10.dp, RoundedCornerShape(20.dp), clip = false)
                    .clip(RoundedCornerShape(20.dp))
                    .background(d.colors.primary)
                    .clickable(onClick = onClick)
                    .height(56.dp)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Add, "Добавить пару", tint = d.colors.onPrimary, modifier = Modifier.size(21.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Добавить",
                    color = d.colors.onPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun rememberBottomMetrics(): BottomMetrics {
    val d = LocalDesign.current
    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val barHeight = when (d.style) {
        ThemeStyle.MATERIAL -> 76.dp
        ThemeStyle.MIUIX -> 60.dp
        ThemeStyle.HYBRID -> 82.dp
    }
    val fabBottom = barHeight + nav + 14.dp
    return BottomMetrics(
        navInset = nav,
        barHeight = barHeight,
        fabBottom = fabBottom,
        contentPadding = barHeight + nav + 24.dp,
        fabContentPadding = barHeight + nav + 92.dp,
    )
}

data class BottomMetrics(
    val navInset: Dp,
    val barHeight: Dp,
    val fabBottom: Dp,
    val contentPadding: Dp,
    val fabContentPadding: Dp,
)
