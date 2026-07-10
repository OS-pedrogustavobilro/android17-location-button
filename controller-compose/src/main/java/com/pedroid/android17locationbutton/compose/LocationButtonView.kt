package com.pedroid.android17locationbutton.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.locationbutton.compose.LocationButton
import androidx.core.locationbutton.compose.LocationButtonTextType

/**
 * Composable wrapper around the system [LocationButton] that bridges it to a
 * [LocationButtonStateHolder].
 *
 * All visual parameters are optional — when omitted the button falls back to the
 * system's secure, high-contrast defaults.  [Color.Unspecified] is the standard
 * Compose sentinel for "no override"; pass explicit values only when customising.
 *
 * Corner radii are nullable: passing `null` omits the parameter entirely so the
 * library applies its own default, avoiding potential issues with [Dp.Unspecified].
 *
 * The [modifier] is applied to a [Box] wrapper (safe even if the underlying
 * [LocationButton] composable does not expose a modifier parameter).
 *
 * **Interop:** To embed this in a Views layout, wrap a `ComposeView` in XML and
 * call `composeView.setContent { LocationButtonView(...) }`.
 */
@Composable
fun LocationButtonView(
    stateHolder: LocationButtonStateHolder,
    modifier: Modifier = Modifier,
    textType: LocationButtonTextType = LocationButtonTextType.PreciseLocation,
    backgroundColor: Color = Color.Unspecified,
    textColor: Color = Color.Unspecified,
    iconTint: Color = Color.Unspecified,
    cornerRadius: Dp? = null,
    pressedCornerRadius: Dp? = null,
    strokeColor: Color = Color.Unspecified,
    strokeWidth: Dp = 0.dp,
    clickablePadding: PaddingValues = PaddingValues(4.dp),
) {
    if (!stateHolder.isVisible) return

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Use separate call sites so null corner-radius params are never forwarded,
        // letting the library apply its own defaults.
        when {
            cornerRadius != null && pressedCornerRadius != null -> LocationButton(
                onPermissionResult = stateHolder::onPermissionGranted,
                textType = textType,
                backgroundColor = backgroundColor,
                textColor = textColor,
                iconTint = iconTint,
                cornerRadius = cornerRadius,
                pressedCornerRadius = pressedCornerRadius,
                strokeColor = strokeColor,
                strokeWidth = strokeWidth,
                clickablePadding = clickablePadding,
            )
            cornerRadius != null -> LocationButton(
                onPermissionResult = stateHolder::onPermissionGranted,
                textType = textType,
                backgroundColor = backgroundColor,
                textColor = textColor,
                iconTint = iconTint,
                cornerRadius = cornerRadius,
                strokeColor = strokeColor,
                strokeWidth = strokeWidth,
                clickablePadding = clickablePadding,
            )
            pressedCornerRadius != null -> LocationButton(
                onPermissionResult = stateHolder::onPermissionGranted,
                textType = textType,
                backgroundColor = backgroundColor,
                textColor = textColor,
                iconTint = iconTint,
                pressedCornerRadius = pressedCornerRadius,
                strokeColor = strokeColor,
                strokeWidth = strokeWidth,
                clickablePadding = clickablePadding,
            )
            else -> LocationButton(
                onPermissionResult = stateHolder::onPermissionGranted,
                textType = textType,
                backgroundColor = backgroundColor,
                textColor = textColor,
                iconTint = iconTint,
                strokeColor = strokeColor,
                strokeWidth = strokeWidth,
                clickablePadding = clickablePadding,
            )
        }
    }
}
