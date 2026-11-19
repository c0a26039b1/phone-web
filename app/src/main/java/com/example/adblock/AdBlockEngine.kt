package com.example.adblock

import android.net.Uri
import android.webkit.WebResourceResponse
import com.example.data.local.SearchEngine
import java.io.ByteArrayInputStream
import java.net.URLEncoder
import java.util.Locale

class AdBlockEngine {

    @Volatile
    private var config: ShieldConfig = ShieldConfig()

    fun updateConfig(newConfig: ShieldConfig) {
        config = newConfig
    }

    fun getCurrentConfig(): ShieldConfig = config

    fun isSiteProtected(pageHost: String): Boolean {
        val current = config
        if (!current.adBlockEnabled) return false
        if (pageHost.isBlank()) return true
        return !matchesDomainSet(pageHost.lowercase(Locale.ROOT), current.allowlistedDomains)
    }

    fun evaluateRequest(
        requestUrl: String,
        pageHost: String,
        isMainFrame: Boolean
    ): BlockDecision {
        val current = config
        if (!current.adBlockEnabled) return BlockDecision(false)

        val normalizedPageHost = pageHost.lowercase(Locale.ROOT)
        if (normalizedPageHost.isNotBlank() && matchesDomainSet(normalizedPageHost, current.allowlistedDomains)) {
            return BlockDecision(false)
        }

        val uri = runCatching { Uri.parse(requestUrl) }.getOrNull() ?: return BlockDecision(false)
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return BlockDecision(false)
        if (scheme != "http" && scheme != "https") return BlockDecision(false)

        val host = uri.host?.lowercase(Locale.ROOT)?.removePrefix("www.") ?: return BlockDecision(false)
        if (host.isBlank()) return BlockDecision(false)

        // Custom rules always apply (even on main frame if user explicitly blocked a domain)
        if (current.customBlockedDomains.isNotEmpty()) {
            val matchedCustom = findMatchedDomain(host, current.customBlockedDomains)
            if (matchedCustom != null) {
                return BlockDecision(true, BlockCategory.CUSTOM, matchedCustom)
            }
        }

        // Popup / malicious redirect hosts can be blocked even on main frame navigation
        if (current.blockPopups) {
            val matchedPopup = findMatchedDomain(host, POPUP_AND_MALWARE_DOMAINS)
            if (matchedPopup != null) {
                return BlockDecision(true, BlockCategory.POPUP, matchedPopup)
            }
        }

        // Never block standard main document pages for ads/trackers so user navigation stays reliable
        if (isMainFrame) return BlockDecision(false)

        if (current.blockAds) {
            val matchedAd = findMatchedDomain(host, AD_DOMAINS)
            if (matchedAd != null) {
                return BlockDecision(true, BlockCategory.AD, matchedAd)
            }
        }

        if (current.blockTrackers) {
            val matchedTracker = findMatchedDomain(host, TRACKER_DOMAINS)
            if (matchedTracker != null) {
                return BlockDecision(true, BlockCategory.TRACKER, matchedTracker)
            }
        }

        if (current.blockSocial) {
            val matchedSocial = findMatchedDomain(host, SOCIAL_TRACKER_DOMAINS)
            if (matchedSocial != null && !isSameRegistrableDomain(host, normalizedPageHost)) {
                return BlockDecision(true, BlockCategory.SOCIAL, matchedSocial)
            }
        }

        // URL path heuristics for third-party ad scripts & banners
        if (current.blockAds) {
            val lowerPathAndQuery = ((uri.encodedPath ?: "") + "?" + (uri.encodedQuery ?: ""))
                .lowercase(Locale.ROOT)
            if (AD_URL_PATTERNS.any { lowerPathAndQuery.contains(it) }) {
                return BlockDecision(true, BlockCategory.AD, host)
            }
        }

        if (current.blockTrackers) {
            val lowerPath = (uri.encodedPath ?: "").lowercase(Locale.ROOT)
            if (TRACKER_PATH_PATTERNS.any { lowerPath.contains(it) }) {
                return BlockDecision(true, BlockCategory.TRACKER, host)
            }
        }

        return BlockDecision(false)
    }

    fun shouldBlockPopupUrl(url: String, currentPageHost: String): Boolean {
        val current = config
        if (!current.adBlockEnabled || !current.blockPopups) return false
        if (currentPageHost.isNotBlank() && matchesDomainSet(currentPageHost.lowercase(Locale.ROOT), current.allowlistedDomains)) {
            return false
        }
        val host = extractHost(url)
        if (host.isBlank()) return false
        return matchesDomainSet(host, POPUP_AND_MALWARE_DOMAINS) || matchesDomainSet(host, AD_DOMAINS)
    }

    fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "utf-8",
            200,
            "Blocked by Aegis",
            mapOf("Cache-Control" to "no-store"),
            ByteArrayInputStream(ByteArray(0))
        )
    }

    fun buildCosmeticFilterScript(): String {
        val css = COSMETIC_HIDE_SELECTORS.joinToString(",") +
            "{display:none!important;visibility:hidden!important;height:0!important;min-height:0!important;max-height:0!important;margin:0!important;padding:0!important;overflow:hidden!important;pointer-events:none!important;border:none!important;}"
        val escapedCss = css.replace("\\", "\\\\").replace("'", "\\'")
        return """
            (function() {
              try {
                var styleId = 'aegis-cosmetic-filter';
                if (!document.getElementById(styleId)) {
                  var style = document.createElement('style');
                  style.id = styleId;
                  style.type = 'text/css';
                  style.appendChild(document.createTextNode('$escapedCss'));
                  (document.head || document.documentElement).appendChild(style);
                }

                var host = (location.hostname || '').toLowerCase();
                var isSearchEngine = /(^|\.)google\./.test(host) ||
                  host.indexOf('search.yahoo.') !== -1 ||
                  host.indexOf('duckduckgo.com') !== -1 ||
                  host.indexOf('bing.com') !== -1;

                if (!isSearchEngine) return;

                var SPONSORED_RE = /^(sponsored|sponsored result|sponsored results|ad|ads|スポンサー|スポンサー結果|スポンサーリンク|広告|プロモーション)$/i;

                function isProtectedContainer(el) {
                  if (!el || el === document.body || el === document.documentElement) return true;
                  var id = (el.id || '').toLowerCase();
                  if (id === 'main' || id === 'cnt' || id === 'rcnt' || id === 'center_col' ||
                      id === 'search' || id === 'rso' || id === 'botstuff' || id === 'taw' || id === 'appbar') {
                    return true;
                  }
                  if (el.querySelector && el.querySelector('#rso, #search, #fprs, .spell_orig, form[role="search"], input[name="q"]')) {
                    return true;
                  }
                  return false;
                }

                function hideEl(el) {
                  if (!el || isProtectedContainer(el)) return;
                  el.style.setProperty('display', 'none', 'important');
                  el.style.setProperty('visibility', 'hidden', 'important');
                  el.style.setProperty('height', '0px', 'important');
                  el.style.setProperty('margin', '0px', 'important');
                  el.style.setProperty('padding', '0px', 'important');
                }

                function findAdWrapper(el) {
                  var direct = el.closest && el.closest('#tads, #tadsb, #tvcap, #bottomads, [data-text-ad], [data-ta-slot], .uEierd, .commercial-unit-mobile-top, .commercial-unit-desktop-top, .commercial-unit-desktop-rhs, .pla-unit, .cu-container, .results--ads, .result--ad, .b_ad, .b_adTop, .b_adBottom, .sw-CardAd');
                  if (direct && !isProtectedContainer(direct)) return direct;

                  var cur = el;
                  var best = el;
                  for (var depth = 0; depth < 12 && cur && cur.parentElement; depth++) {
                    var parent = cur.parentElement;
                    if (isProtectedContainer(parent)) {
                      if (!isProtectedContainer(cur)) {
                        best = cur;
                      }
                      break;
                    }
                    if (!isProtectedContainer(cur)) {
                      best = cur;
                    }
                    cur = parent;
                  }
                  return best;
                }

                function sweepSearchAds() {
                  // 1. Known search ad slots & click-tracking ad links
                  var known = document.querySelectorAll('#tads, #tadsb, #tvcap, #bottomads, [data-text-ad], [data-ta-slot], .uEierd, .commercial-unit-mobile-top, .commercial-unit-desktop-top, .commercial-unit-desktop-rhs, .pla-unit, .cu-container, [data-testid="ad"], .results--ads, .result--ad, .b_ad, .b_adTop, .b_adBottom, .sw-CardAd, div[aria-label="Ads"], div[aria-label="Sponsored"], div[aria-label="Sponsored Result"], div[aria-label="Sponsored Results"], div[aria-label="広告"], div[aria-label="スポンサー"]');
                  for (var i = 0; i < known.length; i++) {
                    hideEl(known[i]);
                  }

                  // 2. Google ad click links (/aclk? or googleadservices)
                  var adLinks = document.querySelectorAll('a[href*="/aclk?"], a[data-rw*="/aclk"], a[href*="googleadservices.com"]');
                  for (var j = 0; j < adLinks.length; j++) {
                    var wrapper = findAdWrapper(adLinks[j]);
                    if (wrapper) hideEl(wrapper);
                  }

                  // 3. Text-based Sponsored Result / スポンサー headings (catches obfuscated classes & leaf divs)
                  var labels = document.querySelectorAll('h1, h2, h3, span, div[role="heading"], div[aria-level], div');
                  for (var k = 0; k < labels.length; k++) {
                    var node = labels[k];
                    if (node.childElementCount > 1) continue;
                    var txt = (node.textContent || '').replace(/[\u200B-\u200D\uFEFF]/g, '').replace(/\s+/g, ' ').trim();
                    if (txt.length > 0 && txt.length <= 24 && SPONSORED_RE.test(txt)) {
                      var block = findAdWrapper(node);
                      if (block) hideEl(block);
                    }
                  }
                }

                sweepSearchAds();

                if (!window.__aegisSearchAdObserver) {
                  var timer = null;
                  var obs = new MutationObserver(function() {
                    if (timer) return;
                    timer = setTimeout(function() {
                      timer = null;
                      sweepSearchAds();
                    }, 80);
                  });
                  obs.observe(document.documentElement || document.body, {
                    childList: true,
                    subtree: true
                  });
                  window.__aegisSearchAdObserver = obs;
                }
              } catch (e) {}
            })();
        """.trimIndent()
    }

    private fun matchesDomainSet(host: String, domainSet: Set<String>): Boolean =
        findMatchedDomain(host, domainSet) != null

    private fun findMatchedDomain(host: String, domainSet: Set<String>): String? {
        if (domainSet.isEmpty()) return null
        val cleanHost = host.removePrefix("www.")
        if (domainSet.contains(cleanHost)) return cleanHost

        var dotIndex = cleanHost.indexOf('.')
        while (dotIndex != -1 && dotIndex < cleanHost.length - 1) {
            val suffix = cleanHost.substring(dotIndex + 1)
            if (domainSet.contains(suffix)) {
                return suffix
            }
            dotIndex = cleanHost.indexOf('.', dotIndex + 1)
        }
        return null
    }

    private fun isSameRegistrableDomain(hostA: String, hostB: String): Boolean {
        if (hostA.isBlank() || hostB.isBlank()) return false
        val cleanA = hostA.removePrefix("www.")
        val cleanB = hostB.removePrefix("www.")
        if (cleanA == cleanB) return true
        val partsA = cleanA.split('.')
        val partsB = cleanB.split('.')
        if (partsA.size < 2 || partsB.size < 2) return false
        val rootA = partsA.takeLast(2).joinToString(".")
        val rootB = partsB.takeLast(2).joinToString(".")
        return rootA == rootB
    }

    companion object {
        fun extractHost(url: String?): String {
            if (url.isNullOrBlank()) return ""
            return runCatching {
                Uri.parse(url).host?.lowercase(Locale.ROOT)?.removePrefix("www.") ?: ""
            }.getOrDefault("")
        }

        fun isSearchUrl(url: String?): Boolean {
            if (url.isNullOrBlank()) return false
            val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
            val host = uri.host?.lowercase(Locale.ROOT)?.removePrefix("www.") ?: return false
            val path = uri.path?.lowercase(Locale.ROOT).orEmpty()
            val query = uri.query?.lowercase(Locale.ROOT).orEmpty()

            return when {
                host == "duckduckgo.com" || host.endsWith(".duckduckgo.com") -> true
                host == "search.yahoo.co.jp" || host == "search.yahoo.com" -> true
                host == "startpage.com" || host.endsWith(".startpage.com") -> true
                host == "bing.com" || host.endsWith(".bing.com") -> path.startsWith("/search") || query.contains("q=")
                Regex("^(google\\.[a-z.]+|.+\\.google\\.[a-z.]+)$").matches(host) ->
                    path.isBlank() || path == "/" || path.startsWith("/search") || query.contains("q=")
                else -> false
            }
        }

        fun sanitizeDomainInput(raw: String): String {
            val trimmed = raw.trim().lowercase(Locale.ROOT)
            if (trimmed.isBlank()) return ""
            val withScheme = if (trimmed.contains("://")) trimmed else "https://$trimmed"
            val host = extractHost(withScheme)
            return host.ifBlank {
                trimmed
                    .removePrefix("http://")
                    .removePrefix("https://")
                    .removePrefix("www.")
                    .substringBefore("/")
                    .trim()
            }
        }

        fun normalizeInputToUrl(
            input: String,
            searchEngine: SearchEngine,
            httpsUpgrade: Boolean
        ): String {
            val trimmed = input.trim()
            if (trimmed.isEmpty()) return ""

            val lower = trimmed.lowercase(Locale.ROOT)
            if (lower.startsWith("http://")) {
                return if (httpsUpgrade) "https://${trimmed.substring(7)}" else trimmed
            }
            if (lower.startsWith("https://") || lower.startsWith("about:")) {
                return trimmed
            }

            // Detect domain-like inputs (no spaces, valid dot TLD or localhost)
            val looksLikeHost = !trimmed.contains(" ") && (
                trimmed.startsWith("localhost", ignoreCase = true) ||
                    Regex("^[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)+(:\\d+)?(/.*)?$").matches(trimmed)
                )

            if (looksLikeHost) {
                return "https://$trimmed"
            }

            val encoded = URLEncoder.encode(trimmed, "UTF-8")
            return String.format(Locale.ROOT, searchEngine.queryUrlTemplate, encoded)
        }

        private val AD_DOMAINS: Set<String> = setOf(
            // Global Ad Networks & Exchanges
            "doubleclick.net",
            "googlesyndication.com",
            "googleadservices.com",
            "adservice.google.com",
            "adservice.google.co.jp",
            "pagead2.googlesyndication.com",
            "tpc.googlesyndication.com",
            "fundingchoicesmessages.google.com",
            "adnxs.com",
            "adnxs-simple.com",
            "amazon-adsystem.com",
            "aax.amazon-adsystem.com",
            "criteo.com",
            "criteo.net",
            "taboola.com",
            "outbrain.com",
            "outbrainimg.com",
            "pubmatic.com",
            "rubiconproject.com",
            "openx.net",
            "casalemedia.com",
            "indexww.com",
            "smartadserver.com",
            "adform.net",
            "adroll.com",
            "advertising.com",
            "adtech.de",
            "adtechus.com",
            "adsrvr.org",
            "bidswitch.net",
            "contextweb.com",
            "gumgum.com",
            "lijit.com",
            "sovrn.com",
            "sharethrough.com",
            "teads.tv",
            "triplelift.com",
            "yieldmo.com",
            "33across.com",
            "spotxchange.com",
            "spotx.tv",
            "mediamath.com",
            "mathtag.com",
            "turn.com",
            "rlcdn.com",
            "tapad.com",
            "bluekai.com",
            "krxd.net",
            "exelator.com",
            "eyeota.net",
            "rfihub.com",
            "mgid.com",
            "revcontent.com",
            "zedo.com",
            "adblade.com",
            "adcolony.com",
            "applovin.com",
            "applvn.com",
            "chartboost.com",
            "inmobi.com",
            "ironsrc.com",
            "mopub.com",
            "tapjoy.com",
            "unityads.unity3d.com",
            "vungle.com",
            "media.net",
            "yieldlab.net",
            "improvedigital.com",
            "sonobi.com",
            "kargo.com",
            "undertone.com",
            "rhythmone.com",
            "connatix.com",
            "nativo.com",
            "polarcdn.com",
            "carbonads.net",
            "buysellads.com",
            "ads.yahoo.com",
            "ads.linkedin.com",
            "ads.pinterest.com",
            "ads.reddit.com",
            "ads.tiktok.com",
            "ads-twitter.com",
            "ads.twitter.com",
            "ad.doubleclick.net",
            "static.doubleclick.net",
            "m.doubleclick.net",
            "mediavisor.doubleclick.net",
            "moatads.com",
            "doubleverify.com",
            "adsafeprotected.com",
            "integralads.com",
            // Major Japanese Ad Networks & SSPs
            "i-mobile.co.jp",
            "spdeliver.i-mobile.co.jp",
            "microad.jp",
            "microad.net",
            "send.microad.jp",
            "cache.send.microad.jp",
            "nend.net",
            "js1.nend.net",
            "geniee.jp",
            "genieesspv.jp",
            "aladdin.genieesspv.jp",
            "fluct.jp",
            "sh.adingo.jp",
            "adingo.jp",
            "adstir.com",
            "ad-stir.com",
            "gmossp-sp.jp",
            "sp.gmossp-sp.jp",
            "im-apps.net",
            "ladsp.com",
            "fout.jp",
            "js.fout.jp",
            "yads.c.yimg.jp",
            "yads.yahoo.co.jp",
            "yads.yjtag.yahoo.co.jp",
            "a8.net",
            "px.a8.net",
            "valuecommerce.com",
            "ck.jp.ap.valuecommerce.com",
            "accesstrade.net",
            "felmat.net",
            "moshimo.com",
            "popin.cc",
            "api.popin.cc",
            "logly.co.jp",
            "l.logly.co.jp",
            "uzou.jp",
            "speee-ad.akamaized.net",
            "taxel.jp",
            "kauli.net",
            "shinobi.jp",
            "adm.shinobi.jp",
            "fam-ad.com",
            "zucks.net",
            "j.zucks.net.zimg.jp",
            "maist.jp",
            "amoad.com",
            "socdm.com",
            "ad.yieldmanager.com",
            "bypass.jp",
            "impact-ad.jp",
            "d.href.asia",
            "adcloud.jp",
            "ad-v.jp",
            "aktrack.pubmatic.com",
            "gsspcln.jp",
            "gsspat.jp",
            "adtdp.com",
            "adingo.jp.eimg.jp",
            "flux-cdn.com",
            "d-cache.microad.jp",
            "rakuten-ad.com",
            "grp09.ias.rakuten.co.jp",
            "ad.jp.ap.valuecommerce.com",
            "banner.jp",
            "adimg.net",
            "ad-nex.com",
            "ad-m.asia",
            "glossom.co.jp",
            "fivecdm.com",
            "maio.jp"
        )

        private val TRACKER_DOMAINS: Set<String> = setOf(
            "google-analytics.com",
            "ssl.google-analytics.com",
            "www.google-analytics.com",
            "googletagmanager.com",
            "analytics.google.com",
            "stats.g.doubleclick.net",
            "scorecardresearch.com",
            "quantserve.com",
            "quantcount.com",
            "hotjar.com",
            "hotjar.io",
            "mixpanel.com",
            "segment.io",
            "segment.com",
            "amplitude.com",
            "fullstory.com",
            "mouseflow.com",
            "crazyegg.com",
            "clarity.ms",
            "bat.bing.com",
            "newrelic.com",
            "nr-data.net",
            "js-agent.newrelic.com",
            "chartbeat.com",
            "chartbeat.net",
            "parsely.com",
            "optimizely.com",
            "omtrdc.net",
            "2o7.net",
            "demdex.net",
            "everesttech.net",
            "branch.io",
            "app.link",
            "appsflyer.com",
            "adjust.com",
            "kochava.com",
            "onesignal.com",
            "pushwoosh.com",
            "braze.com",
            "leanplum.com",
            "bugsnag.com",
            "sentry.io",
            "datadoghq.com",
            "heapanalytics.com",
            "kissmetrics.com",
            "hubspot.com",
            "hs-analytics.net",
            "hs-scripts.com",
            "marketo.net",
            "mktoresp.com",
            "pardot.com",
            "bounceexchange.com",
            "bkrtx.com",
            "agkn.com",
            "crwdcntrl.net",
            "dotomi.com",
            "casalemedia.com",
            "pippio.com",
            "id5-sync.com",
            "liadm.com",
            "liveramp.com",
            "narrative.io",
            "permutive.com",
            "treasuredata.com",
            "in.treasuredata.com",
            "karte.io",
            "b.karte.io",
            "usergram.info",
            "ptengine.jp",
            "ptengine.com",
            "yjtag.yahoo.co.jp",
            "b92.yahoo.co.jp",
            "b97.yahoo.co.jp",
            "s.yjtag.jp",
            "rt.r10s.jp",
            "rat.rakuten.co.jp"
        )

        private val SOCIAL_TRACKER_DOMAINS: Set<String> = setOf(
            "connect.facebook.net",
            "pixel.facebook.com",
            "an.facebook.com",
            "analytics.twitter.com",
            "static.ads-twitter.com",
            "t.co",
            "analytics.tiktok.com",
            "business-api.tiktok.com",
            "snap.licdn.com",
            "px.ads.linkedin.com",
            "ct.pinterest.com",
            "tr.snapchat.com",
            "sc-static.net",
            "tr.line.me",
            "d.line-scdn.net",
            "b.hatena.ne.jp"
        )

        private val POPUP_AND_MALWARE_DOMAINS: Set<String> = setOf(
            "popads.net",
            "popcash.net",
            "propellerads.com",
            "propellerclick.com",
            "exoclick.com",
            "exosrv.com",
            "juicyads.com",
            "trafficjunky.net",
            "trafficfactory.biz",
            "adsterra.com",
            "highperformanceformat.com",
            "clickadu.com",
            "hilltopads.net",
            "adcash.com",
            "admaven.com",
            "onclickads.net",
            "onclkds.com",
            "coinhive.com",
            "coin-hive.com",
            "authedmine.com",
            "crypto-loot.com",
            "cryptoloot.pro",
            "minero.cc",
            "webminepool.com",
            "pushengage.com",
            "pushcrew.com",
            "notifpush.com",
            "directrev.com",
            "ero-advertising.com"
        )

        private val AD_URL_PATTERNS: List<String> = listOf(
            "/pagead/",
            "/adsense/",
            "/adserver/",
            "/adservice/",
            "/ad_banner",
            "/banner_ad",
            "/popunder",
            "/prebid",
            "googlesyndication",
            "doubleclick",
            "ad_status.js",
            "show_ads.js",
            "adsbygoogle.js",
            "/gpt/pubads",
            "/imobile_ad",
            "/yads-iframe",
            "/nend_ad"
        )

        private val TRACKER_PATH_PATTERNS: List<String> = listOf(
            "/gtm.js",
            "/gtag/js",
            "/analytics.js",
            "/fbevents.js",
            "/uwt.js",
            "/bat.js",
            "/pixel.gif",
            "/tr/pixel",
            "/collect?v="
        )

        private val COSMETIC_HIDE_SELECTORS: List<String> = listOf(
            // Search Engine Sponsored Results (Google / Yahoo! JAPAN / DuckDuckGo / Bing)
            "#tads",
            "#tadsb",
            "#tvcap",
            "#bottomads",
            "[data-text-ad]",
            "[data-ta-slot]",
            ".uEierd",
            ".commercial-unit-mobile-top",
            ".commercial-unit-desktop-top",
            ".commercial-unit-desktop-rhs",
            ".pla-unit",
            ".cu-container",
            "div[aria-label='Ads']",
            "div[aria-label='Sponsored']",
            "div[aria-label='広告']",
            "div[aria-label='スポンサー']",
            "[data-testid='ad']",
            ".results--ads",
            ".result--ad",
            ".b_ad",
            ".b_adTop",
            ".b_adBottom",
            ".sw-CardAd",
            // Display & Native Ad Containers
            "ins.adsbygoogle",
            ".adsbygoogle",
            "[id^='div-gpt-ad']",
            "[id^='google_ads_iframe']",
            "[id^='google_ads_div']",
            "[data-ad-slot]",
            "[data-ad-client]",
            "[data-google-query-id]",
            ".OUTBRAIN",
            "[id^='taboola-']",
            ".taboola-widget",
            ".trc_related_container",
            ".imobile_ad",
            "[id^='imobile_ad']",
            ".nend_wrapper",
            "[id^='nend_adspace']",
            ".fluct-ad",
            "[id^='geniee_']",
            ".geniee_ad",
            ".yads_ad",
            "[id^='yads_']",
            ".logly-lift-ad",
            "[id^='logly_']",
            ".popin_recommend",
            ".uzou-widget",
            ".ad-banner",
            ".ad_banner",
            ".ad-container",
            ".ads-container",
            ".sponsored-ad",
            ".sticky-ad",
            ".floating-ad",
            ".bottom-ad-bar",
            "iframe[src*='doubleclick.net']",
            "iframe[src*='googlesyndication.com']",
            "iframe[src*='i-mobile.co.jp']",
            "iframe[src*='microad.jp']",
            "iframe[src*='nend.net']",
            "iframe[src*='adstir.com']",
            "iframe[src*='fluct.jp']",
            "iframe[src*='geniee.jp']",
            "iframe[src*='criteo.com']",
            "iframe[src*='taboola.com']",
            "iframe[src*='outbrain.com']",
            "iframe[src*='yads.c.yimg.jp']"
        )
    }
}
