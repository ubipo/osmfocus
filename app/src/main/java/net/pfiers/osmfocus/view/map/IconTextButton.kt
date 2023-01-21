package net.pfiers.osmfocus.view.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign

/**
 * TextButton with an icon on top
 *
 * Like the buttons in this bottom sheet:
 * https://material.io/components/sheets-bottom#standard-bottom-sheet
 *
 * Mostly for actions like "CALL", "WEBSITE" or "SHARE"
 */
@Composable
fun IconTextButton(
    icon: @Composable (() -> Unit),
    text: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    selectedContentColor: Color = LocalContentColor.current,
    onClick: () -> Unit
) {
    val styledText: @Composable () -> Unit = {
        val style = MaterialTheme.typography.button.copy(textAlign = TextAlign.Center)
        ProvideTextStyle(style, content = text)
    }

    val ripple = rememberRipple(bounded = false, color = selectedContentColor)

    Column(
        modifier = modifier
            .clickable(
                onClick = onClick,
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = ripple
            )
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        icon()
        styledText()
    }
}

@Composable
fun IconTextButton(
    icon: ImageVector,
    text: String,
    contentDescriptor: String = text,
    onClick: () -> Unit
) {
    IconTextButton(
        text = { Text(text) },
        icon = { Icon(icon, contentDescription = contentDescriptor) },
        onClick = onClick
    )
}
