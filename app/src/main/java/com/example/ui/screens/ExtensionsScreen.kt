package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.BrowserExtensionEntity
import com.example.ui.browser.ChromeStoreInstallState
import com.example.ui.browser.ChromeWebStoreDownloader
import com.example.ui.browser.ExtensionEngine
import com.example.ui.theme.AegisTheme
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsScreen(
    extensions: List<BrowserExtensionEntity>,
    extensionsEnabled: Boolean,
    hasActiveWebPage: Boolean,
    chromeStoreInstallState: ChromeStoreInstallState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onToggleMasterEnabled: (Boolean) -> Unit,
    onToggleExtension: (String, Boolean) -> Unit,
    onSaveExtension: (BrowserExtensionEntity) -> Unit,
    onDeleteExtension: (BrowserExtensionEntity) -> Unit,
    onOpenChromeWebStore: (String) -> Unit,
    onInstallFromChromeStore: (String) -> Unit,
    onApplyToActivePage: () -> Unit
) {
    BackHandler(onBack = onBack)

    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors

    var editingExtension by remember { mutableStateOf<BrowserExtensionEntity?>(null) }
    var showChromeStoreDialog by remember { mutableStateOf(false) }

    val installedExtensions = remember(extensions) { extensions.filterNot { it.isBuiltIn } }
    val enabledCount = installedExtensions.count { it.enabled }
    val downloadingState = chromeStoreInstallState as? ChromeStoreInstallState.Downloading

    Scaffold(
        containerColor = colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.extensions_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("extensions_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Chrome Web Store Integration Card
                item(key = "chrome_web_store_card") {
                    ChromeWebStoreCard(
                        downloadingState = downloadingState,
                        onOpenStore = { onOpenChromeWebStore(ChromeWebStoreDownloader.CHROME_WEB_STORE_URL) },
                        onOpenDirectInstallDialog = { showChromeStoreDialog = true }
                    )
                }

                // Master Switch Card
                item(key = "extensions_master_card") {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (extensionsEnabled) aegisColors.shieldActiveContainer else colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (extensionsEnabled) aegisColors.shieldActive.copy(alpha = 0.4f) else colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = aegisColors.shieldActive.copy(alpha = 0.16f),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Extension,
                                            contentDescription = null,
                                            tint = aegisColors.shieldActive,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.extensions_master_title),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.onSurface
                                    )
                                    Text(
                                        text = "有効: ${enabledCount} / 全${installedExtensions.size}個",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = extensionsEnabled,
                                    onCheckedChange = onToggleMasterEnabled,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = colorScheme.onPrimary,
                                        checkedTrackColor = aegisColors.shieldActive
                                    ),
                                    modifier = Modifier.testTag("extensions_master_switch")
                                )
                            }

                            if (hasActiveWebPage && extensionsEnabled && enabledCount > 0) {
                                FilledTonalButton(
                                    onClick = onApplyToActivePage,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("extensions_apply_now_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.extensions_apply_now))
                                }
                            }
                        }
                    }
                }

                // Installed Chrome Web Store Extensions
                if (installedExtensions.isNotEmpty()) {
                    item(key = "header_installed") {
                        Text(
                            text = "追加済みの拡張機能（${installedExtensions.size}）",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    items(installedExtensions, key = { it.id }) { ext ->
                        val cwsId = ext.id.removePrefix("cws_").takeIf { ext.id.startsWith("cws_") && it.length == 32 }
                        ExtensionCard(
                            extension = ext,
                            masterEnabled = extensionsEnabled,
                            isChromeStoreExtension = cwsId != null,
                            onToggle = { onToggleExtension(ext.id, it) },
                            onOpenStorePage = cwsId?.let { id ->
                                { onOpenChromeWebStore(ChromeWebStoreDownloader.buildStoreDetailUrl(id)) }
                            },
                            onEdit = { editingExtension = ext },
                            onDelete = { onDeleteExtension(ext) }
                        )
                    }
                } else {
                    item(key = "extensions_empty_state") {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("extensions_empty_state")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = aegisColors.shieldActiveContainer,
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Extension,
                                            contentDescription = null,
                                            tint = aegisColors.shieldActive,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "追加された拡張機能はまだありません",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "上の「ストアを開く」から Chrome ウェブストアで拡張機能を探すか、「URL / ID から追加」でダウンロードできます。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showChromeStoreDialog) {
        ChromeStoreInstallDialog(
            isDownloading = downloadingState != null,
            onDismiss = { showChromeStoreDialog = false },
            onInstall = { urlOrId ->
                onInstallFromChromeStore(urlOrId)
                showChromeStoreDialog = false
            }
        )
    }

    val toEdit = editingExtension
    if (toEdit != null) {
        EditExtensionDialog(
            existing = toEdit,
            onDismiss = { editingExtension = null },
            onSave = { entity ->
                onSaveExtension(entity)
                editingExtension = null
            }
        )
    }
}

@Composable
private fun ChromeWebStoreCard(
    downloadingState: ChromeStoreInstallState.Downloading?,
    onOpenStore: () -> Unit,
    onOpenDirectInstallDialog: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colorScheme.surface,
        border = BorderStroke(1.2.dp, aegisColors.shieldActive.copy(alpha = 0.45f)),
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chrome_web_store_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            aegisColors.shieldActiveContainer.copy(alpha = 0.65f),
                            colorScheme.surfaceContainerLow
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = aegisColors.shieldActive.copy(alpha = 0.18f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Storefront,
                                contentDescription = null,
                                tint = aegisColors.shieldActive,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.chrome_store_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.chrome_store_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (downloadingState != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colorScheme.surface.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, aegisColors.shieldActive.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.2.dp,
                                color = aegisColors.shieldActive
                            )
                            val progressSuffix = downloadingState.progressLabel
                                .takeIf { it.isNotBlank() }
                                ?.let { " ($it)" }
                                .orEmpty()
                            Text(
                                text = "「${downloadingState.displayName}」をダウンロード・解析中…$progressSuffix",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenStore,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = aegisColors.shieldActive,
                            contentColor = colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1.15f)
                            .testTag("open_chrome_web_store_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ストアを開く",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenDirectInstallDialog,
                        enabled = downloadingState == null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chrome_store_url_install_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Link,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.chrome_store_url_install_button),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChromeStoreInstallDialog(
    isDownloading: Boolean,
    onDismiss: () -> Unit,
    onInstall: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }
    val detectedId = remember(input) { ChromeWebStoreDownloader.extractExtensionId(input) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Storefront,
                contentDescription = null,
                tint = AegisTheme.colors.shieldActive
            )
        },
        title = {
            Text(stringResource(R.string.chrome_store_dialog_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Chrome ウェブストアの拡張機能ページURL、または32文字の拡張機能IDを貼り付けると、CRXパッケージを自動ダウンロードして追加します。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text(stringResource(R.string.chrome_store_dialog_hint)) },
                    placeholder = { Text("https://chromewebstore.google.com/detail/...") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chrome_store_url_input")
                )
                if (input.isNotBlank()) {
                    Text(
                        text = if (detectedId != null) {
                            "検出ID: $detectedId"
                        } else {
                            "Chrome ウェブストアのURLまたは32文字のID（a〜p）を入力してください"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = if (detectedId != null) JetBrainsMonoFontFamily else MaterialTheme.typography.labelSmall.fontFamily
                        ),
                        color = if (detectedId != null) {
                            AegisTheme.colors.shieldActive
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
                Text(
                    text = "※ Webページへ直接適用する content_scripts（JS / CSS）を含む Chrome 拡張機能に対応しています。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onInstall(input) },
                enabled = detectedId != null && !isDownloading,
                modifier = Modifier.testTag("chrome_store_dialog_confirm")
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.chrome_store_dialog_download))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

@Composable
private fun ExtensionCard(
    extension: BrowserExtensionEntity,
    masterEnabled: Boolean,
    isChromeStoreExtension: Boolean,
    onToggle: (Boolean) -> Unit,
    onOpenStorePage: (() -> Unit)?,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors
    val isActive = masterEnabled && extension.enabled
    val scopeLabel = if (extension.matchPatterns.trim() == "*" || extension.matchPatterns.contains("<all_urls>")) {
        "すべてのサイト"
    } else {
        extension.matchPatterns
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colorScheme.surface,
        border = BorderStroke(
            width = if (isActive) 1.2.dp else 1.dp,
            color = if (isActive) aegisColors.shieldActive.copy(alpha = 0.55f) else colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("extension_card_${extension.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isActive) aegisColors.shieldActiveContainer else colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = extensionIconFor(extension.iconKey),
                            contentDescription = null,
                            tint = if (isActive) aegisColors.shieldActive else colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = extension.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = "v${extension.version}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = extension.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = extension.enabled,
                    onCheckedChange = onToggle,
                    enabled = masterEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colorScheme.onPrimary,
                        checkedTrackColor = aegisColors.shieldActive
                    ),
                    modifier = Modifier.testTag("extension_switch_${extension.id}")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isChromeStoreExtension) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = aegisColors.shieldActiveContainer
                        ) {
                            Text(
                                text = "Chrome ストア",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = aegisColors.shieldActive,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colorScheme.surfaceContainerLow
                    ) {
                        Text(
                            text = "対象: $scopeLabel",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                if (onOpenStorePage != null || onEdit != null || onDelete != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (onOpenStorePage != null) {
                            IconButton(
                                onClick = onOpenStorePage,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("extension_store_link_${extension.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Chrome ウェブストアで開く",
                                    tint = aegisColors.shieldActive,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (onEdit != null) {
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("extension_edit_${extension.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "編集",
                                    tint = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (onDelete != null) {
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("extension_delete_${extension.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "削除",
                                    tint = colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditExtensionDialog(
    existing: BrowserExtensionEntity,
    onDismiss: () -> Unit,
    onSave: (BrowserExtensionEntity) -> Unit
) {
    var name by remember { mutableStateOf(existing.name) }
    var description by remember { mutableStateOf(existing.description) }
    var matchPatterns by remember { mutableStateOf(existing.matchPatterns) }
    val isExternalJs = ExtensionEngine.isExternalCodeRef(existing.jsCode)
    val isExternalCss = ExtensionEngine.isExternalCodeRef(existing.cssCode)
    var jsCode by remember { mutableStateOf(if (isExternalJs) "" else existing.jsCode) }
    var cssCode by remember { mutableStateOf(if (isExternalCss) "" else existing.cssCode) }

    val hasJs = isExternalJs || jsCode.isNotBlank()
    val hasCss = isExternalCss || cssCode.isNotBlank()
    val canSave = name.isNotBlank() && (hasJs || hasCss)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("拡張機能を編集")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("拡張機能名") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ext_dialog_name_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("説明（任意）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = matchPatterns,
                    onValueChange = { matchPatterns = it },
                    label = { Text("対象サイト（例: * または youtube.com）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isExternalJs) {
                    Text(
                        text = "※ 大容量の JavaScript スクリプトは端末内ファイルとして保存されています（そのまま保持されます）。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    OutlinedTextField(
                        value = jsCode,
                        onValueChange = { jsCode = it },
                        label = { Text("JavaScript コード") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ext_dialog_js_input")
                    )
                }
                if (isExternalCss) {
                    Text(
                        text = "※ 大容量の CSS スタイルは端末内ファイルとして保存されています（そのまま保持されます）。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    OutlinedTextField(
                        value = cssCode,
                        onValueChange = { cssCode = it },
                        label = { Text("CSS スタイル（任意）") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val entity = existing.copy(
                        name = name.trim(),
                        description = description.trim().ifBlank { "Chrome 拡張機能" },
                        matchPatterns = matchPatterns.trim().ifBlank { "*" },
                        jsCode = if (isExternalJs) existing.jsCode else jsCode.trim(),
                        cssCode = if (isExternalCss) existing.cssCode else cssCode.trim()
                    )
                    onSave(entity)
                },
                enabled = canSave,
                modifier = Modifier.testTag("ext_dialog_save_button")
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

private fun extensionIconFor(iconKey: String): ImageVector = when (iconKey) {
    "chrome_store" -> Icons.Filled.Storefront
    "code" -> Icons.Filled.Code
    else -> Icons.Filled.Extension
}
