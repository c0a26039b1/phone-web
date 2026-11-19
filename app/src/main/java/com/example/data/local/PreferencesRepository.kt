package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.browserDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "aegis_browser_prefs"
)

enum class SearchEngine(
    val displayName: String,
    val queryUrlTemplate: String,
    val domain: String
) {
    DUCKDUCKGO(
        displayName = "DuckDuckGo（プライバシー重視）",
        queryUrlTemplate = "https://duckduckgo.com/?q=%s",
        domain = "duckduckgo.com"
    ),
    GOOGLE(
        displayName = "Google",
        queryUrlTemplate = "https://www.google.com/search?q=%s",
        domain = "google.com"
    ),
    YAHOO_JAPAN(
        displayName = "Yahoo! JAPAN",
        queryUrlTemplate = "https://search.yahoo.co.jp/search?p=%s",
        domain = "yahoo.co.jp"
    ),
    STARTPAGE(
        displayName = "Startpage",
        queryUrlTemplate = "https://www.startpage.com/sp/search?query=%s",
        domain = "startpage.com"
    );

    companion object {
        fun fromName(name: String?): SearchEngine =
            entries.firstOrNull { it.name == name } ?: DUCKDUCKGO
    }
}

enum class ThemeMode(val displayName: String) {
    SYSTEM("システム設定に従う"),
    DARK("ダークモード"),
    LIGHT("ライトモード");

    companion object {
        fun fromName(name: String?): ThemeMode =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

enum class ShieldDisplayMode(val displayName: String, val shortLabel: String) {
    ALWAYS_SHOW("常に表示", "常に表示"),
    HIDE_ON_SEARCH("検索時は非表示", "検索時OFF"),
    HIDE_WHILE_BROWSING("閲覧中は非表示", "閲覧中OFF");

    companion object {
        fun fromName(name: String?): ShieldDisplayMode =
            entries.firstOrNull { it.name == name } ?: ALWAYS_SHOW
    }
}

enum class OmniboxShortcut(
    val displayName: String,
    val shortLabel: String,
    val description: String
) {
    AI_SOLVER(
        displayName = "AIでページの問題を回答",
        shortLabel = "AI回答",
        description = "ページ全体を分析して問題の回答をポップアップ表示"
    ),
    EXTENSIONS(
        displayName = "拡張機能",
        shortLabel = "拡張機能",
        description = "インストール済みの拡張機能・ユーザースクリプトを管理"
    ),
    FULL_SCREENSHOT(
        displayName = "ページ全体をスクリーンショット",
        shortLabel = "全体スクショ",
        description = "表示中のページ全体を縦長画像として撮影"
    ),
    FULLSCREEN(
        displayName = "画面全体表示（フルスクリーン）",
        shortLabel = "全画面表示",
        description = "上下のバーを隠してWebページを画面いっぱいに表示"
    ),
    PAGE_ZOOM(
        displayName = "画面ズーム",
        shortLabel = "ズーム",
        description = "表示中のWebページを50%〜300%で拡大・縮小"
    ),
    TRANSLATE(
        displayName = "ページを翻訳",
        shortLabel = "翻訳",
        description = "表示中のWebページを日本語や他の言語へ翻訳"
    ),
    BOOKMARK(
        displayName = "ブックマークに追加 / 解除",
        shortLabel = "ブックマーク",
        description = "表示中のページをブックマーク登録・解除"
    ),
    FIND_IN_PAGE(
        displayName = "ページ内検索",
        shortLabel = "ページ内検索",
        description = "表示中のページからキーワードを素早く検索"
    ),
    DESKTOP_MODE(
        displayName = "PC版サイト切替",
        shortLabel = "PC版サイト",
        description = "モバイル表示とPC版表示を切り替え"
    ),
    SHARE(
        displayName = "URLを共有",
        shortLabel = "共有",
        description = "表示中のページURLを他のアプリへ共有"
    );

    companion object {
        val DEFAULT_SELECTION: List<OmniboxShortcut> = listOf(
            FULL_SCREENSHOT,
            FULLSCREEN,
            TRANSLATE
        )

        fun fromName(name: String?): OmniboxShortcut? =
            entries.firstOrNull { it.name == name }

        fun encodeList(shortcuts: List<OmniboxShortcut>): String {
            val clean = entries.filter { it in shortcuts }
            return (clean.ifEmpty { DEFAULT_SELECTION }).joinToString(",") { it.name }
        }

        fun decodeList(raw: String?): List<OmniboxShortcut> {
            if (raw.isNullOrBlank()) return DEFAULT_SELECTION
            val parsed = raw.split(",")
                .mapNotNull { fromName(it.trim()) }
                .toSet()
            val ordered = entries.filter { it in parsed }
            return ordered.ifEmpty { DEFAULT_SELECTION }
        }
    }
}

enum class MenuLayoutStyle(
    val displayName: String,
    val shortLabel: String,
    val description: String
) {
    POPUP_LIST(
        displayName = "リストポップアップ（縦型）",
        shortLabel = "縦型リスト",
        description = "アイコンと項目名が縦に並ぶコンパクトなポップアップメニュー＋下部クイック操作バー"
    ),
    CLASSIC_SHEET(
        displayName = "標準シート（カード型）",
        shortLabel = "カード型シート",
        description = "上部のクイックタイルとシールドバナーを備えたボトムシート表示"
    );

    companion object {
        fun fromName(name: String?): MenuLayoutStyle =
            entries.firstOrNull { it.name == name } ?: POPUP_LIST
    }
}

enum class BrowserMenuItem(
    val displayName: String,
    val description: String,
    val requiresWebPage: Boolean = false
) {
    NEW_TAB(
        displayName = "新しいタブ",
        description = "通常タブを新しく開く"
    ),
    NEW_PRIVATE_TAB(
        displayName = "新しいプライベートタブ",
        description = "履歴を残さないプライベートタブを開く"
    ),
    ADD_TO_GROUP(
        displayName = "新しいグループに追加",
        description = "表示中のタブをタブグループへ整理"
    ),
    HISTORY(
        displayName = "履歴",
        description = "過去に閲覧したページ一覧を表示"
    ),
    BOOKMARKS(
        displayName = "ブックマーク",
        description = "保存したブックマーク一覧を表示"
    ),
    AI_PAGE_SOLVER(
        displayName = "AIページ問題解析",
        description = "ページ全体を分析して問題の回答をポップアップ表示",
        requiresWebPage = true
    ),
    EXTENSIONS(
        displayName = "拡張機能",
        description = "Webページ用の拡張機能・ユーザースクリプトを管理・追加"
    ),
    TRANSLATE_PAGE(
        displayName = "ページを翻訳",
        description = "表示中のページを日本語などへ翻訳",
        requiresWebPage = true
    ),
    FULL_SCREENSHOT(
        displayName = "全体スクリーンショット",
        description = "ページ全体を縦長画像として撮影",
        requiresWebPage = true
    ),
    FULLSCREEN(
        displayName = "画面全体表示",
        description = "上下のバーを隠してフルスクリーン表示"
    ),
    PAGE_ZOOM(
        displayName = "画面ズーム",
        description = "表示中のWebページを拡大・縮小（50%〜300%）",
        requiresWebPage = true
    ),
    FIND_IN_PAGE(
        displayName = "ページ内検索",
        description = "ページ内のキーワードを検索",
        requiresWebPage = true
    ),
    DESKTOP_MODE(
        displayName = "PC版サイト",
        description = "チェックでPC版レイアウトに切り替え",
        requiresWebPage = true
    ),
    TOGGLE_BOOKMARK(
        displayName = "このページをブックマーク",
        description = "表示中のページをブックマーク登録・解除",
        requiresWebPage = true
    ),
    SHIELD_DASHBOARD(
        displayName = "シールド保護とフィルター",
        description = "広告ブロック統計とフィルター設定を開く"
    ),
    SHORTCUT_PICKER(
        displayName = "右上ショートカット設定",
        description = "アドレスバー右上のショートカットを編集"
    ),
    SITE_PERMISSIONS(
        displayName = "サイト別の権限管理",
        description = "カメラ・マイク・位置情報のサイト別設定"
    ),
    SETTINGS(
        displayName = "設定",
        description = "ブラウザ設定画面を開く"
    );

    companion object {
        val DEFAULT_ORDER: List<BrowserMenuItem> = listOf(
            NEW_TAB,
            NEW_PRIVATE_TAB,
            ADD_TO_GROUP,
            HISTORY,
            BOOKMARKS,
            AI_PAGE_SOLVER,
            EXTENSIONS,
            PAGE_ZOOM,
            TRANSLATE_PAGE,
            FULL_SCREENSHOT,
            FULLSCREEN,
            FIND_IN_PAGE,
            DESKTOP_MODE,
            SHIELD_DASHBOARD,
            SHORTCUT_PICKER,
            SETTINGS
        )

        fun fromName(name: String?): BrowserMenuItem? =
            entries.firstOrNull { it.name == name }

        /**
         * Preserves the user's custom display order while removing duplicates/unknowns.
         */
        fun encodeOrderedList(items: List<BrowserMenuItem>): String {
            val unique = items.distinct()
            return (unique.ifEmpty { DEFAULT_ORDER }).joinToString(",") { it.name }
        }

        fun decodeOrderedList(raw: String?): List<BrowserMenuItem> {
            if (raw.isNullOrBlank()) return DEFAULT_ORDER
            val parsed = raw.split(",")
                .mapNotNull { fromName(it.trim()) }
                .distinct()
            return parsed.ifEmpty { DEFAULT_ORDER }
        }
    }
}

data class BrowserPreferences(
    val adBlockEnabled: Boolean = true,
    val blockAds: Boolean = true,
    val blockTrackers: Boolean = true,
    val blockSocial: Boolean = true,
    val blockPopups: Boolean = true,
    val cosmeticFiltering: Boolean = true,
    val javascriptEnabled: Boolean = true,
    val httpsUpgradeEnabled: Boolean = true,
    val searchEngine: SearchEngine = SearchEngine.DUCKDUCKGO,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val shieldDisplayMode: ShieldDisplayMode = ShieldDisplayMode.ALWAYS_SHOW,
    val omniboxShortcuts: List<OmniboxShortcut> = OmniboxShortcut.DEFAULT_SELECTION,
    val menuLayoutStyle: MenuLayoutStyle = MenuLayoutStyle.POPUP_LIST,
    val menuItems: List<BrowserMenuItem> = BrowserMenuItem.DEFAULT_ORDER,
    val extensionsEnabled: Boolean = true,
    val defaultZoomPercent: Int = 100,
    val forcePinchZoomEnabled: Boolean = true,
    val homeBackgroundPath: String = "",
    val homeBackgroundDimAlpha: Float = 0.35f,
    val totalBlockedCount: Long = 0L,
    val totalBytesSaved: Long = 0L,
    val seededBookmarks: Boolean = false,
    val seededExtensions: Boolean = false
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val AD_BLOCK_ENABLED = booleanPreferencesKey("ad_block_enabled")
        val BLOCK_ADS = booleanPreferencesKey("block_ads")
        val BLOCK_TRACKERS = booleanPreferencesKey("block_trackers")
        val BLOCK_SOCIAL = booleanPreferencesKey("block_social")
        val BLOCK_POPUPS = booleanPreferencesKey("block_popups")
        val COSMETIC_FILTERING = booleanPreferencesKey("cosmetic_filtering")
        val JAVASCRIPT_ENABLED = booleanPreferencesKey("javascript_enabled")
        val HTTPS_UPGRADE = booleanPreferencesKey("https_upgrade")
        val SEARCH_ENGINE = stringPreferencesKey("search_engine")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SHIELD_DISPLAY_MODE = stringPreferencesKey("shield_display_mode")
        val OMNIBOX_SHORTCUTS = stringPreferencesKey("omnibox_shortcuts")
        val MENU_LAYOUT_STYLE = stringPreferencesKey("menu_layout_style")
        val MENU_ITEMS = stringPreferencesKey("menu_items")
        val MIGRATED_PAGE_ZOOM_MENU = booleanPreferencesKey("migrated_page_zoom_menu")
        val EXTENSIONS_ENABLED = booleanPreferencesKey("extensions_enabled")
        val DEFAULT_ZOOM_PERCENT = intPreferencesKey("default_zoom_percent")
        val FORCE_PINCH_ZOOM = booleanPreferencesKey("force_pinch_zoom")
        val HOME_BACKGROUND_PATH = stringPreferencesKey("home_background_path")
        val HOME_BACKGROUND_DIM_ALPHA = floatPreferencesKey("home_background_dim_alpha")
        val TOTAL_BLOCKED_COUNT = longPreferencesKey("total_blocked_count")
        val TOTAL_BYTES_SAVED = longPreferencesKey("total_bytes_saved")
        val SEEDED_BOOKMARKS = booleanPreferencesKey("seeded_bookmarks")
        val SEEDED_EXTENSIONS = booleanPreferencesKey("seeded_extensions")
    }

    val preferencesFlow: Flow<BrowserPreferences> = context.browserDataStore.data.map { prefs ->
        BrowserPreferences(
            adBlockEnabled = prefs[Keys.AD_BLOCK_ENABLED] ?: true,
            blockAds = prefs[Keys.BLOCK_ADS] ?: true,
            blockTrackers = prefs[Keys.BLOCK_TRACKERS] ?: true,
            blockSocial = prefs[Keys.BLOCK_SOCIAL] ?: true,
            blockPopups = prefs[Keys.BLOCK_POPUPS] ?: true,
            cosmeticFiltering = prefs[Keys.COSMETIC_FILTERING] ?: true,
            javascriptEnabled = prefs[Keys.JAVASCRIPT_ENABLED] ?: true,
            httpsUpgradeEnabled = prefs[Keys.HTTPS_UPGRADE] ?: true,
            searchEngine = SearchEngine.fromName(prefs[Keys.SEARCH_ENGINE]),
            themeMode = ThemeMode.fromName(prefs[Keys.THEME_MODE]),
            shieldDisplayMode = ShieldDisplayMode.fromName(prefs[Keys.SHIELD_DISPLAY_MODE]),
            omniboxShortcuts = OmniboxShortcut.decodeList(prefs[Keys.OMNIBOX_SHORTCUTS]),
            menuLayoutStyle = MenuLayoutStyle.fromName(prefs[Keys.MENU_LAYOUT_STYLE]),
            menuItems = BrowserMenuItem.decodeOrderedList(prefs[Keys.MENU_ITEMS]),
            extensionsEnabled = prefs[Keys.EXTENSIONS_ENABLED] ?: true,
            defaultZoomPercent = (prefs[Keys.DEFAULT_ZOOM_PERCENT] ?: 100).coerceIn(50, 200),
            forcePinchZoomEnabled = prefs[Keys.FORCE_PINCH_ZOOM] ?: true,
            homeBackgroundPath = prefs[Keys.HOME_BACKGROUND_PATH].orEmpty(),
            homeBackgroundDimAlpha = (prefs[Keys.HOME_BACKGROUND_DIM_ALPHA] ?: 0.35f).coerceIn(0f, 0.75f),
            totalBlockedCount = prefs[Keys.TOTAL_BLOCKED_COUNT] ?: 0L,
            totalBytesSaved = prefs[Keys.TOTAL_BYTES_SAVED] ?: 0L,
            seededBookmarks = prefs[Keys.SEEDED_BOOKMARKS] ?: false,
            seededExtensions = prefs[Keys.SEEDED_EXTENSIONS] ?: false
        )
    }

    suspend fun setAdBlockEnabled(enabled: Boolean) {
        context.browserDataStore.edit { it[Keys.AD_BLOCK_ENABLED] = enabled }
    }

    suspend fun setBlockAds(enabled: Boolean) {
        context.browserDataStore.edit { it[Keys.BLOCK_ADS] = enabled }
    }

    suspend fun setBlockTrackers(enabled: Boolean) {
        context.browserDataStore.edit { it[Keys.BLOCK_TRACKERS] = enabled }
    }

    suspend fun setBlockSocial(enabled: Boolean) {
        context.browserDataStore.edit { it[Keys.BLOCK_SOCIAL] = enabled }
    }

    suspend fun setBlockPopups(enabled: Boolean) {
        context.browserDataStore.edit { it[Keys.BLOCK_POPUPS] = enabled }
    }

    suspend fun setCosmeticFiltering(enabled: Boolean) {
        context.browserDataStore.edit { it[Keys.COSMETIC_FILTERING] = enabled }
    }

    suspend fun setJavascriptEnabled(enabled: Boolean) {
        context.browserDataStore.edit { it[Keys.JAVASCRIPT_ENABLED] = enabled }
    }

    suspend fun setHttpsUpgradeEnabled(enabled: Boolean) {
        context.browserDataStore.edit { it[Keys.HTTPS_UPGRADE] = enabled }
    }

    suspend fun setSearchEngine(engine: SearchEngine) {
        context.browserDataStore.edit { it[Keys.SEARCH_ENGINE] = engine.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.browserDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setShieldDisplayMode(mode: ShieldDisplayMode) {
        context.browserDataStore.edit { it[Keys.SHIELD_DISPLAY_MODE] = mode.name }
    }

    suspend fun setOmniboxShortcuts(shortcuts: List<OmniboxShortcut>) {
        context.browserDataStore.edit {
            it[Keys.OMNIBOX_SHORTCUTS] = OmniboxShortcut.encodeList(shortcuts)
        }
    }

    suspend fun setMenuLayoutStyle(style: MenuLayoutStyle) {
        context.browserDataStore.edit { it[Keys.MENU_LAYOUT_STYLE] = style.name }
    }

    suspend fun setMenuItems(items: List<BrowserMenuItem>) {
        context.browserDataStore.edit {
            it[Keys.MENU_ITEMS] = BrowserMenuItem.encodeOrderedList(items)
        }
    }

    suspend fun setExtensionsEnabled(enabled: Boolean) {
        context.browserDataStore.edit { it[Keys.EXTENSIONS_ENABLED] = enabled }
    }

    suspend fun setDefaultZoomPercent(percent: Int) {
        context.browserDataStore.edit { it[Keys.DEFAULT_ZOOM_PERCENT] = percent.coerceIn(50, 200) }
    }

    suspend fun setForcePinchZoomEnabled(enabled: Boolean) {
        context.browserDataStore.edit { it[Keys.FORCE_PINCH_ZOOM] = enabled }
    }

    suspend fun setHomeBackgroundPath(path: String) {
        context.browserDataStore.edit { it[Keys.HOME_BACKGROUND_PATH] = path }
    }

    suspend fun setHomeBackgroundDimAlpha(alpha: Float) {
        context.browserDataStore.edit { it[Keys.HOME_BACKGROUND_DIM_ALPHA] = alpha.coerceIn(0f, 0.75f) }
    }

    /**
     * One-time migration so users who already saved a custom menu item list
     * still discover the newly added PAGE_ZOOM item automatically once.
     */
    suspend fun ensurePageZoomInCustomMenuIfNeeded() {
        context.browserDataStore.edit { prefs ->
            if (prefs[Keys.MIGRATED_PAGE_ZOOM_MENU] == true) return@edit
            val savedRaw = prefs[Keys.MENU_ITEMS]
            if (!savedRaw.isNullOrBlank()) {
                val decoded = BrowserMenuItem.decodeOrderedList(savedRaw)
                if (BrowserMenuItem.PAGE_ZOOM !in decoded) {
                    val mutable = decoded.toMutableList()
                    val insertIndex = mutable.indexOf(BrowserMenuItem.FIND_IN_PAGE)
                        .takeIf { it >= 0 }
                        ?: mutable.indexOf(BrowserMenuItem.FULLSCREEN).takeIf { it >= 0 }
                        ?: mutable.size
                    mutable.add(insertIndex, BrowserMenuItem.PAGE_ZOOM)
                    prefs[Keys.MENU_ITEMS] = BrowserMenuItem.encodeOrderedList(mutable)
                }
            }
            prefs[Keys.MIGRATED_PAGE_ZOOM_MENU] = true
        }
    }

    suspend fun markBookmarksSeeded() {
        context.browserDataStore.edit { it[Keys.SEEDED_BOOKMARKS] = true }
    }

    suspend fun markExtensionsSeeded() {
        context.browserDataStore.edit { it[Keys.SEEDED_EXTENSIONS] = true }
    }

    suspend fun incrementBlockedStats(countDelta: Long = 1L, estimatedBytesDelta: Long = 48_000L) {
        context.browserDataStore.edit { prefs ->
            prefs[Keys.TOTAL_BLOCKED_COUNT] = (prefs[Keys.TOTAL_BLOCKED_COUNT] ?: 0L) + countDelta
            prefs[Keys.TOTAL_BYTES_SAVED] = (prefs[Keys.TOTAL_BYTES_SAVED] ?: 0L) + estimatedBytesDelta
        }
    }

    suspend fun resetBlockedStats() {
        context.browserDataStore.edit { prefs ->
            prefs[Keys.TOTAL_BLOCKED_COUNT] = 0L
            prefs[Keys.TOTAL_BYTES_SAVED] = 0L
        }
    }
}
