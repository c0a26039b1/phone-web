package com.example.ui.components

import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.SitePermissionType
import com.example.ui.theme.AegisTheme
import com.example.ui.theme.JetBrainsMonoFontFamily

sealed class PendingWebPermissionPrompt {
    abstract val originHost: String
    abstract val requestedTypes: List<SitePermissionType>

    data class Media(
        val tabId: String,
        override val originHost: String,
        val resources: List<String>,
        override val requestedTypes: List<SitePermissionType>,
        val request: PermissionRequest
    ) : PendingWebPermissionPrompt()

    data class Geolocation(
        val tabId: String,
        override val originHost: String,
        val origin: String,
        val callback: GeolocationPermissions.Callback
    ) : PendingWebPermissionPrompt() {
        override val requestedTypes: List<SitePermissionType> = listOf(SitePermissionType.LOCATION)
    }
}

@Composable
fun WebPermissionPromptDialog(
    prompt: PendingWebPermissionPrompt,
    onAllow: (rememberChoice: Boolean) -> Unit,
    onDeny: (rememberChoice: Boolean) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors
    var rememberDecision by remember(prompt) { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = { onDeny(false) },
        containerColor = colorScheme.surface,
        icon = {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = aegisColors.shieldActiveContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        tint = aegisColors.shieldActive,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = stringResource(R.string.site_perm_prompt_title),
                style = MaterialTheme.typography.titleLarge,
                color = colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = prompt.originHost.ifBlank { "このウェブサイト" },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }

                Text(
                    text = "次の端末機能へのアクセスを要求しています：",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    prompt.requestedTypes.forEach { type ->
                        val icon = when (type) {
                            SitePermissionType.CAMERA -> Icons.Filled.Videocam
                            SitePermissionType.MICROPHONE -> Icons.Filled.Mic
                            SitePermissionType.LOCATION -> Icons.Filled.LocationOn
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = aegisColors.shieldActiveContainer.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, aegisColors.shieldActive.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = aegisColors.shieldActive,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = type.labelJa,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { rememberDecision = !rememberDecision }
                        .padding(vertical = 2.dp)
                        .testTag("web_perm_remember_row"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberDecision,
                        onCheckedChange = { rememberDecision = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = aegisColors.shieldActive,
                            checkmarkColor = colorScheme.onPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.site_perm_prompt_remember),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAllow(rememberDecision) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = aegisColors.shieldActive,
                    contentColor = colorScheme.onPrimary
                ),
                modifier = Modifier
                    .height(44.dp)
                    .testTag("web_perm_allow_button")
            ) {
                Text(
                    text = stringResource(R.string.site_perm_allow),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { onDeny(rememberDecision) },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, colorScheme.outlineVariant),
                modifier = Modifier
                    .height(44.dp)
                    .testTag("web_perm_deny_button")
            ) {
                Text(
                    text = stringResource(R.string.site_perm_deny),
                    color = colorScheme.error,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    )
}
