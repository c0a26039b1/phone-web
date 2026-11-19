package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.adblock.AdBlockEngine
import com.example.adblock.BlockCategory
import com.example.adblock.ShieldConfig
import com.example.data.local.BrowserMenuItem
import com.example.data.local.MenuLayoutStyle
import com.example.data.local.OmniboxShortcut
import com.example.data.local.SearchEngine
import com.example.data.local.SitePermissionEntity
import com.example.data.local.SitePermissionState
import com.example.data.local.SitePermissionType
import com.example.ui.browser.BrowserViewModel
import com.example.ui.browser.ChromeWebStoreDownloader
import com.example.ui.browser.ExtensionEngine
import com.example.ui.browser.ExtractedPageContent
import com.example.ui.browser.FirebasePageAiAnalyzer
import com.example.ui.browser.PageContentExtractor
import com.example.ui.browser.PageTranslationHelper
import com.example.ui.browser.TabGroupColor
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MrRブラウザ", appName)
    }

    @Test
    fun `ad block engine blocks global and japanese ad networks`() {
        val engine = AdBlockEngine()

        val doubleclick = engine.evaluateRequest(
            requestUrl = "https://securepubads.g.doubleclick.net/tag/js/gpt.js",
            pageHost = "news.example.jp",
            isMainFrame = false
        )
        assertTrue(doubleclick.shouldBlock)
        assertEquals(BlockCategory.AD, doubleclick.category)

        val imobile = engine.evaluateRequest(
            requestUrl = "https://spdeliver.i-mobile.co.jp/script/ad.js",
            pageHost = "blog.example.jp",
            isMainFrame = false
        )
        assertTrue(imobile.shouldBlock)
        assertEquals(BlockCategory.AD, imobile.category)
    }

    @Test
    fun `ad block engine respects allowlist and custom blocklist`() {
        val engine = AdBlockEngine()
        engine.updateConfig(
            ShieldConfig(
                allowlistedDomains = setOf("trusted-news.jp"),
                customBlockedDomains = setOf("annoying.example.com")
            )
        )

        val allowedSiteAd = engine.evaluateRequest(
            requestUrl = "https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js",
            pageHost = "www.trusted-news.jp",
            isMainFrame = false
        )
        assertFalse(allowedSiteAd.shouldBlock)

        val customBlocked = engine.evaluateRequest(
            requestUrl = "https://sub.annoying.example.com/banner.js",
            pageHost = "other-site.jp",
            isMainFrame = false
        )
        assertTrue(customBlocked.shouldBlock)
        assertEquals(BlockCategory.CUSTOM, customBlocked.category)
    }

    @Test
    fun `url normalization upgrades http and routes search queries`() {
        val upgraded = AdBlockEngine.normalizeInputToUrl(
            input = "http://example.jp/article",
            searchEngine = SearchEngine.DUCKDUCKGO,
            httpsUpgrade = true
        )
        assertEquals("https://example.jp/article", upgraded)

        val domainOnly = AdBlockEngine.normalizeInputToUrl(
            input = "ja.wikipedia.org",
            searchEngine = SearchEngine.DUCKDUCKGO,
            httpsUpgrade = true
        )
        assertEquals("https://ja.wikipedia.org", domainOnly)

        val searchQuery = AdBlockEngine.normalizeInputToUrl(
            input = "東京 天気",
            searchEngine = SearchEngine.DUCKDUCKGO,
            httpsUpgrade = true
        )
        assertTrue(searchQuery.startsWith("https://duckduckgo.com/?q="))
    }

    @Test
    fun `tab grouping creates assigns ungroups and prunes empty groups`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = BrowserViewModel(app)

        val firstTabId = vm.uiState.value.tabs.first().id
        val secondTabId = vm.createNewTab(isPrivate = false)

        val groupId = vm.createTabGroup(
            name = "ニュース",
            color = TabGroupColor.CYAN,
            tabIds = listOf(firstTabId, secondTabId)
        )

        val afterGroup = vm.uiState.value
        assertEquals(1, afterGroup.tabGroups.size)
        assertEquals("ニュース", afterGroup.tabGroups.first().name)
        assertEquals(2, afterGroup.tabs.count { it.groupId == groupId })

        // Rename & recolor group
        vm.updateTabGroup(groupId, "調査", TabGroupColor.AMBER)
        assertEquals("調査", vm.uiState.value.tabGroups.first().name)
        assertEquals(TabGroupColor.AMBER, vm.uiState.value.tabGroups.first().color)

        // Remove tabs from group -> empty group auto-prunes
        vm.assignTabToGroup(firstTabId, null)
        assertEquals(1, vm.uiState.value.tabGroups.size)
        vm.assignTabToGroup(secondTabId, null)
        assertTrue(vm.uiState.value.tabGroups.isEmpty())
        assertNull(vm.uiState.value.tabs.first().groupId)

        // Create group and ungroup directly
        val group2 = vm.createTabGroup("仕事", TabGroupColor.EMERALD, listOf(firstTabId))
        assertEquals(1, vm.uiState.value.tabGroups.size)
        vm.ungroupTabs(group2)
        assertTrue(vm.uiState.value.tabGroups.isEmpty())
        assertEquals(2, vm.uiState.value.tabs.size)
    }

    @Test
    fun `creating new tab in tab switcher stays on tabs screen and drag merge forms group`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = BrowserViewModel(app)

        vm.navigateTo(com.example.ui.browser.BrowserDestination.TABS)
        val tab1 = vm.uiState.value.tabs.first().id
        val tab2 = vm.createNewTab(isPrivate = false, keepCurrentDestination = true)
        val tab3 = vm.createNewTab(isPrivate = false, keepCurrentDestination = true)

        assertEquals(com.example.ui.browser.BrowserDestination.TABS, vm.uiState.value.destination)
        assertEquals(3, vm.uiState.value.tabs.size)

        // Drag tab1 onto tab2 -> creates a new group containing both
        vm.mergeTabsIntoGroup(sourceTabId = tab1, targetTabId = tab2)
        assertEquals(1, vm.uiState.value.tabGroups.size)
        val createdGroupId = vm.uiState.value.tabGroups.first().id
        assertEquals(2, vm.uiState.value.tabs.count { it.groupId == createdGroupId })

        // Drag tab3 onto tab2 -> joins the same existing group
        vm.mergeTabsIntoGroup(sourceTabId = tab3, targetTabId = tab2)
        assertEquals(1, vm.uiState.value.tabGroups.size)
        assertEquals(3, vm.uiState.value.tabs.count { it.groupId == createdGroupId })
    }

    @Test
    fun `search url detection distinguishes search engines from regular websites`() {
        assertTrue(AdBlockEngine.isSearchUrl("https://www.google.com/search?q=youtube"))
        assertTrue(AdBlockEngine.isSearchUrl("https://duckduckgo.com/?q=news"))
        assertTrue(AdBlockEngine.isSearchUrl("https://search.yahoo.co.jp/search?p=tokyo"))
        assertTrue(AdBlockEngine.isSearchUrl("https://www.startpage.com/sp/search?query=privacy"))
        assertFalse(AdBlockEngine.isSearchUrl("https://www.youtube.com/watch?v=123"))
        assertFalse(AdBlockEngine.isSearchUrl("https://github.com/"))
    }

    @Test
    fun `omnibox shortcuts support multi-selection encode decode and defaults`() {
        assertEquals(OmniboxShortcut.DEFAULT_SELECTION, OmniboxShortcut.decodeList(null))
        assertEquals(OmniboxShortcut.DEFAULT_SELECTION, OmniboxShortcut.decodeList("UNKNOWN"))

        val selected = listOf(
            OmniboxShortcut.SHARE,
            OmniboxShortcut.TRANSLATE,
            OmniboxShortcut.FULL_SCREENSHOT
        )
        val encoded = OmniboxShortcut.encodeList(selected)
        val decoded = OmniboxShortcut.decodeList(encoded)
        assertEquals(
            listOf(
                OmniboxShortcut.FULL_SCREENSHOT,
                OmniboxShortcut.TRANSLATE,
                OmniboxShortcut.SHARE
            ),
            decoded
        )
    }

    @Test
    fun `page translation helper builds translate url and extracts original url`() {
        val original = "https://en.wikipedia.org/wiki/Android"
        val translated = PageTranslationHelper.buildTranslateUrl(original, "ja")
        assertTrue(translated.startsWith("https://translate.google.com/translate?sl=auto&tl=ja&u="))
        assertTrue(PageTranslationHelper.isTranslatedUrl(translated))
        assertEquals(original, PageTranslationHelper.extractOriginalUrl(translated))

        val proxyUrl = "https://en-wikipedia-org.translate.goog/wiki/Android?_x_tr_sl=auto&_x_tr_tl=ja"
        assertTrue(PageTranslationHelper.isTranslatedUrl(proxyUrl))
        assertEquals("https://en.wikipedia.org/wiki/Android", PageTranslationHelper.extractOriginalUrl(proxyUrl))
    }

    @Test
    fun `site permission entity manages camera microphone and location states independently`() {
        val initial = SitePermissionEntity(domain = "meet.google.com")
        assertEquals(SitePermissionState.ASK, initial.cameraState)
        assertEquals(SitePermissionState.ASK, initial.microphoneState)
        assertEquals(SitePermissionState.ASK, initial.locationState)

        val updated = initial
            .withState(SitePermissionType.CAMERA, SitePermissionState.ALLOW)
            .withState(SitePermissionType.MICROPHONE, SitePermissionState.ALLOW)
            .withState(SitePermissionType.LOCATION, SitePermissionState.DENY)

        assertEquals(SitePermissionState.ALLOW, updated.stateFor(SitePermissionType.CAMERA))
        assertEquals(SitePermissionState.ALLOW, updated.stateFor(SitePermissionType.MICROPHONE))
        assertEquals(SitePermissionState.DENY, updated.stateFor(SitePermissionType.LOCATION))
    }

    @Test
    fun `browser menu items preserve custom order and menu layout style decodes safely`() {
        assertEquals(MenuLayoutStyle.POPUP_LIST, MenuLayoutStyle.fromName(null))
        assertEquals(MenuLayoutStyle.CLASSIC_SHEET, MenuLayoutStyle.fromName("CLASSIC_SHEET"))

        val customOrder = listOf(
            BrowserMenuItem.SETTINGS,
            BrowserMenuItem.AI_PAGE_SOLVER,
            BrowserMenuItem.BOOKMARKS,
            BrowserMenuItem.HISTORY
        )
        val encoded = BrowserMenuItem.encodeOrderedList(customOrder)
        val decoded = BrowserMenuItem.decodeOrderedList(encoded)
        assertEquals(customOrder, decoded)
        assertEquals(BrowserMenuItem.DEFAULT_ORDER, BrowserMenuItem.decodeOrderedList(""))
    }

    @Test
    fun `page content extractor parses js payload and firebase ai analyzer parses structured json`() {
        val rawJs = "\"{\\\"title\\\":\\\"数学テスト\\\",\\\"url\\\":\\\"https://quiz.example.jp/1\\\",\\\"headings\\\":[\\\"第1問\\\"],\\\"questions\\\":[\\\"問1: 2x + 4 = 10 の解を求めなさい。\\\"],\\\"text\\\":\\\"問1: 2x + 4 = 10 の解を求めなさい。\\\\n日本の首都はどこですか？\\\"}\""
        val extracted = PageContentExtractor.parseJsPayload(
            rawResult = rawJs,
            fallbackTitle = "fallback",
            fallbackUrl = "https://fallback.example.jp"
        )
        assertEquals("数学テスト", extracted.title)
        assertEquals("https://quiz.example.jp/1", extracted.url)
        assertTrue(extracted.fullText.contains("2x + 4 = 10"))

        val structuredJson = """
            {
              "pageSummary": "一次方程式と地理に関する2問の確認テストです。",
              "problems": [
                {
                  "question": "問1: 2x + 4 = 10 の解を求めなさい。",
                  "answer": "x = 3",
                  "explanation": "両辺から4を引くと 2x = 6 となり、両辺を2で割ると x = 3 です。"
                },
                {
                  "question": "日本の首都はどこですか？",
                  "answer": "東京",
                  "explanation": "日本の首都は東京都です。"
                }
              ]
            }
        """.trimIndent()
        val result = FirebasePageAiAnalyzer.parseGeminiJsonResponse(structuredJson, extracted)
        assertFalse(result.isPlaceholder)
        assertEquals(2, result.problems.size)
        assertEquals("x = 3", result.problems.first().answer)
        assertEquals("東京", result.problems[1].answer)
    }

    @Test
    fun `extension engine matches url patterns and parses user scripts and zip extensions`() {
        assertTrue(ExtensionEngine.matchesUrl("https://www.youtube.com/watch?v=1", "*.youtube.com"))
        assertTrue(ExtensionEngine.matchesUrl("https://example.org/test", "*"))
        assertFalse(ExtensionEngine.matchesUrl("https://news.yahoo.co.jp/", "github.com"))

        val userScript = """
            // ==UserScript==
            // @name         YouTube Helper
            // @version      2.1.0
            // @description  Enhances YouTube playback
            // @match        https://*.youtube.com/*
            // ==/UserScript==
            console.log('ok');
        """.trimIndent()
        val parsedUser = ExtensionEngine.parseUserScript(userScript, "yt.user.js")
        assertEquals("YouTube Helper", parsedUser.name)
        assertEquals("2.1.0", parsedUser.version)
        assertTrue(parsedUser.matchPatterns.contains("youtube.com"))
        assertTrue(parsedUser.enabled)

        // Build an in-memory WebExtension ZIP with manifest.json + content script + CSS
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("manifest.json"))
            zos.write(
                """
                {
                  "manifest_version": 3,
                  "name": "Sample WebExt",
                  "version": "1.3.0",
                  "description": "Test MV3 extension",
                  "content_scripts": [
                    {
                      "matches": ["https://*.example.com/*"],
                      "js": ["content.js"],
                      "css": ["style.css"]
                    }
                  ]
                }
                """.trimIndent().toByteArray(Charsets.UTF_8)
            )
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("content.js"))
            zos.write("document.title = 'Injected';".toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("style.css"))
            zos.write("body { border: 1px solid green; }".toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        val parsedZip = ExtensionEngine.parseZipExtension(ByteArrayInputStream(baos.toByteArray()), "sample.zip")
        assertEquals("Sample WebExt", parsedZip.name)
        assertEquals("1.3.0", parsedZip.version)
        assertTrue(parsedZip.jsCode.contains("Injected"))
        assertTrue(parsedZip.cssCode.contains("border: 1px solid green"))

        val engine = ExtensionEngine()
        engine.updateState(enabled = true, extensions = listOf(parsedZip))
        val script = engine.buildCombinedInjectionScript("https://sub.example.com/page")
        assertTrue(script != null && script.contains("Injected"))
        assertTrue(script != null && script.contains("chrome.runtime") || script!!.contains("storage:"))
        assertNull(engine.buildCombinedInjectionScript("https://other.org/page"))
    }

    @Test
    fun `chrome web store downloader extracts ids strips crx3 header and resolves localized manifest`() {
        val darkReaderId = "eimadpbcbfnmbkopoojfekhnkhdbieeh"
        assertEquals(
            darkReaderId,
            ChromeWebStoreDownloader.extractExtensionId(
                "https://chromewebstore.google.com/detail/dark-reader/$darkReaderId?hl=ja"
            )
        )
        assertEquals(
            darkReaderId,
            ChromeWebStoreDownloader.extractExtensionId(
                "https://chrome.google.com/webstore/detail/dark-reader/$darkReaderId"
            )
        )
        assertEquals(darkReaderId, ChromeWebStoreDownloader.extractExtensionId(darkReaderId))
        assertNull(ChromeWebStoreDownloader.extractExtensionId("https://example.com/not-an-extension"))
        assertTrue(
            ChromeWebStoreDownloader.isChromeWebStoreDetailUrl(
                "https://chromewebstore.google.com/detail/dark-reader/$darkReaderId"
            )
        )
        assertEquals(
            "Dark Reader",
            ChromeWebStoreDownloader.cleanStorePageTitle("Dark Reader - Chrome ウェブストア", darkReaderId)
        )

        // Build ZIP with _locales/ja/messages.json and ./relative.js path
        val zipOut = ByteArrayOutputStream()
        ZipOutputStream(zipOut).use { zos ->
            zos.putNextEntry(ZipEntry("manifest.json"))
            zos.write(
                """
                {
                  "manifest_version": 3,
                  "name": "__MSG_extName__",
                  "version": "4.9.0",
                  "description": "__MSG_extDesc__",
                  "default_locale": "en",
                  "content_scripts": [
                    {
                      "matches": ["<all_urls>"],
                      "js": ["./inject/main.js"]
                    }
                  ]
                }
                """.trimIndent().toByteArray(Charsets.UTF_8)
            )
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("_locales/ja/messages.json"))
            zos.write(
                """
                {
                  "extName": { "message": "ダークリーダーJP" },
                  "extDesc": { "message": "すべてのWebサイトをダークテーマ化します" }
                }
                """.trimIndent().toByteArray(Charsets.UTF_8)
            )
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("inject/main.js"))
            zos.write("chrome.storage.local.get('mode', function(){});".toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
        val zipBytes = zipOut.toByteArray()

        // Wrap ZIP inside a valid CRX3 binary header ("Cr24" + version 3 + protobuf header length + dummy header)
        val dummyProtoHeader = byteArrayOf(0x0A, 0x04, 0x01, 0x02, 0x03, 0x04)
        val crx3Buffer = ByteBuffer.allocate(12 + dummyProtoHeader.size + zipBytes.size)
            .order(ByteOrder.LITTLE_ENDIAN)
            .put("Cr24".toByteArray(Charsets.US_ASCII))
            .putInt(3)
            .putInt(dummyProtoHeader.size)
            .put(dummyProtoHeader)
            .put(zipBytes)
            .array()

        val entity = ExtensionEngine.parseCrxOrZipExtension(
            rawBytes = crx3Buffer,
            fallbackName = "fallback.crx",
            customId = "cws_$darkReaderId",
            iconKey = "chrome_store"
        )
        assertEquals("cws_$darkReaderId", entity.id)
        assertEquals("ダークリーダーJP", entity.name)
        assertEquals("すべてのWebサイトをダークテーマ化します", entity.description)
        assertEquals("4.9.0", entity.version)
        assertTrue(entity.jsCode.contains("chrome.storage.local.get"))
    }

    @Test
    fun `streaming crx3 parser handles archives over 25mb and externalizes large content scripts`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempCrx = File.createTempFile("large_ext_test_", ".crx", context.cacheDir)
        val scriptsDir = File(context.cacheDir, "ext_scripts_test_${System.nanoTime()}")
        try {
            // Write a CRX3 file > 26 MB (stored binary asset + manifest + large >450KB content script)
            val dummyProto = byteArrayOf(0x0A, 0x02, 0x01, 0x02)
            val largeJsPayload = "window.__largeMarker = 'BIG_OK';\n" + "// ".repeat(160_000)
            FileOutputStream(tempCrx).use { fos ->
                val crxHeader = ByteBuffer.allocate(12 + dummyProto.size)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .put("Cr24".toByteArray(Charsets.US_ASCII))
                    .putInt(3)
                    .putInt(dummyProto.size)
                    .put(dummyProto)
                    .array()
                fos.write(crxHeader)

                ZipOutputStream(fos).use { zos ->
                    // 26 MB uncompressed STORED dummy asset (e.g., WASM/dictionary bundle)
                    val chunk = ByteArray(1024 * 1024) { 0x2A }
                    val chunkCount = 26
                    val crc = CRC32()
                    repeat(chunkCount) { crc.update(chunk) }
                    val bigEntry = ZipEntry("assets/dictionary.wasm").apply {
                        method = ZipEntry.STORED
                        size = chunk.size.toLong() * chunkCount
                        compressedSize = size
                        this.crc = crc.value
                    }
                    zos.putNextEntry(bigEntry)
                    repeat(chunkCount) { zos.write(chunk) }
                    zos.closeEntry()

                    zos.putNextEntry(ZipEntry("manifest.json"))
                    zos.write(
                        """
                        {
                          "manifest_version": 3,
                          "name": "Large Extension",
                          "version": "2.0.0",
                          "description": "Over 25MB test extension",
                          "content_scripts": [
                            {
                              "matches": ["https://*.example.com/*"],
                              "js": ["content.js"]
                            }
                          ]
                        }
                        """.trimIndent().toByteArray(Charsets.UTF_8)
                    )
                    zos.closeEntry()

                    zos.putNextEntry(ZipEntry("content.js"))
                    zos.write(largeJsPayload.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }
            }

            assertTrue("Expected CRX file > 25MB", tempCrx.length() > 25L * 1024L * 1024L)

            val parsed = ExtensionEngine.parseCrxOrZipFile(
                file = tempCrx,
                fallbackName = "large.crx",
                customId = "cws_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                iconKey = "chrome_store"
            )
            assertEquals("Large Extension", parsed.name)
            assertTrue(parsed.jsCode.contains("BIG_OK"))

            // Externalize large script to stay safely below SQLite CursorWindow limit
            val stored = ExtensionEngine.externalizeLargeCodeIfNeeded(parsed, scriptsDir)
            assertTrue(ExtensionEngine.isExternalCodeRef(stored.jsCode))

            val engine = ExtensionEngine(scriptsDir = scriptsDir)
            engine.updateState(enabled = true, extensions = listOf(stored))
            val injected = engine.buildCombinedInjectionScript("https://docs.example.com/test")
            assertTrue(injected != null && injected.contains("BIG_OK"))

            ExtensionEngine.deleteExternalCodeFiles(stored.id, scriptsDir)
            assertFalse(File(scriptsDir, "${stored.id}.js").exists())
        } finally {
            runCatching { tempCrx.delete() }
            runCatching { scriptsDir.deleteRecursively() }
        }
    }

    @Test
    fun `page zoom script applies zoom clamps bounds and viewmodel steps zoom cleanly`() {
        val zoomedScript = com.example.ui.browser.WebViewRegistry.buildPageZoomScript(
            zoomPercent = 150,
            forcePinchZoom = true
        )
        assertTrue(zoomedScript.contains("zoom: 150% !important"))
        assertTrue(zoomedScript.contains("user-scalable=yes"))

        val resetScript = com.example.ui.browser.WebViewRegistry.buildPageZoomScript(
            zoomPercent = 100,
            forcePinchZoom = false
        )
        assertTrue(resetScript.contains("removeProperty('zoom')"))
        assertFalse(resetScript.contains("user-scalable=yes"))

        val clampedHigh = com.example.ui.browser.WebViewRegistry.buildPageZoomScript(
            zoomPercent = 999,
            forcePinchZoom = false
        )
        assertTrue(clampedHigh.contains("zoom: 300% !important"))

        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = BrowserViewModel(app)
        assertEquals(100, vm.uiState.value.activeTab.zoomPercent)
        assertEquals(110, vm.stepActiveTabZoom(10))
        assertEquals(110, vm.uiState.value.activeTab.zoomPercent)
        assertEquals(100, vm.stepActiveTabZoom(-10))
        assertEquals(50, vm.setActiveTabZoom(10))
        assertEquals(300, vm.setActiveTabZoom(500))
        assertEquals(100, vm.resetActiveTabZoom())
    }

    @Test
    fun `home background storage computes safe sample size and cleans up wallpaper directory`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals(1, com.example.ui.browser.HomeBackgroundStorage.calculateInSampleSize(1200, 800, 2048))
        assertEquals(2, com.example.ui.browser.HomeBackgroundStorage.calculateInSampleSize(6000, 4000, 2048))

        val dir = com.example.ui.browser.HomeBackgroundStorage.getWallpaperDir(context)
        val dummy = File(dir, "bg_test.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        assertTrue(dummy.exists())
        com.example.ui.browser.HomeBackgroundStorage.deleteAllWallpapers(context)
        assertFalse(dummy.exists())
    }

    @Test
    fun `mobile user agent removes webview and x86 tokens and youtube url normalizes cleanly`() {
        val rawEmulatorUa =
            "Mozilla/5.0 (Linux; Android 14; sdk_gphone64_x86_64 Build/UE1A.230829.036; wv) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/128.0.6613.88 Safari/537.36"
        val cleanMobileUa = com.example.ui.browser.WebViewRegistry.buildMobileUserAgent(rawEmulatorUa)
        assertTrue(cleanMobileUa.contains("Android"))
        assertTrue(cleanMobileUa.contains("Mobile Safari/537.36"))
        assertTrue(cleanMobileUa.contains("Chrome/128.0.6613.88"))
        assertFalse(cleanMobileUa.contains("; wv"))
        assertFalse(cleanMobileUa.contains("Version/4.0"))
        assertFalse(cleanMobileUa.contains("x86_64"))

        // Mobile mode routes standard www.youtube.com pages to m.youtube.com and removes app=desktop
        assertEquals(
            "https://m.youtube.com/watch?v=dQw4w9WgXcQ",
            com.example.ui.browser.WebViewRegistry.normalizeUrlForViewportMode(
                url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ&app=desktop&persist_app=1",
                isDesktopMode = false
            )
        )
        assertEquals(
            "https://m.youtube.com/",
            com.example.ui.browser.WebViewRegistry.normalizeUrlForViewportMode(
                url = "https://youtube.com/",
                isDesktopMode = false
            )
        )
        // Embeds & non-standard YouTube endpoints remain untouched
        assertEquals(
            "https://www.youtube.com/embed/dQw4w9WgXcQ",
            com.example.ui.browser.WebViewRegistry.normalizeUrlForViewportMode(
                url = "https://www.youtube.com/embed/dQw4w9WgXcQ",
                isDesktopMode = false
            )
        )
        // Desktop mode cleanly switches mobile hosts to desktop hosts
        assertEquals(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            com.example.ui.browser.WebViewRegistry.normalizeUrlForViewportMode(
                url = "https://m.youtube.com/watch?v=dQw4w9WgXcQ",
                isDesktopMode = true
            )
        )
        assertEquals(
            "https://ja.wikipedia.org/wiki/Android",
            com.example.ui.browser.WebViewRegistry.normalizeUrlForViewportMode(
                url = "https://ja.m.wikipedia.org/wiki/Android",
                isDesktopMode = true
            )
        )
    }

    @Test
    fun `bottom bar adjacent tab switching navigates left and right without out of bounds`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = BrowserViewModel(app)

        val tab0 = vm.uiState.value.tabs.first().id
        val tab1 = vm.createNewTab(isPrivate = false)
        val tab2 = vm.createNewTab(isPrivate = false)

        // Currently on tab2 (index 2)
        assertEquals(tab2, vm.uiState.value.activeTabId)
        assertFalse(vm.switchToAdjacentTab(+1))
        assertEquals(tab2, vm.uiState.value.activeTabId)

        assertTrue(vm.switchToAdjacentTab(-1))
        assertEquals(tab1, vm.uiState.value.activeTabId)

        assertTrue(vm.switchToAdjacentTab(-1))
        assertEquals(tab0, vm.uiState.value.activeTabId)

        assertFalse(vm.switchToAdjacentTab(-1))
        assertEquals(tab0, vm.uiState.value.activeTabId)

        assertTrue(vm.switchToAdjacentTab(+1))
        assertEquals(tab1, vm.uiState.value.activeTabId)
    }
}
