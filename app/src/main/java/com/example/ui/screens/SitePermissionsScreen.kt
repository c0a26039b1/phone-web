package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.adblock.AdBlockEngine
import com.example.data.local.SitePermissionEntity
import com.example.data.local.SitePermissionState
import com.example.data.local.SitePermissionType
import com.example.ui.theme.AegisTheme
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SitePermissionsScreen(
    sitePermissions: List<SitePermissionEntity>,
    currentHost: String,
    onBack: () -> Unit,
    onUpdatePermission: (domain: String, type: SitePermissionType, state: SitePermissionState) -> Unit,
    onAddSitePermission: (
        domain: String,
        camera: SitePermissionState,
        microphone: SitePermissionState,
        location: SitePermissionState
    ) -> Unit,
    onDeleteSitePermission: (domain: String) -> Unit,
    onClearAllPermissions: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<SitePermissionType?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var osPermCheckTrigger by remember { mutableIntStateOf(0) }

    val osPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        osPermCheckTrigger++
    }

    val hasCameraOs = remember(osPermCheckTrigger) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }
    val hasMicOs = remember(osPermCheckTrigger) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }
    val hasLocationOs = remember(osPermCheckTrigger) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    val cleanCurrentHost = remember(currentHost) { AdBlockEngine.sanitizeDomainInput(currentHost) }
    val currentHostAlreadyListed = remember(cleanCurrentHost, sitePermissions) {
        cleanCurrentHost.isNotBlank() && sitePermissions.any { it.domain == cleanCurrentHost }
    }

    val filteredPermissions = remember(sitePermissions, searchQuery, selectedFilter) {
        val q = searchQuery.trim().lowercase()
        sitePermissions.filter { entry ->
            val matchesQuery = q.isEmpty() || entry.domain.lowercase().contains(q)
            val matchesType = selectedFilter == null || entry.stateFor(selectedFilter!!) != SitePermissionState.ASK
            matchesQuery && matchesType
        }
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.site_perm_title),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("site_perm_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("site_perm_add_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.site_perm_add_site)
                        )
                    }
                    if (sitePermissions.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearAllDialog = true },
                            modifier = Modifier.testTag("site_perm_clear_all_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = stringResource(R.string.site_perm_reset_all)
                            )
                        }
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
                item(key = "os_permissions_card") {
                    OsPermissionStatusCard(
                        hasCamera = hasCameraOs,
                        hasMic = hasMicOs,
                        hasLocation = hasLocationOs,
                        onRequestMissing = {
                            val missing = buildList {
                                if (!hasCameraOs) add(Manifest.permission.CAMERA)
                                if (!hasMicOs) add(Manifest.permission.RECORD_AUDIO)
                                if (!hasLocationOs) {
                                    add(Manifest.permission.ACCESS_FINE_LOCATION)
                                    add(Manifest.permission.ACCESS_COARSE_LOCATION)
                                }
                            }
                            if (missing.isNotEmpty()) {
                                osPermLauncher.launch(missing.toTypedArray())
                            }
                        }
                    )
                }

                if (cleanCurrentHost.isNotBlank() && !currentHostAlreadyListed) {
                    item(key = "current_site_quick_card") {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = aegisColors.shieldActiveContainer.copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, aegisColors.shieldActive.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Language,
                                        contentDescription = null,
                                        tint = aegisColors.shieldActive,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "表示中のサイトを今すぐ設定",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = aegisColors.shieldActive
                                    )
                                }
                                Text(
                                    text = cleanCurrentHost,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = colorScheme.onSurface
                                )
                                SitePermissionControlRows(
                                    domain = cleanCurrentHost,
                                    cameraState = SitePermissionState.ASK,
                                    micState = SitePermissionState.ASK,
                                    locationState = SitePermissionState.ASK,
                                    onUpdate = { type, state ->
                                        onUpdatePermission(cleanCurrentHost, type, state)
                                    }
                                )
                            }
                        }
                    }
                }

                item(key = "search_and_filter") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("site_perm_search_input"),
                            placeholder = { Text("ドメインを検索（例: meet.google.com）") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Filled.Search, contentDescription = null)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedFilter == null,
                                onClick = { selectedFilter = null },
                                label = { Text("すべて (${sitePermissions.size})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = aegisColors.shieldActiveContainer,
                                    selectedLabelColor = aegisColors.shieldActive
                                ),
                                modifier = Modifier.testTag("site_perm_filter_all")
                            )
                            SitePermissionType.entries.forEach { type ->
                                val icon = permissionTypeIcon(type)
                                val configuredCount = sitePermissions.count {
                                    it.stateFor(type) != SitePermissionState.ASK
                                }
                                FilterChip(
                                    selected = selectedFilter == type,
                                    onClick = {
                                        selectedFilter = if (selectedFilter == type) null else type
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = { Text("${type.labelJa} ($configuredCount)") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = aegisColors.shieldActiveContainer,
                                        selectedLabelColor = aegisColors.shieldActive,
                                        selectedLeadingIconColor = aegisColors.shieldActive
                                    ),
                                    modifier = Modifier.testTag("site_perm_filter_${type.name}")
                                )
                            }
                        }
                    }
                }

                if (filteredPermissions.isEmpty()) {
                    item(key = "empty_state") {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = colorScheme.surface,
                            border = BorderStroke(1.dp, colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Security,
                                    contentDescription = null,
                                    tint = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = if (sitePermissions.isEmpty()) {
                                        stringResource(R.string.site_perm_empty_title)
                                    } else {
                                        "条件に一致するサイトがありません"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.site_perm_empty_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = { showAddDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = aegisColors.shieldActive,
                                        contentColor = colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.testTag("site_perm_empty_add_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.site_perm_add_site))
                                }
                            }
                        }
                    }
                } else {
                    items(filteredPermissions, key = { it.domain }) { entity ->
                        SitePermissionCard(
                            entity = entity,
                            onUpdate = { type, state ->
                                onUpdatePermission(entity.domain, type, state)
                            },
                            onDelete = { onDeleteSitePermission(entity.domain) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSitePermissionDialog(
            initialDomain = if (!currentHostAlreadyListed) cleanCurrentHost else "",
            onDismiss = { showAddDialog = false },
            onConfirm = { domain, cam, mic, loc ->
                onAddSitePermission(domain, cam, mic, loc)
                showAddDialog = false
            }
        )
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text(stringResource(R.string.site_perm_reset_all)) },
            text = { Text("すべてのウェブサイトに対するカメラ・マイク・位置情報の個別設定を削除し、「毎回確認」に戻します。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearAllDialog = false
                        onClearAllPermissions()
                    },
                    modifier = Modifier.testTag("site_perm_confirm_clear_all")
                ) {
                    Text("リセット", color = colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

@Composable
private fun OsPermissionStatusCard(
    hasCamera: Boolean,
    hasMic: Boolean,
    hasLocation: Boolean,
    onRequestMissing: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors
    val allGranted = hasCamera && hasMic && hasLocation

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colorScheme.surface,
        border = BorderStroke(1.dp, colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (allGranted) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint = if (allGranted) aegisColors.shieldActive else aegisColors.shieldPaused,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Android端末側のアプリ権限",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurface
                )
            }
            Text(
                text = "ウェブサイトに「許可」を設定しても、端末側でアプリ権限がオフの場合は利用できません。",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OsPermissionBadge("カメラ", hasCamera, Modifier.weight(1f))
                OsPermissionBadge("マイク", hasMic, Modifier.weight(1f))
                OsPermissionBadge("位置情報", hasLocation, Modifier.weight(1f))
            }
            if (!allGranted) {
                OutlinedButton(
                    onClick = onRequestMissing,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, aegisColors.shieldActive),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("site_perm_request_os_button")
                ) {
                    Text(
                        text = "未許可の端末権限をリクエスト",
                        color = aegisColors.shieldActive,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
private fun OsPermissionBadge(
    label: String,
    granted: Boolean,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (granted) aegisColors.shieldActiveContainer else colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = 1.dp,
            color = if (granted) aegisColors.shieldActive.copy(alpha = 0.35f) else colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant
            )
            Text(
                text = if (granted) "許可済み" else "未許可",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (granted) aegisColors.shieldActive else aegisColors.shieldPaused
            )
        }
    }
}

@Composable
private fun SitePermissionCard(
    entity: SitePermissionEntity,
    onUpdate: (SitePermissionType, SitePermissionState) -> Unit,
    onDelete: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colorScheme.surface,
        border = BorderStroke(1.dp, colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Language,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = entity.domain,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("site_perm_delete_${entity.domain}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "設定を削除",
                        tint = colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)
            SitePermissionControlRows(
                domain = entity.domain,
                cameraState = entity.cameraState,
                micState = entity.microphoneState,
                locationState = entity.locationState,
                onUpdate = onUpdate
            )
        }
    }
}

@Composable
private fun SitePermissionControlRows(
    domain: String,
    cameraState: SitePermissionState,
    micState: SitePermissionState,
    locationState: SitePermissionState,
    onUpdate: (SitePermissionType, SitePermissionState) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PermissionTriStateRow(
            domain = domain,
            type = SitePermissionType.CAMERA,
            selectedState = cameraState,
            onSelect = { onUpdate(SitePermissionType.CAMERA, it) }
        )
        PermissionTriStateRow(
            domain = domain,
            type = SitePermissionType.MICROPHONE,
            selectedState = micState,
            onSelect = { onUpdate(SitePermissionType.MICROPHONE, it) }
        )
        PermissionTriStateRow(
            domain = domain,
            type = SitePermissionType.LOCATION,
            selectedState = locationState,
            onSelect = { onUpdate(SitePermissionType.LOCATION, it) }
        )
    }
}

@Composable
private fun PermissionTriStateRow(
    domain: String,
    type: SitePermissionType,
    selectedState: SitePermissionState,
    onSelect: (SitePermissionState) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = permissionTypeIcon(type),
                contentDescription = null,
                tint = when (selectedState) {
                    SitePermissionState.ALLOW -> aegisColors.shieldActive
                    SitePermissionState.DENY -> colorScheme.error
                    SitePermissionState.ASK -> colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = type.labelJa,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                color = colorScheme.onSurface
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SitePermissionState.entries.forEach { state ->
                val isSelected = selectedState == state
                val activeBg = when (state) {
                    SitePermissionState.ALLOW -> aegisColors.shieldActiveContainer
                    SitePermissionState.ASK -> colorScheme.secondaryContainer
                    SitePermissionState.DENY -> colorScheme.errorContainer
                }
                val activeFg = when (state) {
                    SitePermissionState.ALLOW -> aegisColors.shieldActive
                    SitePermissionState.ASK -> colorScheme.onSecondaryContainer
                    SitePermissionState.DENY -> colorScheme.error
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelect(state) }
                        .testTag("site_perm_${domain}_${type.name}_${state.name}"),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) activeBg else colorScheme.surfaceContainerLow,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) activeFg else colorScheme.outlineVariant
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = state.labelJa,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) activeFg else colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddSitePermissionDialog(
    initialDomain: String,
    onDismiss: () -> Unit,
    onConfirm: (
        domain: String,
        camera: SitePermissionState,
        microphone: SitePermissionState,
        location: SitePermissionState
    ) -> Unit
) {
    var domainInput by remember { mutableStateOf(initialDomain) }
    var camState by remember { mutableStateOf(SitePermissionState.ASK) }
    var micState by remember { mutableStateOf(SitePermissionState.ASK) }
    var locState by remember { mutableStateOf(SitePermissionState.ASK) }

    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colorScheme.surface,
        title = { Text(stringResource(R.string.site_perm_add_site)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = domainInput,
                    onValueChange = { domainInput = it },
                    label = { Text("ドメイン名（例: meet.google.com）") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_site_perm_domain_input")
                )
                SitePermissionControlRows(
                    domain = "new_site",
                    cameraState = camState,
                    micState = micState,
                    locationState = locState,
                    onUpdate = { type, state ->
                        when (type) {
                            SitePermissionType.CAMERA -> camState = state
                            SitePermissionType.MICROPHONE -> micState = state
                            SitePermissionType.LOCATION -> locState = state
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(domainInput, camState, micState, locState) },
                enabled = AdBlockEngine.sanitizeDomainInput(domainInput).isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = aegisColors.shieldActive,
                    contentColor = colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("add_site_perm_confirm_button")
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

private fun permissionTypeIcon(type: SitePermissionType): ImageVector = when (type) {
    SitePermissionType.CAMERA -> Icons.Filled.Videocam
    SitePermissionType.MICROPHONE -> Icons.Filled.Mic
    SitePermissionType.LOCATION -> Icons.Filled.LocationOn
}
