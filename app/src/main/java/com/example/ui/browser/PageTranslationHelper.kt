package com.example.ui.browser

import android.net.Uri
import java.net.URLEncoder
import java.util.Locale

data class TranslationLanguage(
    val code: String,
    val label: String,
    val nativeLabel: String
)

object PageTranslationHelper {

    val supportedLanguages: List<TranslationLanguage> = listOf(
        TranslationLanguage("ja", "日本語", "Japanese"),
        TranslationLanguage("en", "英語", "English"),
        TranslationLanguage("zh-CN", "中国語（簡体）", "简体中文"),
        TranslationLanguage("zh-TW", "中国語（繁体）", "繁體中文"),
        TranslationLanguage("ko", "韓国語", "한국어"),
        TranslationLanguage("es", "スペイン語", "Español"),
        TranslationLanguage("fr", "フランス語", "Français"),
        TranslationLanguage("de", "ドイツ語", "Deutsch")
    )

    fun isTranslatedUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val host = uri.host?.lowercase(Locale.ROOT) ?: return false
        val path = uri.path?.lowercase(Locale.ROOT).orEmpty()
        return host.endsWith(".translate.goog") ||
            (host == "translate.google.com" && path.startsWith("/translate"))
    }

    fun extractOriginalUrl(url: String): String? {
        if (url.isBlank()) return null
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
        val host = uri.host?.lowercase(Locale.ROOT) ?: return null

        if (host == "translate.google.com" && (uri.path ?: "").startsWith("/translate")) {
            return uri.getQueryParameter("u")?.takeIf { it.isNotBlank() }
        }

        if (host.endsWith(".translate.goog")) {
            val encodedHost = host.removeSuffix(".translate.goog")
            // Google Translate proxy encodes "." as "-" and original "-" as "--"
            val decodedHost = encodedHost
                .replace("--", "\u0000")
                .replace("-", ".")
                .replace("\u0000", "-")
            val builder = Uri.Builder()
                .scheme("https")
                .encodedAuthority(decodedHost)
                .encodedPath(uri.encodedPath)

            uri.queryParameterNames
                .filterNot { it.startsWith("_x_tr_") }
                .forEach { key ->
                    uri.getQueryParameters(key).forEach { value ->
                        builder.appendQueryParameter(key, value)
                    }
                }
            if (!uri.encodedFragment.isNullOrBlank()) {
                builder.encodedFragment(uri.encodedFragment)
            }
            return builder.build().toString()
        }

        return null
    }

    fun buildTranslateUrl(pageUrl: String, targetLangCode: String): String {
        val trimmed = pageUrl.trim()
        if (trimmed.isBlank()) return ""
        val sourceUrl = extractOriginalUrl(trimmed) ?: trimmed
        val encodedUrl = URLEncoder.encode(sourceUrl, "UTF-8")
        val encodedLang = URLEncoder.encode(targetLangCode, "UTF-8")
        return "https://translate.google.com/translate?sl=auto&tl=$encodedLang&u=$encodedUrl"
    }
}
