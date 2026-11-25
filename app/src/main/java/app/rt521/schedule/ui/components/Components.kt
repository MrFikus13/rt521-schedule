package app.rt521.schedule.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.rt521.schedule.data.ThemeStyle
import app.rt521.schedule.ui.theme.LocalDesign
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/* ============================================================
 *  Background
 * ============================================================ */

@Composable
fun AppBackground(modifier: Modifier = Modifier) {
    val d = LocalDesign.current
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(d.backgroundGradient)),
    ) {
        if (d.style != ThemeStyle.MIUIX) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(d.colors.primary.copy(alpha = if (d.dark) 0.18f else 0.13f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(w * 0.88f, h * 0.06f),
                        radius = w * 0.75f,
                    ),
                    radius = w * 0.75f,
                    center = androidx.compose.ui.geometry.Offset(w * 0.88f, h * 0.06f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(d.colors.tertiary.copy(alpha = if (d.dark) 0.12f else 0.09f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(w * 0.02f, h * 0.74f),
                        radius = w * 0.65f,
                    ),
                    radius = w * 0.65f,
                    center = androidx.compose.ui.geometry.Offset(w * 0.02f, h * 0.74f),
                )
            }
        }
    }
}

/* ============================================================
 *  Press / surface helpers
 * ============================================================ */

fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.96f,
): Modifier {
    // The caller must observe the interaction source elsewhere; kept for API completeness.
    return this
}

@Composable
fun rememberPressScale(interactionSource: MutableInteractionSource, pressedScale: Float = 0.96f): Float {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 700f),
        label = "pressScale",
    )
    return scale
}

/* ============================================================
 *  Buttons / icon buttons
 * ============================================================ */

@Composable
fun DesignIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    tint: Color? = null,
    container: Color? = null,
) {
    val d = LocalDesign.current
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction, 0.9f)
    val bg = container ?: when {
        d.isMiuix -> d.miuix?.surfaceSecondary ?: d.colors.surfaceVariant
        active -> d.colors.primaryContainer
        else -> d.colors.surfaceContainerHigh
    }
    val fg = tint ?: when {
        active -> d.colors.primary
        d.isMiuix -> d.miuix?.textPrimary ?: d.colors.onSurface
        else -> d.colors.onSurfaceVariant
    }
    Box(
        modifier
            .size(44.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(bg)
            .clickable(
                interactionSource = interaction,
                indication = androidx.compose.material3.ripple(color = fg),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = fg, modifier = Modifier.size(21.dp))
    }
}

@Composable
fun DesignButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    tonal: Boolean = false,
    danger: Boolean = false,
) {
    val d = LocalDesign.current
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction, 0.975f)
    val bg: Brush = when {
        !enabled -> SolidColor(d.colors.onSurface.copy(alpha = 0.12f))
        danger -> SolidColor(d.colors.error)
        tonal -> SolidColor(
            if (d.isMiuix) d.colors.primaryContainer
            else d.colors.secondaryContainer,
        )
        d.style == ThemeStyle.HYBRID -> Brush.horizontalGradient(listOf(d.colors.primary, d.colors.tertiary))
        else -> SolidColor(d.colors.primary)
    }
    val fg = when {
        !enabled -> d.colors.onSurface.copy(alpha = 0.38f)
        danger -> d.colors.onError
        tonal -> if (d.isMiuix) d.colors.primary else d.colors.onSecondaryContainer
        else -> d.colors.onPrimary
    }
    Row(
        modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(d.shapes.button)
            .background(bg)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = androidx.compose.material3.ripple(color = fg),
                onClick = onClick,
            )
            .heightIn(min = 52.dp)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(9.dp))
        }
        Text(
            text = text,
            color = fg,
            style = d.type.label.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun DesignTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val d = LocalDesign.current
    val fg = when {
        !enabled -> d.colors.onSurface.copy(alpha = 0.38f)
        else -> color ?: d.colors.primary
    }
    Row(
        modifier
            .clip(d.shapes.button)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
        }
        Text(text, color = fg, style = d.type.label.copy(fontWeight = FontWeight.Bold))
    }
}

/* ============================================================
 *  Pills, badges, segmented controls
 * ============================================================ */

@Composable
fun DesignPill(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    container: Color? = null,
    content: Color? = null,
    onClick: (() -> Unit)? = null,
    small: Boolean = false,
) {
    val d = LocalDesign.current
    val bg = container ?: if (d.isMiuix) {
        d.miuix?.surfaceSecondary ?: d.colors.surfaceVariant
    } else {
        d.colors.secondaryContainer
    }
    val fg = content ?: if (d.isMiuix) {
        d.miuix?.textSecondary ?: d.colors.onSurfaceVariant
    } else {
        d.colors.onSecondaryContainer
    }
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction, 0.94f)
    Row(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(d.shapes.chip)
            .background(bg)
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = interaction,
                    indication = androidx.compose.material3.ripple(color = fg),
                    onClick = onClick,
                ) else Modifier
            )
            .padding(horizontal = if (small) 9.dp else 12.dp, vertical = if (small) 4.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(if (small) 13.dp else 15.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text = text,
            color = fg,
            style = if (small) d.type.labelSmall else d.type.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun <T> DesignSegmented(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    equalWidth: Boolean = true,
) {
    val d = LocalDesign.current
    val bg = when {
        d.isMiuix -> d.miuix?.surfaceSecondary ?: d.colors.surfaceVariant
        d.style == ThemeStyle.HYBRID -> d.colors.surfaceContainerHigh
        else -> d.colors.surfaceContainerHighest
    }
    Row(
        modifier
            .fillMaxWidth()
            .clip(if (d.isMiuix) RoundedCornerShape(12.dp) else CircleShape)
            .background(bg)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val itemBg = when {
                !isSelected -> Color.Transparent
                d.isMiuix -> d.colors.surface
                else -> d.colors.primary
            }
            val itemFg = when {
                isSelected && d.isMiuix -> d.colors.primary
                isSelected -> d.colors.onPrimary
                else -> if (d.isMiuix) d.miuix?.textSecondary ?: d.colors.onSurfaceVariant else d.colors.onSurfaceVariant
            }
            Row(
                modifier = Modifier
                    .then(if (equalWidth) Modifier.weight(1f) else Modifier)
                    .clip(if (d.isMiuix) RoundedCornerShape(9.dp) else CircleShape)
                    .background(itemBg)
                    .clickable { onSelect(option) }
                    .heightIn(min = 38.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label(option),
                    color = itemFg,
                    style = d.type.label.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/* ============================================================
 *  Switch
 * ============================================================ */

@Composable
fun DesignSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = LocalDesign.current
    val track by androidx.compose.animation.animateColorAsState(
        targetValue = if (checked) d.colors.primary else d.colors.surfaceContainerHighest,
        animationSpec = tween(180),
        label = "switchTrack",
    )
    val thumbX by animateDpAsState(
        targetValue = if (checked) 20.dp else 2.dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 700f),
        label = "switchThumb",
    )
    Box(
        modifier
            .width(46.dp)
            .height(26.dp)
            .clip(CircleShape)
            .background(if (d.isMiuix && !checked) d.miuix?.surfaceSecondary ?: track else track)
            .clickable { onCheckedChange(!checked) }
            .padding(2.dp),
    ) {
        Box(
            Modifier
                .offset { IntOffset(thumbX.roundToPx(), 0) }
                .size(22.dp)
                .shadow(1.dp, CircleShape)
                .clip(CircleShape)
                .background(if (checked) d.colors.onPrimary else d.colors.surface),
        )
    }
}

/* ============================================================
 *  Text fields
 * ============================================================ */

@Composable
fun DesignTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    onDone: (() -> Unit)? = null,
) {
    val d = LocalDesign.current
    var focused by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val container = when {
        d.isMiuix -> d.miuix?.surfaceSecondary ?: d.colors.surfaceVariant
        else -> d.colors.surfaceContainerLowest
    }
    val borderColor = when {
        focused -> d.colors.primary
        d.isMiuix -> Color.Transparent
        else -> d.colors.outlineVariant
    }
    Column(modifier) {
        if (label != null) {
            Text(
                text = label,
                style = if (d.isMiuix) {
                    d.type.bodySmall.copy(color = d.miuix?.textSecondary ?: d.colors.onSurfaceVariant)
                } else {
                    d.type.labelSmall.copy(
                        color = d.colors.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                modifier = Modifier.padding(start = 2.dp, bottom = 6.dp),
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(d.shapes.field)
                .background(container)
                .border(if (focused) 1.5.dp else 1.dp, borderColor, d.shapes.field)
                .padding(horizontal = 14.dp, vertical = if (singleLine) 0.dp else 12.dp)
                .heightIn(min = 52.dp),
            verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
        ) {
            if (leadingIcon != null) {
                Icon(
                    leadingIcon, null,
                    tint = d.colors.onSurfaceVariant,
                    modifier = Modifier.size(19.dp),
                )
                Spacer(Modifier.width(9.dp))
            }
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        style = d.type.body.copy(color = d.colors.onSurfaceVariant.copy(alpha = 0.65f)),
                        maxLines = if (singleLine) 1 else 4,
                        modifier = Modifier.padding(top = if (singleLine) 0.dp else 1.dp),
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    textStyle = d.type.body.copy(color = d.colors.onSurface),
                    cursorBrush = SolidColor(d.colors.primary),
                    singleLine = singleLine,
                    minLines = minLines,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
                        imeAction = imeAction,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            onDone?.invoke()
                        },
                    ),
                    visualTransformation = if (isPassword && !passwordVisible) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focused = it.isFocused },
                )
            }
            if (isPassword) {
                Icon(
                    if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    "Показать пароль",
                    tint = d.colors.onSurfaceVariant,
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { passwordVisible = !passwordVisible }
                        .padding(13.dp),
                )
            }
        }
    }
}

/* ============================================================
 *  Layout pieces
 * ============================================================ */

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    val d = LocalDesign.current
    Text(
        text = if (d.isMiuix) text else text.uppercase(),
        style = if (d.isMiuix) {
            d.type.bodySmall.copy(
                color = d.miuix?.textSecondary ?: d.colors.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
        } else {
            d.type.labelSmall.copy(color = d.colors.onSurfaceVariant)
        },
        modifier = modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

@Composable
fun SettingsGroup(
    title: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val d = LocalDesign.current
    Column(modifier) {
        if (title != null) {
            SectionLabel(title)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(d.shapes.card)
                .background(if (d.isMiuix) d.colors.surface else d.colors.surfaceContainerLow)
                .then(
                    if (d.style == ThemeStyle.HYBRID) {
                        Modifier.border(1.dp, d.colors.outlineVariant.copy(alpha = 0.6f), d.shapes.card)
                    } else Modifier
                ),
            content = content,
        )
    }
}

@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: ImageVector? = null,
    leadingTint: Color? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = true,
    danger: Boolean = false,
) {
    val d = LocalDesign.current
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) Modifier.clickable(onClick = onClick)
                    else Modifier
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(if (d.isMiuix) RoundedCornerShape(10.dp) else CircleShape)
                        .background(
                            if (d.isMiuix) (leadingTint ?: d.colors.primary).copy(alpha = 0.12f)
                            else d.colors.surfaceContainerHighest
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        leading, null,
                        tint = leadingTint ?: if (d.isMiuix) d.colors.primary else d.colors.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = d.type.body.copy(
                        fontWeight = FontWeight.Medium,
                        color = if (danger) d.colors.error else d.colors.onSurface,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = d.type.bodySmall,
                        color = d.colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(10.dp))
                trailing()
            }
        }
        if (showDivider) {
            Box(
                Modifier
                    .padding(start = if (leading != null) 62.dp else 16.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(d.colors.outlineVariant.copy(alpha = if (d.isMiuix) 0.55f else 0.4f)),
            )
        }
    }
}

/* ============================================================
 *  Sheet / dialog
 * ============================================================ */

@Composable
fun AppSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val d = LocalDesign.current
    val transition = remember { MutableTransitionState(false).apply { targetState = true } }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val maxHeight = with(density) { (windowInfo.containerSize.height * 0.92f).toDp() }
    val dismissAnimated: () -> Unit = {
        transition.targetState = false
        scope.launch {
            delay(180)
            onDismiss()
        }
    }
    androidx.activity.compose.BackHandler(onBack = dismissAnimated)

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = dismissAnimated,
                ),
        )
        AnimatedVisibility(
            visibleState = transition,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 520f),
            ) + fadeIn(tween(120)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(180),
            ) + fadeOut(tween(140)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .heightIn(max = maxHeight)
                    .shadow(18.dp, d.shapes.sheet, clip = false)
                    .clip(d.shapes.sheet)
                    .background(
                        if (d.isMiuix) d.colors.surface else d.colors.surfaceContainerHigh
                    )
                    .imePadding()
                    .navigationBarsPaddingCompat(),
            ) {
                Box(
                    Modifier
                        .padding(top = 10.dp, bottom = 4.dp)
                        .size(width = 34.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(d.colors.outlineVariant)
                        .align(Alignment.CenterHorizontally),
                )
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun Modifier.navigationBarsPaddingCompat(): Modifier =
    this.then(
        Modifier.padding(
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        )
    )

@Composable
fun SheetHeader(title: String, subtitle: String? = null, onClose: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            SheetTitle(title, subtitle)
        }
        if (onClose != null) {
            Spacer(Modifier.width(8.dp))
            StyledCloseButton(onClose)
        }
    }
}

@Composable
fun SheetTitle(title: String, subtitle: String? = null) {
    val d = LocalDesign.current
    Text(
        title,
        style = if (d.isMiuix) d.type.title else d.type.title.copy(fontWeight = FontWeight.ExtraBold),
        color = d.colors.onSurface,
    )
    if (subtitle != null) {
        Text(
            subtitle,
            style = d.type.bodySmall,
            color = d.colors.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String = "Удалить",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = true,
) {
    val d = LocalDesign.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            Modifier
                .padding(28.dp)
                .fillMaxWidth()
                .clip(d.shapes.card)
                .background(d.colors.surfaceContainerHigh)
                .padding(22.dp),
        ) {
            Text(title, style = d.type.title, color = d.colors.onSurface)
            Text(
                text,
                style = d.type.body,
                color = d.colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                DesignTextButton("Отмена", onDismiss, color = d.colors.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                DesignTextButton(
                    confirmLabel,
                    onConfirm,
                    color = if (danger) d.colors.error else d.colors.primary,
                )
            }
        }
    }
}

/* ============================================================
 *  Subject glyph / empty state
 * ============================================================ */

@Composable
fun SubjectGlyph(
    abbreviation: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val d = LocalDesign.current
    val shape = when (d.style) {
        ThemeStyle.MATERIAL -> RoundedCornerShape(14.dp)
        ThemeStyle.MIUIX -> RoundedCornerShape(12.dp)
        ThemeStyle.HYBRID -> RoundedCornerShape(
            topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 5.dp,
        )
    }
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(color.copy(alpha = if (d.dark) 0.30f else 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            abbreviation,
            color = color,
            style = d.type.label.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = if (size >= 44.dp) 14.sp else 11.sp,
            ),
        )
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val d = LocalDesign.current
    Column(
        modifier
            .fillMaxWidth()
            .clip(d.shapes.card)
            .background(if (d.isMiuix) d.colors.surface else d.colors.surfaceContainerLow)
            .padding(horizontal = 20.dp, vertical = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(if (d.isMiuix) RoundedCornerShape(20.dp) else CircleShape)
                .background(d.colors.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = d.colors.onPrimaryContainer, modifier = Modifier.size(28.dp))
        }
        Text(
            title,
            style = d.type.title,
            color = d.colors.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 14.dp),
        )
        Text(
            subtitle,
            style = d.type.body,
            color = d.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/* ============================================================
 *  Toast
 * ============================================================ */

@Composable
fun BoxScope.AppToast(message: String?, onDismiss: () -> Unit, bottomPadding: Dp) {
    val d = LocalDesign.current
    LaunchedEffect(message) {
        if (message != null) {
            delay(2800)
            onDismiss()
        }
    }
    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically(initialOffsetY = { it / 2 }, animationSpec = spring(stiffness = 500f)) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(bottom = bottomPadding),
    ) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
            Row(
                Modifier
                    .shadow(8.dp, d.shapes.chip, clip = false)
                    .clip(d.shapes.chip)
                    .background(d.colors.inverseSurface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(d.colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Check, null, tint = d.colors.onPrimary, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(10.dp))
                Text(
                    message.orEmpty(),
                    style = d.type.body.copy(fontWeight = FontWeight.Medium),
                    color = d.colors.inverseOnSurface,
                    maxLines = 2,
                )
            }
        }
    }
}

/* ============================================================
 *  Misc
 * ============================================================ */

@Composable
fun DividerLine(modifier: Modifier = Modifier, indent: Dp = 0.dp) {
    val d = LocalDesign.current
    Box(
        modifier
            .padding(start = indent)
            .fillMaxWidth()
            .height(1.dp)
            .background(d.colors.outlineVariant.copy(alpha = 0.5f)),
    )
}

@Composable
fun TinyDot(color: Color, size: Dp = 7.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
fun MutedText(text: String, modifier: Modifier = Modifier, maxLines: Int = 2) {
    val d = LocalDesign.current
    Text(
        text,
        style = d.type.bodySmall,
        color = d.colors.onSurfaceVariant,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
fun StyledCloseButton(onClick: () -> Unit) {
    DesignIconButton(Icons.Rounded.Close, "Закрыть", onClick)
}
