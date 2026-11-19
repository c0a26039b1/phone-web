package com.example.ui.browser

import com.example.adblock.AdBlockEngine
import com.example.data.local.BrowserExtensionEntity
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.ZipInputStream

class ExtensionEngine(
    private val scriptsDir: File? = null
) {

    private val masterEnabled = AtomicBoolean(true)
    private val activeExtensions = AtomicReference<List<BrowserExtensionEntity>>(emptyList())

    fun updateState(enabled: Boolean, extensions: List<BrowserExtensionEntity>) {
        masterEnabled.set(enabled)
        activeExtensions.set(extensions.filter { it.enabled })
    }

    fun isMasterEnabled(): Boolean = masterEnabled.get()

    fun getMatchingExtensions(url: String): List<BrowserExtensionEntity> {
        if (!masterEnabled.get()) return emptyList()
        if (url.isBlank() || url.startsWith("about:")) return emptyList()
        return activeExtensions.get().filter { ext ->
            matchesUrl(url, ext.matchPatterns)
        }
    }

    fun buildCombinedInjectionScript(url: String, forceReapply: Boolean = false): String? {
        val matching = getMatchingExtensions(url)
        if (matching.isEmpty()) return null
        return matching.joinToString("\n") { ext ->
            buildInjectionScript(ext, forceReapply = forceReapply, scriptsDir = scriptsDir)
        }
    }

    companion object {
        private const val MAX_ENTRY_BYTES = 16 * 1024 * 1024 // 16 MB per referenced script/CSS file
        private const val MAX_LOCALE_BYTES = 2 * 1024 * 1024
        private const val DB_INLINE_CODE_LIMIT = 400_000 // Keep Room rows safely under SQLite CursorWindow limit
        const val EXTERNAL_FILE_PREFIX = "@file:"

        fun matchesUrl(url: String, rawPatterns: String): Boolean {
            if (url.isBlank()) return false
            val host = AdBlockEngine.extractHost(url).lowercase()
            val lowerUrl = url.lowercase()
            val patterns = rawPatterns
                .split(",", "\n", ";")
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }

            if (patterns.isEmpty()) return true

            return patterns.any { pattern ->
                when {
                    pattern == "*" ||
                        pattern == "<all_urls>" ||
                        pattern == "*://*/*" ||
                        pattern == "http://*/*" ||
                        pattern == "https://*/*" -> true

                    pattern.contains("://") -> {
                        val normalized = pattern.replace("://*.", "://__ANY_SUB__")
                        val regexStr = "^" + normalized.split("*")
                            .joinToString(".*") { part ->
                                part.split("__ANY_SUB__").joinToString("(?:[^/]+\\.)?") { Regex.escape(it) }
                            } + "$"
                        runCatching { Regex(regexStr).matches(lowerUrl) }.getOrDefault(false)
                    }

                    pattern.startsWith("*.") -> {
                        val suffix = pattern.removePrefix("*.")
                        host == suffix || host.endsWith(".$suffix")
                    }

                    else -> {
                        val cleanDomain = AdBlockEngine.sanitizeDomainInput(pattern)
                        cleanDomain.isNotEmpty() && (host == cleanDomain || host.endsWith(".$cleanDomain"))
                    }
                }
            }
        }

        fun buildInjectionScript(
            extension: BrowserExtensionEntity,
            forceReapply: Boolean = false,
            scriptsDir: File? = null
        ): String {
            val resolvedCss = resolveCode(extension.cssCode, scriptsDir)
            val resolvedJs = resolveCode(extension.jsCode, scriptsDir)
            val safeKey = extension.id.replace(Regex("[^a-zA-Z0-9_]"), "_")
            val styleId = "aegis_ext_style_$safeKey"
            val quotedExtId = JSONObject.quote(extension.id)
            val quotedExtName = JSONObject.quote(extension.name)
            val quotedExtVer = JSONObject.quote(extension.version)
            val guardCheck = if (forceReapply) {
                ""
            } else {
                "if (window.__aegisExt_$safeKey) return; window.__aegisExt_$safeKey = true;"
            }

            val cssSnippet = if (resolvedCss.isNotBlank()) {
                val quotedCss = JSONObject.quote(resolvedCss)
                """
                (function() {
                    var existing = document.getElementById('$styleId');
                    if (existing) existing.remove();
                    var st = document.createElement('style');
                    st.id = '$styleId';
                    st.textContent = $quotedCss;
                    (document.head || document.documentElement).appendChild(st);
                })();
                """.trimIndent()
            } else {
                ""
            }

            val chromeShimSnippet = if (resolvedJs.isNotBlank()) {
                """
                var __aegisStorePrefix = '__aegis_ext_${safeKey}_';
                function __aegisMakeStorage() {
                    return {
                        get: function(keys, cb) {
                            var out = {};
                            try {
                                if (keys == null) {
                                    for (var i = 0; i < localStorage.length; i++) {
                                        var k = localStorage.key(i);
                                        if (k && k.indexOf(__aegisStorePrefix) === 0) {
                                            out[k.slice(__aegisStorePrefix.length)] = JSON.parse(localStorage.getItem(k));
                                        }
                                    }
                                } else if (typeof keys === 'string') {
                                    var raw = localStorage.getItem(__aegisStorePrefix + keys);
                                    if (raw !== null) out[keys] = JSON.parse(raw);
                                } else if (Array.isArray(keys)) {
                                    keys.forEach(function(key) {
                                        var raw = localStorage.getItem(__aegisStorePrefix + key);
                                        if (raw !== null) out[key] = JSON.parse(raw);
                                    });
                                } else if (typeof keys === 'object') {
                                    Object.keys(keys).forEach(function(key) {
                                        var raw = localStorage.getItem(__aegisStorePrefix + key);
                                        out[key] = raw !== null ? JSON.parse(raw) : keys[key];
                                    });
                                }
                            } catch (e) {}
                            if (typeof cb === 'function') cb(out);
                            return Promise.resolve(out);
                        },
                        set: function(items, cb) {
                            try {
                                if (items && typeof items === 'object') {
                                    Object.keys(items).forEach(function(key) {
                                        localStorage.setItem(__aegisStorePrefix + key, JSON.stringify(items[key]));
                                    });
                                }
                            } catch (e) {}
                            if (typeof cb === 'function') cb();
                            return Promise.resolve();
                        },
                        remove: function(keys, cb) {
                            try {
                                var list = Array.isArray(keys) ? keys : [keys];
                                list.forEach(function(key) { localStorage.removeItem(__aegisStorePrefix + key); });
                            } catch (e) {}
                            if (typeof cb === 'function') cb();
                            return Promise.resolve();
                        },
                        clear: function(cb) {
                            try {
                                var toRemove = [];
                                for (var i = 0; i < localStorage.length; i++) {
                                    var k = localStorage.key(i);
                                    if (k && k.indexOf(__aegisStorePrefix) === 0) toRemove.push(k);
                                }
                                toRemove.forEach(function(k) { localStorage.removeItem(k); });
                            } catch (e) {}
                            if (typeof cb === 'function') cb();
                            return Promise.resolve();
                        }
                    };
                }
                var __aegisEvent = function() {
                    return { addListener: function() {}, removeListener: function() {}, hasListener: function() { return false; } };
                };
                var chrome = (window.chrome && window.chrome.runtime) ? window.chrome : {
                    runtime: {
                        id: $quotedExtId,
                        getManifest: function() { return { name: $quotedExtName, version: $quotedExtVer }; },
                        getURL: function(p) { return String(p || ''); },
                        sendMessage: function() {
                            var cb = arguments[arguments.length - 1];
                            if (typeof cb === 'function') { try { cb(undefined); } catch (e) {} }
                            return Promise.resolve(undefined);
                        },
                        onMessage: __aegisEvent(),
                        lastError: null
                    },
                    storage: {
                        local: __aegisMakeStorage(),
                        sync: __aegisMakeStorage(),
                        session: __aegisMakeStorage(),
                        onChanged: __aegisEvent()
                    },
                    i18n: {
                        getMessage: function(k) { return k || ''; },
                        getUILanguage: function() { return navigator.language || 'ja'; }
                    }
                };
                var browser = chrome;
                """.trimIndent()
            } else {
                ""
            }

            val jsSnippet = if (resolvedJs.isNotBlank()) {
                """
                try {
                    $chromeShimSnippet
                    $resolvedJs
                } catch (e) {}
                """.trimIndent()
            } else {
                ""
            }

            return """
                (function() {
                    try {
                        $guardCheck
                        $cssSnippet
                        $jsSnippet
                    } catch (e) {}
                })();
            """.trimIndent()
        }

        fun parseUserScript(source: String, fallbackName: String = "カスタムスクリプト"): BrowserExtensionEntity {
            val trimmed = source.trim()
            require(trimmed.isNotEmpty()) { "スクリプトが空です" }

            var name = fallbackName.removeSuffix(".user.js").removeSuffix(".js").ifBlank { "カスタムスクリプト" }
            var description = "インポートされたユーザースクリプト"
            var version = "1.0.0"
            val matches = mutableListOf<String>()

            val metaStart = trimmed.indexOf("==UserScript==")
            val metaEnd = trimmed.indexOf("==/UserScript==")
            if (metaStart >= 0 && metaEnd > metaStart) {
                val metaBlock = trimmed.substring(metaStart, metaEnd)
                metaBlock.lineSequence().forEach { line ->
                    val clean = line.trim().removePrefix("//").trim()
                    when {
                        clean.startsWith("@name ") || clean.startsWith("@name\t") -> {
                            val v = clean.substringAfter("@name").trim()
                            if (v.isNotEmpty()) name = v
                        }
                        clean.startsWith("@description ") || clean.startsWith("@description\t") -> {
                            val v = clean.substringAfter("@description").trim()
                            if (v.isNotEmpty()) description = v
                        }
                        clean.startsWith("@version ") || clean.startsWith("@version\t") -> {
                            val v = clean.substringAfter("@version").trim()
                            if (v.isNotEmpty()) version = v
                        }
                        clean.startsWith("@match ") || clean.startsWith("@match\t") -> {
                            val v = clean.substringAfter("@match").trim()
                            if (v.isNotEmpty()) matches.add(v)
                        }
                        clean.startsWith("@include ") || clean.startsWith("@include\t") -> {
                            val v = clean.substringAfter("@include").trim()
                            if (v.isNotEmpty()) matches.add(v)
                        }
                    }
                }
            }

            return BrowserExtensionEntity(
                id = "user_${UUID.randomUUID().toString().take(8)}",
                name = name.take(60),
                description = description.take(180),
                version = version.take(20),
                matchPatterns = if (matches.isEmpty()) "*" else matches.distinct().joinToString(", "),
                jsCode = trimmed,
                cssCode = "",
                enabled = true,
                isBuiltIn = false,
                iconKey = "code"
            )
        }

        fun isExternalCodeRef(code: String): Boolean = code.startsWith(EXTERNAL_FILE_PREFIX)

        fun resolveCode(code: String, scriptsDir: File?): String {
            if (!isExternalCodeRef(code)) return code
            val dir = scriptsDir ?: return ""
            val fileName = code.removePrefix(EXTERNAL_FILE_PREFIX).trim()
            if (fileName.isEmpty() || fileName.contains("/") || fileName.contains("..")) return ""
            val target = File(dir, fileName)
            return if (target.isFile) runCatching { target.readText(Charsets.UTF_8) }.getOrDefault("") else ""
        }

        /**
         * Stores very large JS/CSS payloads in [scriptsDir] to keep Room/SQLite rows safely
         * below Android's ~2MB CursorWindow limit.
         */
        fun externalizeLargeCodeIfNeeded(
            extension: BrowserExtensionEntity,
            scriptsDir: File
        ): BrowserExtensionEntity {
            val safeId = extension.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            var updatedJs = extension.jsCode
            var updatedCss = extension.cssCode

            if (!isExternalCodeRef(updatedJs) && updatedJs.length > DB_INLINE_CODE_LIMIT) {
                scriptsDir.mkdirs()
                val jsFileName = "${safeId}.js"
                File(scriptsDir, jsFileName).writeText(updatedJs, Charsets.UTF_8)
                updatedJs = "$EXTERNAL_FILE_PREFIX$jsFileName"
            }

            if (!isExternalCodeRef(updatedCss) && updatedCss.length > DB_INLINE_CODE_LIMIT) {
                scriptsDir.mkdirs()
                val cssFileName = "${safeId}.css"
                File(scriptsDir, cssFileName).writeText(updatedCss, Charsets.UTF_8)
                updatedCss = "$EXTERNAL_FILE_PREFIX$cssFileName"
            }

            return extension.copy(jsCode = updatedJs, cssCode = updatedCss)
        }

        fun deleteExternalCodeFiles(extensionId: String, scriptsDir: File) {
            val safeId = extensionId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            runCatching { File(scriptsDir, "${safeId}.js").delete() }
            runCatching { File(scriptsDir, "${safeId}.css").delete() }
        }

        /**
         * Two-pass streaming CRX/ZIP parser backed by a [File] on disk.
         * Supports arbitrarily large (25MB+) extensions by reading only `manifest.json`,
         * `_locales/<locale>/messages.json`, and the exact `content_scripts` JS/CSS files referenced
         * by the manifest without loading non-script assets or the full archive into RAM.
         */
        fun parseCrxOrZipFile(
            file: File,
            fallbackName: String = "インポート拡張機能",
            customId: String? = null,
            iconKey: String = "extension"
        ): BrowserExtensionEntity {
            // Pass 1: Read only manifest.json and _locales/*/messages.json
            val metaEntries = linkedMapOf<String, String>()
            ChromeWebStoreDownloader.openZipStreamFromCrxFile(file).use { zipRawStream ->
                ZipInputStream(zipRawStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val normalized = entry.name.trimStart('/')
                            val isManifest = normalized == "manifest.json" || normalized.endsWith("/manifest.json")
                            val isLocale = normalized.contains("_locales/") && normalized.endsWith("/messages.json")
                            if (isManifest || isLocale) {
                                metaEntries[normalized] = readStreamBoundedText(zis, MAX_LOCALE_BYTES)
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }

            val manifestEntry = metaEntries.entries.firstOrNull { (path, _) ->
                path == "manifest.json" || path.endsWith("/manifest.json")
            } ?: throw IllegalArgumentException("パッケージ内に manifest.json が見つかりませんでした")

            val basePrefix = manifestEntry.key.substringBeforeLast("manifest.json", "")
            val manifest = JSONObject(manifestEntry.value)
            val defaultLocale = manifest.optString("default_locale", "").trim()
            val localeMessages = loadLocaleMessages(metaEntries, basePrefix, defaultLocale)

            val resolvedName = resolveManifestMessage(manifest.optString("name", "").trim(), localeMessages)
            val name = if (resolvedName.isBlank() || resolvedName.startsWith("__MSG_")) {
                fallbackName.removeSuffix(".zip").removeSuffix(".crx").ifBlank { "インポート拡張機能" }
            } else {
                resolvedName
            }
            val resolvedDesc = resolveManifestMessage(manifest.optString("description", "").trim(), localeMessages)
            val description = if (resolvedDesc.isBlank() || resolvedDesc.startsWith("__MSG_")) {
                "Chrome 拡張機能（manifest.json）からインポート"
            } else {
                resolvedDesc
            }
            val version = manifest.optString("version", "1.0.0").ifBlank { "1.0.0" }

            val matches = linkedSetOf<String>()
            val orderedJsRefs = mutableListOf<String>()
            val orderedCssRefs = mutableListOf<String>()
            val neededPaths = hashSetOf<String>()

            val contentScripts = manifest.optJSONArray("content_scripts")
            if (contentScripts != null) {
                for (i in 0 until contentScripts.length()) {
                    val cs = contentScripts.optJSONObject(i) ?: continue
                    val matchArr = cs.optJSONArray("matches")
                    if (matchArr != null) {
                        for (m in 0 until matchArr.length()) {
                            val pat = matchArr.optString(m).trim()
                            if (pat.isNotEmpty()) matches.add(pat)
                        }
                    }
                    val jsArr = cs.optJSONArray("js")
                    if (jsArr != null) {
                        for (j in 0 until jsArr.length()) {
                            val rel = normalizeRelativeAssetPath(jsArr.optString(j))
                            if (rel.isNotEmpty()) {
                                orderedJsRefs.add(rel)
                                neededPaths.add("$basePrefix$rel")
                                neededPaths.add(rel)
                            }
                        }
                    }
                    val cssArr = cs.optJSONArray("css")
                    if (cssArr != null) {
                        for (c in 0 until cssArr.length()) {
                            val rel = normalizeRelativeAssetPath(cssArr.optString(c))
                            if (rel.isNotEmpty()) {
                                orderedCssRefs.add(rel)
                                neededPaths.add("$basePrefix$rel")
                                neededPaths.add(rel)
                            }
                        }
                    }
                }
            }

            if (neededPaths.isEmpty()) {
                throw IllegalArgumentException(
                    "この拡張機能には Web ページへ直接適用する content_scripts（JS / CSS）が含まれていないため、モバイル WebView では実行できません"
                )
            }

            // Pass 2: Extract ONLY the referenced content_scripts JS/CSS files
            val scriptContents = hashMapOf<String, String>()
            ChromeWebStoreDownloader.openZipStreamFromCrxFile(file).use { zipRawStream ->
                ZipInputStream(zipRawStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val normalized = entry.name.trimStart('/')
                            if (normalized in neededPaths) {
                                scriptContents[normalized] = readStreamBoundedText(zis, MAX_ENTRY_BYTES)
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }

            val jsBuilder = StringBuilder()
            orderedJsRefs.forEach { rel ->
                val content = scriptContents["$basePrefix$rel"] ?: scriptContents[rel]
                if (!content.isNullOrBlank()) {
                    jsBuilder.append(content).append("\n")
                }
            }
            val cssBuilder = StringBuilder()
            orderedCssRefs.forEach { rel ->
                val content = scriptContents["$basePrefix$rel"] ?: scriptContents[rel]
                if (!content.isNullOrBlank()) {
                    cssBuilder.append(content).append("\n")
                }
            }

            if (jsBuilder.isBlank() && cssBuilder.isBlank()) {
                throw IllegalArgumentException(
                    "この拡張機能には Web ページへ直接適用する content_scripts（JS / CSS）が含まれていないため、モバイル WebView では実行できません"
                )
            }

            return BrowserExtensionEntity(
                id = customId ?: "zip_${UUID.randomUUID().toString().take(8)}",
                name = name.take(60),
                description = description.take(180),
                version = version.take(20),
                matchPatterns = if (matches.isEmpty()) "*" else matches.joinToString(", "),
                jsCode = jsBuilder.toString().trim(),
                cssCode = cssBuilder.toString().trim(),
                enabled = true,
                isBuiltIn = false,
                iconKey = iconKey
            )
        }

        private fun readStreamBoundedText(input: InputStream, maxBytes: Int): String {
            val baos = ByteArrayOutputStream()
            val buf = ByteArray(16 * 1024)
            var total = 0
            var read = input.read(buf)
            while (read > 0 && total < maxBytes) {
                val allowed = minOf(read, maxBytes - total)
                baos.write(buf, 0, allowed)
                total += allowed
                read = input.read(buf)
            }
            return baos.toString(Charsets.UTF_8.name())
        }

        fun parseCrxOrZipExtension(
            rawBytes: ByteArray,
            fallbackName: String = "インポート拡張機能",
            customId: String? = null,
            iconKey: String = "extension"
        ): BrowserExtensionEntity {
            val zipBytes = ChromeWebStoreDownloader.extractZipBytesFromCrx(rawBytes)
            return parseZipExtension(
                inputStream = zipBytes.inputStream(),
                fallbackName = fallbackName,
                customId = customId,
                iconKey = iconKey
            )
        }

        fun parseZipExtension(
            inputStream: InputStream,
            fallbackName: String = "インポート拡張機能",
            customId: String? = null,
            iconKey: String = "extension"
        ): BrowserExtensionEntity {
            val entries = linkedMapOf<String, String>()
            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val normalized = entry.name.trimStart('/')
                        if (normalized.endsWith(".json", ignoreCase = true) ||
                            normalized.endsWith(".js", ignoreCase = true) ||
                            normalized.endsWith(".css", ignoreCase = true)
                        ) {
                            val baos = ByteArrayOutputStream()
                            val buf = ByteArray(4096)
                            var total = 0
                            var read = zis.read(buf)
                            while (read > 0 && total < MAX_ENTRY_BYTES) {
                                val allowed = minOf(read, MAX_ENTRY_BYTES - total)
                                baos.write(buf, 0, allowed)
                                total += allowed
                                read = zis.read(buf)
                            }
                            entries[normalized] = baos.toString(Charsets.UTF_8.name())
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            val manifestEntry = entries.entries.firstOrNull { (path, _) ->
                path == "manifest.json" || path.endsWith("/manifest.json")
            } ?: throw IllegalArgumentException("パッケージ内に manifest.json が見つかりませんでした")

            val basePrefix = manifestEntry.key.substringBeforeLast("manifest.json", "")
            val manifest = JSONObject(manifestEntry.value)
            val defaultLocale = manifest.optString("default_locale", "").trim()
            val localeMessages = loadLocaleMessages(entries, basePrefix, defaultLocale)

            val resolvedName = resolveManifestMessage(manifest.optString("name", "").trim(), localeMessages)
            val name = if (resolvedName.isBlank() || resolvedName.startsWith("__MSG_")) {
                fallbackName.removeSuffix(".zip").removeSuffix(".crx").ifBlank { "インポート拡張機能" }
            } else {
                resolvedName
            }
            val resolvedDesc = resolveManifestMessage(manifest.optString("description", "").trim(), localeMessages)
            val description = if (resolvedDesc.isBlank() || resolvedDesc.startsWith("__MSG_")) {
                "Chrome 拡張機能（manifest.json）からインポート"
            } else {
                resolvedDesc
            }
            val version = manifest.optString("version", "1.0.0").ifBlank { "1.0.0" }

            val matches = linkedSetOf<String>()
            val jsBuilder = StringBuilder()
            val cssBuilder = StringBuilder()

            val contentScripts = manifest.optJSONArray("content_scripts")
            if (contentScripts != null) {
                for (i in 0 until contentScripts.length()) {
                    val cs = contentScripts.optJSONObject(i) ?: continue
                    val matchArr = cs.optJSONArray("matches")
                    if (matchArr != null) {
                        for (m in 0 until matchArr.length()) {
                            val pat = matchArr.optString(m).trim()
                            if (pat.isNotEmpty()) matches.add(pat)
                        }
                    }
                    val jsArr = cs.optJSONArray("js")
                    if (jsArr != null) {
                        for (j in 0 until jsArr.length()) {
                            val rel = normalizeRelativeAssetPath(jsArr.optString(j))
                            val content = entries["$basePrefix$rel"] ?: entries[rel]
                            if (!content.isNullOrBlank()) {
                                jsBuilder.append(content).append("\n")
                            }
                        }
                    }
                    val cssArr = cs.optJSONArray("css")
                    if (cssArr != null) {
                        for (c in 0 until cssArr.length()) {
                            val rel = normalizeRelativeAssetPath(cssArr.optString(c))
                            val content = entries["$basePrefix$rel"] ?: entries[rel]
                            if (!content.isNullOrBlank()) {
                                cssBuilder.append(content).append("\n")
                            }
                        }
                    }
                }
            }

            if (jsBuilder.isBlank() && cssBuilder.isBlank()) {
                throw IllegalArgumentException(
                    "この拡張機能には Web ページへ直接適用する content_scripts（JS / CSS）が含まれていないため、モバイル WebView では実行できません"
                )
            }

            return BrowserExtensionEntity(
                id = customId ?: "zip_${UUID.randomUUID().toString().take(8)}",
                name = name.take(60),
                description = description.take(180),
                version = version.take(20),
                matchPatterns = if (matches.isEmpty()) "*" else matches.joinToString(", "),
                jsCode = jsBuilder.toString().trim(),
                cssCode = cssBuilder.toString().trim(),
                enabled = true,
                isBuiltIn = false,
                iconKey = iconKey
            )
        }

        private fun normalizeRelativeAssetPath(raw: String): String {
            return raw.trim().removePrefix("./").trimStart('/')
        }

        private fun loadLocaleMessages(
            entries: Map<String, String>,
            basePrefix: String,
            defaultLocale: String
        ): Map<String, String> {
            val candidates = buildList {
                add("${basePrefix}_locales/ja/messages.json")
                add("${basePrefix}_locales/ja_JP/messages.json")
                if (defaultLocale.isNotBlank()) {
                    add("${basePrefix}_locales/$defaultLocale/messages.json")
                }
                add("${basePrefix}_locales/en/messages.json")
                add("${basePrefix}_locales/en_US/messages.json")
            }

            val orderedJson = candidates.mapNotNull { entries[it] } +
                entries.entries
                    .filter { (path, _) -> path.contains("_locales/") && path.endsWith("/messages.json") }
                    .map { it.value }

            val resolved = linkedMapOf<String, String>()
            for (jsonStr in orderedJson) {
                runCatching {
                    val obj = JSONObject(jsonStr)
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val lowerKey = key.lowercase()
                        if (lowerKey !in resolved) {
                            val msg = obj.optJSONObject(key)?.optString("message", "").orEmpty().trim()
                            if (msg.isNotEmpty()) {
                                resolved[lowerKey] = msg
                            }
                        }
                    }
                }
            }
            return resolved
        }

        private fun resolveManifestMessage(raw: String, messages: Map<String, String>): String {
            if (!raw.contains("__MSG_")) return raw
            return Regex("__MSG_([A-Za-z0-9_@]+)__").replace(raw) { match ->
                val key = match.groupValues[1].lowercase()
                messages[key] ?: match.value
            }
        }

        val builtInExtensions: List<BrowserExtensionEntity> = listOf(
            BrowserExtensionEntity(
                id = "builtin_dark_reader",
                name = "ダークリーダー",
                description = "白いWebサイトを自動で目に優しいダーク配色へ変換します（画像・動画の色は保持）",
                version = "1.2.0",
                matchPatterns = "*",
                cssCode = """
                    html {
                        filter: invert(0.92) hue-rotate(180deg) !important;
                        background-color: #121212 !important;
                    }
                    img, video, picture, canvas, iframe, svg image {
                        filter: invert(1) hue-rotate(180deg) !important;
                    }
                """.trimIndent(),
                enabled = false,
                isBuiltIn = true,
                iconKey = "dark_mode",
                createdAt = 6000L
            ),
            BrowserExtensionEntity(
                id = "builtin_video_speed",
                name = "動画再生速度コントローラー",
                description = "動画ページに再生速度切替バッジ（0.75x〜2.0x）を表示して倍速再生できます",
                version = "1.1.0",
                matchPatterns = "*",
                jsCode = """
                    (function() {
                        var speeds = [0.75, 1.0, 1.25, 1.5, 2.0];
                        function ensureBar() {
                            var videos = document.querySelectorAll('video');
                            if (!videos || videos.length === 0) return;
                            if (document.getElementById('aegis-vspeed-btn')) return;
                            var idx = 1;
                            var btn = document.createElement('button');
                            btn.id = 'aegis-vspeed-btn';
                            btn.textContent = '1.0x';
                            btn.style.cssText = 'position:fixed;right:14px;bottom:86px;z-index:2147483647;background:rgba(16,24,32,0.88);color:#34d399;border:1px solid rgba(52,211,153,0.6);border-radius:999px;padding:6px 12px;font-size:12px;font-weight:700;font-family:sans-serif;box-shadow:0 4px 12px rgba(0,0,0,0.35);';
                            btn.addEventListener('click', function(e) {
                                e.stopPropagation();
                                idx = (idx + 1) % speeds.length;
                                var rate = speeds[idx];
                                document.querySelectorAll('video').forEach(function(v) { v.playbackRate = rate; });
                                btn.textContent = rate.toFixed(2).replace(/\.00$/, '.0') + 'x';
                            });
                            document.body.appendChild(btn);
                        }
                        ensureBar();
                        setInterval(ensureBar, 2500);
                    })();
                """.trimIndent(),
                enabled = false,
                isBuiltIn = true,
                iconKey = "speed",
                createdAt = 5000L
            ),
            BrowserExtensionEntity(
                id = "builtin_unlock_copy",
                name = "コピー・選択制限解除",
                description = "テキスト選択や長押しコピーが禁止されたサイトで選択・コピーを可能にします",
                version = "1.0.0",
                matchPatterns = "*",
                cssCode = """
                    *, *::before, *::after {
                        -webkit-user-select: text !important;
                        user-select: text !important;
                        -webkit-touch-callout: default !important;
                    }
                """.trimIndent(),
                jsCode = """
                    ['copy','cut','selectstart','contextmenu','mousedown'].forEach(function(evt) {
                        document.addEventListener(evt, function(e) { e.stopPropagation(); }, true);
                    });
                """.trimIndent(),
                enabled = false,
                isBuiltIn = true,
                iconKey = "copy",
                createdAt = 4000L
            ),
            BrowserExtensionEntity(
                id = "builtin_cookie_banner",
                name = "Cookie同意バナー非表示",
                description = "画面下部や中央を覆うCookie承諾ポップアップを自動で非表示にします",
                version = "1.0.0",
                matchPatterns = "*",
                cssCode = """
                    [id*="cookie-banner"], [class*="cookie-banner"],
                    [id*="cookie-consent"], [class*="cookie-consent"],
                    [id*="onetrust-banner"], #CybotCookiebotDialog,
                    .cc-window, .gdpr-banner, [aria-label*="cookie" i] {
                        display: none !important;
                        visibility: hidden !important;
                        pointer-events: none !important;
                    }
                """.trimIndent(),
                enabled = true,
                isBuiltIn = true,
                iconKey = "cookie",
                createdAt = 3000L
            ),
            BrowserExtensionEntity(
                id = "builtin_scroll_top",
                name = "ページ先頭へ戻るボタン",
                description = "長いページをスクロールすると右下にワンタップで先頭へ戻れるボタンを表示します",
                version = "1.0.0",
                matchPatterns = "*",
                jsCode = """
                    (function() {
                        if (document.getElementById('aegis-top-btn')) return;
                        var btn = document.createElement('button');
                        btn.id = 'aegis-top-btn';
                        btn.textContent = '↑';
                        btn.setAttribute('aria-label', '先頭へ戻る');
                        btn.style.cssText = 'position:fixed;right:14px;bottom:24px;z-index:2147483646;width:42px;height:42px;border-radius:50%;background:rgba(16,24,32,0.85);color:#fff;border:1px solid rgba(255,255,255,0.25);font-size:18px;font-weight:700;display:none;align-items:center;justify-content:center;box-shadow:0 4px 12px rgba(0,0,0,0.3);';
                        btn.addEventListener('click', function() {
                            window.scrollTo({ top: 0, behavior: 'smooth' });
                        });
                        document.body.appendChild(btn);
                        window.addEventListener('scroll', function() {
                            btn.style.display = window.scrollY > 360 ? 'flex' : 'none';
                        }, { passive: true });
                    })();
                """.trimIndent(),
                enabled = false,
                isBuiltIn = true,
                iconKey = "arrow_up",
                createdAt = 2000L
            ),
            BrowserExtensionEntity(
                id = "builtin_readable_font",
                name = "記事の文字サイズ拡大",
                description = "ニュースやブログ記事の本文文字を読みやすいサイズと行間に整えます",
                version = "1.0.0",
                matchPatterns = "*",
                cssCode = """
                    article p, main p, .article p, .post-content p {
                        font-size: 115% !important;
                        line-height: 1.85 !important;
                    }
                """.trimIndent(),
                enabled = false,
                isBuiltIn = true,
                iconKey = "text_size",
                createdAt = 1000L
            )
        )
    }
}
