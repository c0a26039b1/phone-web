package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import com.example.R
import com.example.data.local.BrowserMenuItem
import com.example.data.local.BrowserPreferences
import com.example.data.local.MenuLayoutStyle
import com.example.data.local.OmniboxShortcut
import com.example.data.local.SearchEngine
import com.example.data.local.ShieldDisplayMode
import com.example.data.local.ThemeMode
import com.example.ui.components.icon
import com.example.ui.theme.AegisTheme

private enum class SettingsSection(val title: String) {
    SEARCH_ENGINE("デフォルト検索エンジン"),
    THEME("外観テーマ"),
    HOME_BACKGROUND("ホーム画面の背景"),
    PAGE_ZOOM("画面ズーム"),
    MENU_LAYOUT("メニューレイアウト・項目"),
    SHIELD_DISPLAY("「広告ブロック稼働中」の表示"),
    OMNIBOX_SHORTCUTS("右上ショートカット"),
    PRIVACY("プライバシーとセキュリティ"),
    DATA_MANAGEMENT("データ管理")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: BrowserPreferences,
    sitePermissionCount: Int,
    enabledExtensionCount: Int = 0,
    totalExtensionCount: Int = 0,
    openMenuCustomizeInitially: Boolean = false,
    onConsumeOpenMenuCustomize: () -> Unit = {},
    onBack: () -> Unit,
    onSelectSearchEngine: (SearchEngine) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    isImportingHomeBackground: Boolean = false,
    homeBackgroundError: String? = null,
    onPickHomeBackground: () -> Unit,
    onPickHomeBackgroundFile: () -> Unit,
    onChangeHomeBackgroundDim: (Float) -> Unit,
    onResetHomeBackground: () -> Unit,
    onSelectDefaultZoomPercent: (Int) -> Unit,
    onToggleForcePinchZoom: (Boolean) -> Unit,
    onSelectMenuLayoutStyle: (MenuLayoutStyle) -> Unit,
    onToggleMenuItem: (BrowserMenuItem) -> Unit,
    onMoveMenuItem: (BrowserMenuItem, Boolean) -> Unit,
    onResetMenuCustomization: () -> Unit,
    onSelectShieldDisplayMode: (ShieldDisplayMode) -> Unit,
    onToggleOmniboxShortcut: (OmniboxShortcut) -> Unit,
    onOpenExtensions: () -> Unit,
    onOpenSitePermissions: () -> Unit,
    onToggleJavaScript: (Boolean) -> Unit,
    onToggleHttpsUpgrade: (Boolean) -> Unit,
    onClearBrowsingData: () -> Unit,
    onResetStats: () -> Unit
) {
    var activeSection by remember {
        mutableStateOf(if (openMenuCustomizeInitially) SettingsSection.MENU_LAYOUT else null)
    }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showResetStatsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(openMenuCustomizeInitially) {
        if (openMenuCustomizeInitially) {
            activeSection = SettingsSection.MENU_LAYOUT
            onConsumeOpenMenuCustomize()
        }
    }

    val handleBack: () -> Unit = {
        if (activeSection != null) {
            activeSection = null
        } else {
            onBack()
        }
    }

    BackHandler(onBack = handleBack)

    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = activeSection?.title ?: stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = handleBack,
                        modifier = Modifier.testTag("settings_back_button")
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
            AnimatedContent(
                targetState = activeSection,
                transitionSpec = {
                    if (targetState != null) {
                        (slideInHorizontally { it / 4 } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it / 6 } + fadeOut())
                    } else {
                        (slideInHorizontally { -it / 4 } + fadeIn()) togetherWith
                            (slideOutHorizontally { it / 6 } + fadeOut())
                    }
                },
                label = "settings_two_level_transition"
            ) { section ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 680.dp)
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (section) {
                        null -> {
                            item(key = "root_general_header") {
                                Text(
                                    text = "一般・表示",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                            item(key = "root_general_card") {
                                val shortcutSummary = buildString {
                                    val preview = preferences.omniboxShortcuts.take(2).joinToString("・") { it.shortLabel }
                                    append(preview)
                                    if (preferences.omniboxShortcuts.size > 2) {
                                        append(" ほか${preferences.omniboxShortcuts.size - 2}件")
                                    }
                                    append("（${preferences.omniboxShortcuts.size}個）")
                                }

                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = colorScheme.surface,
                                    border = BorderStroke(1.dp, colorScheme.outlineVariant)
                                ) {
                                    Column {
                                        SettingsCategoryRow(
                                            icon = Icons.Filled.Search,
                                            title = stringResource(R.string.settings_search_engine),
                                            summary = preferences.searchEngine.displayName,
                                            onClick = { activeSection = SettingsSection.SEARCH_ENGINE },
                                            testTag = "settings_category_search_engine"
                                        )
                                        HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)
                                        SettingsCategoryRow(
                                            icon = Icons.Filled.Palette,
                                            title = stringResource(R.string.settings_theme),
                                            summary = preferences.themeMode.displayName,
                                            onClick = { activeSection = SettingsSection.THEME },
                                            testTag = "settings_category_theme"
                                        )
                                        HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)
                                        SettingsCategoryRow(
                                            icon = Icons.Filled.Wallpaper,
                                            title = stringResource(R.string.settings_home_background),
                                            summary = if (preferences.homeBackgroundPath.isNotBlank()) {
                                                "カスタム画像を設定中（暗さ ${(preferences.homeBackgroundDimAlpha * 100).toInt()}%）"
                                            } else {
                                                "デフォルト（テーマ背景）"
                                            },
                                            onClick = { activeSection = SettingsSection.HOME_BACKGROUND },
                                            testTag = "settings_category_home_background"
                                        )
                                        HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)
                                        SettingsCategoryRow(
                                            icon = Icons.Filled.ZoomIn,
                                            title = stringResource(R.string.settings_page_zoom),
                                            summary = "既定 ${preferences.defaultZoomPercent}%・ピンチズーム${if (preferences.forcePinchZoomEnabled) "常に許可" else "サイト設定"}",
                                            onClick = { activeSection = SettingsSection.PAGE_ZOOM },
                                            testTag = "settings_category_page_zoom"
                                        )
                                        HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)
                                        SettingsCategoryRow(
                                            icon = Icons.Filled.ViewList,
                                            title = stringResource(R.string.menu_layout_title),
                                            summary = "${preferences.menuLayoutStyle.shortLabel}・${preferences.menuItems.size}項目を表示",
                                            onClick = { activeSection = SettingsSection.MENU_LAYOUT },
                                            testTag = "settings_category_menu_layout"
                                        )
                                        HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)
                                        SettingsCategoryRow(
                                            icon = Icons.Filled.Shield,
                                            title = "「広告ブロック稼働中」の表示",
                                            summary = preferences.shieldDisplayMode.displayName,
                                            onClick = { activeSection = SettingsSection.SHIELD_DISPLAY },
                                            testTag = "settings_category_shield_display"
                                        )
                                        HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)
                                        SettingsCategoryRow(
                                            icon = Icons.Filled.Bolt,
                                            title = "右上ショートカット",
                                            summary = shortcutSummary,
                                            onClick = { activeSection = SettingsSection.OMNIBOX_SHORTCUTS },
                                            testTag = "settings_category_shortcuts"
                                        )
                                        HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)
                                        SettingsCategoryRow(
                                            icon = Icons.Filled.Extension,
                                            title = stringResource(R.string.extensions_title),
                                            summary = if (preferences.extensionsEnabled) {
                                                "有効 ${enabledExtensionCount} / ${totalExtensionCount}個（.zip / .user.js 対応）"
                                            } else {
                                                "拡張機能エンジン停止中"
                                            },
                                            onClick = onOpenExtensions,
                                            testTag = "settings_category_extensions"
                                        )
                                    }
                                }
                            }

                            item(key = "root_privacy_header") {
                                Text(
                                    text = "プライバシー・データ",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                            item(key = "root_privacy_card") {
                                val httpsLabel = if (preferences.httpsUpgradeEnabled) "HTTPS化 ON" else "HTTPS化 OFF"
                                val jsLabel = if (preferences.javascriptEnabled) "JS ON" else "JS OFF"

                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = colorScheme.surface,
                                    border = BorderStroke(1.dp, colorScheme.outlineVariant)
                                ) {
                                    Column {
                                        SettingsCategoryRow(
                                            icon = Icons.Filled.Security,
                                            title = stringResource(R.string.site_perm_title),
                                            summary = if (sitePermissionCount > 0) {
                                                "カメラ・マイク・位置情報（${sitePermissionCount}サイト設定済み）"
                                            } else {
                                                "カメラ・マイク・位置情報をサイトごとに許可・拒否"
                                            },
                                            onClick = onOpenSitePermissions,
                                            testTag = "settings_category_site_permissions"
                                        )
                                        HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)
                                        SettingsCategoryRow(
                                            icon = Icons.Filled.Lock,
                                            title = stringResource(R.string.settings_privacy_section),
                                            summary = "$httpsLabel・$jsLabel",
                                            onClick = { activeSection = SettingsSection.PRIVACY },
                                            testTag = "settings_category_privacy"
                                        )
                                        HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)
                                        SettingsCategoryRow(
                                            icon = Icons.Filled.Storage,
                                            title = "データ管理",
                                            summary = "キャッシュ・履歴の削除、ブロック統計リセット",
                                            onClick = { activeSection = SettingsSection.DATA_MANAGEMENT },
                                            testTag = "settings_category_data"
                                        )
                                    }
                                }
                            }
                        }

                        SettingsSection.SEARCH_ENGINE -> {
                            item(key = "detail_search_engine") {
                                SearchEngineDetailSection(
                                    selectedEngine = preferences.searchEngine,
                                    onSelectSearchEngine = onSelectSearchEngine
                                )
                            }
                        }

                        SettingsSection.THEME -> {
                            item(key = "detail_theme") {
                                ThemeDetailSection(
                                    selectedMode = preferences.themeMode,
                                    onSelectThemeMode = onSelectThemeMode
                                )
                            }
                        }

                        SettingsSection.HOME_BACKGROUND -> {
                            item(key = "detail_home_background") {
                                HomeBackgroundDetailSection(
                                    backgroundPath = preferences.homeBackgroundPath,
                                    dimAlpha = preferences.homeBackgroundDimAlpha,
                                    isImporting = isImportingHomeBackground,
                                    errorMessage = homeBackgroundError,
                                    onPickImage = onPickHomeBackground,
                                    onPickFromFile = onPickHomeBackgroundFile,
                                    onDimAlphaChange = onChangeHomeBackgroundDim,
                                    onResetBackground = onResetHomeBackground
                                )
                            }
                        }

                        SettingsSection.PAGE_ZOOM -> {
                            item(key = "detail_page_zoom") {
                                PageZoomDetailSection(
                                    defaultZoomPercent = preferences.defaultZoomPercent,
                                    forcePinchZoomEnabled = preferences.forcePinchZoomEnabled,
                                    onSelectDefaultZoom = onSelectDefaultZoomPercent,
                                    onToggleForcePinchZoom = onToggleForcePinchZoom
                                )
                            }
                        }

                        SettingsSection.MENU_LAYOUT -> {
                            item(key = "detail_menu_layout") {
                                MenuCustomizationDetailSection(
                                    selectedLayout = preferences.menuLayoutStyle,
                                    enabledItems = preferences.menuItems,
                                    onSelectLayout = onSelectMenuLayoutStyle,
                                    onToggleItem = onToggleMenuItem,
                                    onMoveItem = onMoveMenuItem,
                                    onResetDefaults = onResetMenuCustomization
                                )
                            }
                        }

                        SettingsSection.SHIELD_DISPLAY -> {
                            item(key = "detail_shield_display") {
                                ShieldDisplayDetailSection(
                                    selectedMode = preferences.shieldDisplayMode,
                                    onSelectShieldDisplayMode = onSelectShieldDisplayMode
                                )
                            }
                        }

                        SettingsSection.OMNIBOX_SHORTCUTS -> {
                            item(key = "detail_shortcuts") {
                                ShortcutsDetailSection(
                                    selectedShortcuts = preferences.omniboxShortcuts,
                                    onToggleOmniboxShortcut = onToggleOmniboxShortcut
                                )
                            }
                        }

                        SettingsSection.PRIVACY -> {
                            item(key = "detail_privacy") {
                                PrivacyDetailSection(
                                    preferences = preferences,
                                    onToggleHttpsUpgrade = onToggleHttpsUpgrade,
                                    onToggleJavaScript = onToggleJavaScript
                                )
                            }
                        }

                        SettingsSection.DATA_MANAGEMENT -> {
                            item(key = "detail_data_management") {
                                DataManagementDetailSection(
                                    onRequestClearData = { showClearDataDialog = true },
                                    onRequestResetStats = { showResetStatsDialog = true }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text(stringResource(R.string.settings_clear_data)) },
            text = { Text("すべてのCookie・キャッシュ・Webストレージ・閲覧履歴を完全に削除します。よろしいですか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDataDialog = false
                        onClearBrowsingData()
                    },
                    modifier = Modifier.testTag("confirm_clear_data_button")
                ) {
                    Text("削除", color = colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    if (showResetStatsDialog) {
        AlertDialog(
            onDismissRequest = { showResetStatsDialog = false },
            title = { Text(stringResource(R.string.settings_reset_stats)) },
            text = { Text("広告ブロックの累計カウントとドメイン統計をリセットします。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetStatsDialog = false
                        onResetStats()
                    },
                    modifier = Modifier.testTag("confirm_reset_stats_button")
                ) {
                    Text("リセット", color = colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetStatsDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

@Composable
private fun SettingsCategoryRow(
    icon: ImageVector,
    title: String,
    summary: String,
    onClick: () -> Unit,
    testTag: String
) {
    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = aegisColors.shieldActiveContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = aegisColors.shieldActive,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun SearchEngineDetailSection(
    selectedEngine: SearchEngine,
    onSelectSearchEngine: (SearchEngine) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "アドレスバーからキーワード検索したときに使用する検索エンジンを選択します。",
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant
        )
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colorScheme.surface,
            border = BorderStroke(1.dp, colorScheme.outlineVariant)
        ) {
            Column {
                SearchEngine.entries.forEachIndexed { idx, engine ->
                    if (idx > 0) {
                        HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clickable { onSelectSearchEngine(engine) }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("search_engine_${engine.name}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedEngine == engine,
                            onClick = { onSelectSearchEngine(engine) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = aegisColors.shieldActive
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = engine.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = engine.domain,
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeDetailSection(
    selectedMode: ThemeMode,
    onSelectThemeMode: (ThemeMode) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "アプリの配色テーマを選択します。",
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant
        )
        ThemeMode.entries.forEach { mode ->
            val selected = selectedMode == mode
            val (icon, subtitle) = when (mode) {
                ThemeMode.SYSTEM -> Icons.Filled.BrightnessAuto to "端末のダークテーマ設定に合わせて自動切り替え"
                ThemeMode.DARK -> Icons.Filled.DarkMode to "暗い画面で目の負担を抑えるダーク配色"
                ThemeMode.LIGHT -> Icons.Filled.LightMode to "明るく視認性の高いライト配色"
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelectThemeMode(mode) }
                    .testTag("theme_mode_${mode.name}"),
                shape = RoundedCornerShape(16.dp),
                color = if (selected) aegisColors.shieldActiveContainer else colorScheme.surface,
                border = BorderStroke(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) aegisColors.shieldActive else colorScheme.outlineVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) {
                            aegisColors.shieldActive.copy(alpha = 0.16f)
                        } else {
                            colorScheme.surfaceContainerHigh
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (selected) aegisColors.shieldActive else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mode.displayName,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                            ),
                            color = if (selected) aegisColors.shieldActive else colorScheme.onSurface
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    if (selected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = aegisColors.shieldActive,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShieldDisplayDetailSection(
    selectedMode: ShieldDisplayMode,
    onSelectShieldDisplayMode: (ShieldDisplayMode) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "検索時や閲覧中にアドレスバーのシールド表示を隠すか選択できます（非表示でも広告ブロックは動作します）。",
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant
        )
        ShieldDisplayMode.entries.forEach { mode ->
            val selected = selectedMode == mode
            val description = when (mode) {
                ShieldDisplayMode.ALWAYS_SHOW -> "すべてのページでアドレスバー左上にシールドとブロック件数を表示"
                ShieldDisplayMode.HIDE_ON_SEARCH -> "検索結果ページではシールド表示を隠し、通常サイトでのみ表示"
                ShieldDisplayMode.HIDE_WHILE_BROWSING -> "Webページ閲覧中は常にシールド表示を隠す（ホーム画面のみ表示）"
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelectShieldDisplayMode(mode) }
                    .testTag("settings_shield_display_${mode.name}"),
                shape = RoundedCornerShape(16.dp),
                color = if (selected) aegisColors.shieldActiveContainer else colorScheme.surface,
                border = BorderStroke(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) aegisColors.shieldActive else colorScheme.outlineVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = { onSelectShieldDisplayMode(mode) },
                        colors = RadioButtonDefaults.colors(selectedColor = aegisColors.shieldActive)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mode.displayName,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                            ),
                            color = if (selected) aegisColors.shieldActive else colorScheme.onSurface
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutsDetailSection(
    selectedShortcuts: List<OmniboxShortcut>,
    onToggleOmniboxShortcut: (OmniboxShortcut) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "複数選ぶと、右上のボタンを1回押したときに一覧表示して好きな機能を選べます。",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = aegisColors.shieldActiveContainer
            ) {
                Text(
                    text = "${selectedShortcuts.size}個選択中",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = aegisColors.shieldActive,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colorScheme.surface,
            border = BorderStroke(1.dp, colorScheme.outlineVariant)
        ) {
            Column {
                OmniboxShortcut.entries.forEachIndexed { idx, shortcut ->
                    if (idx > 0) {
                        HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)
                    }
                    val selected = shortcut in selectedShortcuts
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clickable { onToggleOmniboxShortcut(shortcut) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("settings_shortcut_${shortcut.name}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selected,
                            onCheckedChange = { onToggleOmniboxShortcut(shortcut) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = aegisColors.shieldActive,
                                checkmarkColor = colorScheme.onPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = shortcut.icon(),
                            contentDescription = null,
                            tint = if (selected) aegisColors.shieldActive else colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = shortcut.displayName,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = if (selected) aegisColors.shieldActive else colorScheme.onSurface
                            )
                            Text(
                                text = shortcut.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyDetailSection(
    preferences: BrowserPreferences,
    onToggleHttpsUpgrade: (Boolean) -> Unit,
    onToggleJavaScript: (Boolean) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colorScheme.surface,
        border = BorderStroke(1.dp, colorScheme.outlineVariant)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_https_upgrade),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "HTTP接続を自動的に安全なHTTPSへ切り替えます",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = preferences.httpsUpgradeEnabled,
                    onCheckedChange = onToggleHttpsUpgrade,
                    modifier = Modifier.testTag("settings_https_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colorScheme.onPrimary,
                        checkedTrackColor = aegisColors.shieldActive
                    )
                )
            }
            HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_javascript),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "オフにするとスクリプト系広告を完全に遮断できます",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = preferences.javascriptEnabled,
                    onCheckedChange = onToggleJavaScript,
                    modifier = Modifier.testTag("settings_js_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colorScheme.onPrimary,
                        checkedTrackColor = aegisColors.shieldActive
                    )
                )
            }
        }
    }
}

@Composable
private fun DataManagementDetailSection(
    onRequestClearData: () -> Unit,
    onRequestResetStats: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colorScheme.surface,
        border = BorderStroke(1.dp, colorScheme.outlineVariant)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .clickable(onClick = onRequestClearData)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .testTag("settings_clear_data_row"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.CleaningServices,
                    contentDescription = null,
                    tint = colorScheme.error,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_clear_data),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.settings_clear_data_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .clickable(onClick = onRequestResetStats)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .testTag("settings_reset_stats_row"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.RestartAlt,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_reset_stats),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "累計ブロック件数とドメイン統計を0に初期化します",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
