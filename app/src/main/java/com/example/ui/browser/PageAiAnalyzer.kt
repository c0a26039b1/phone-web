package com.example.ui.browser

import android.webkit.WebView
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.coroutines.resume

data class ExtractedPageContent(
    val title: String,
    val url: String,
    val fullText: String,
    val headings: List<String> = emptyList(),
    val detectedQuestions: List<String> = emptyList()
)

data class SolvedProblem(
    val number: Int,
    val question: String,
    val answer: String,
    val explanation: String
)

data class PageAiAnalysisResult(
    val pageTitle: String,
    val pageUrl: String,
    val extractedCharCount: Int,
    val pageSummary: String,
    val problems: List<SolvedProblem>,
    val isPlaceholder: Boolean = false
)

sealed interface PageAiUiState {
    data object Hidden : PageAiUiState
    data class Loading(val pageTitle: String, val pageUrl: String) : PageAiUiState
    data class Success(val result: PageAiAnalysisResult) : PageAiUiState
    data class Error(val pageTitle: String, val pageUrl: String, val message: String) : PageAiUiState
}

/**
 * Single AI abstraction point for whole-page problem analysis.
 */
interface PageAiAnalyzer {
    suspend fun analyzePage(content: ExtractedPageContent): PageAiAnalysisResult
}

/**
 * Gemini-backed page problem analyzer powered by Firebase AI Logic (`Firebase.ai`).
 * Uses structured JSON output via `Schema.obj(...)` and lazy model initialization.
 */
class FirebasePageAiAnalyzer : PageAiAnalyzer {

    private val model by lazy {
        val responseSchema = Schema.obj(
            properties = mapOf(
                "pageSummary" to Schema.string(
                    description = "ページ全体の簡潔な日本語要約（2〜3文）"
                ),
                "problems" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "question" to Schema.string(
                                description = "ページ内から抽出した問題文・設問・または主要な疑問点"
                            ),
                            "answer" to Schema.string(
                                description = "その問題に対する明確で正確な回答"
                            ),
                            "explanation" to Schema.string(
                                description = "回答の根拠や解き方の簡潔な解説"
                            )
                        )
                    ),
                    description = "ページ内の問題・設問と回答・解説のリスト（最大10件）"
                )
            )
        )

        val config = generationConfig {
            responseMimeType = "application/json"
            this.responseSchema = responseSchema
            temperature = 0.2f
        }

        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = config,
            systemInstruction = content {
                text(
                    "あなたはWebページ全体のテキストを精密に読み取り、含まれる問題・クイズ・演習・設問を特定して正確に解答する学習・調査アシスタントです。" +
                        "すべて自然な日本語で簡潔かつ正確に回答してください。" +
                        "ページ内に選択問題・計算問題・記述問題がある場合は、すべての問題（最大10件）を漏れなく抽出し、結論となる回答をanswerに、根拠や計算手順をexplanationに記述してください。" +
                        "明示的な問題が見つからないページの場合は、ページ全体の中心的な疑問・ポイントを1〜3件のQ&A形式に整理してわかりやすく回答してください。"
                )
            }
        )
    }

    override suspend fun analyzePage(content: ExtractedPageContent): PageAiAnalysisResult {
        val cleanedText = content.fullText.trim()
        if (cleanedText.isBlank()) {
            throw IllegalStateException("このページから解析可能なテキストを取得できませんでした。ページの読み込み完了後に再試行してください。")
        }

        val prompt = buildPrompt(content)
        val response = withTimeout(INFERENCE_TIMEOUT_MS) {
            model.generateContent(prompt)
        }
        val jsonText = response.text
        if (jsonText.isNullOrBlank()) {
            throw IllegalStateException("AIモデルから空の応答が返されました。もう一度お試しください。")
        }

        return parseGeminiJsonResponse(
            jsonString = jsonText,
            content = content
        )
    }

    companion object {
        private const val MODEL_NAME = "gemini-3.8-flash"
        private const val INFERENCE_TIMEOUT_MS = 60_000L
        private const val MAX_PROMPT_BODY_CHARS = 18_000

        fun buildPrompt(content: ExtractedPageContent): String = buildString {
            appendLine("以下のWebページ全体の内容を分析し、含まれる問題・設問に対する正確な回答と解説をJSON形式で出力してください。")
            appendLine()
            appendLine("【ページタイトル】")
            appendLine(content.title.ifBlank { "無題" })
            appendLine()
            appendLine("【URL】")
            appendLine(content.url)
            if (content.headings.isNotEmpty()) {
                appendLine()
                appendLine("【見出し一覧】")
                content.headings.take(20).forEach { heading ->
                    appendLine("- $heading")
                }
            }
            if (content.detectedQuestions.isNotEmpty()) {
                appendLine()
                appendLine("【ページ内で検出された設問候補】")
                content.detectedQuestions.take(15).forEachIndexed { idx, q ->
                    appendLine("${idx + 1}. $q")
                }
            }
            appendLine()
            appendLine("【ページ本文】")
            appendLine(content.fullText.trim().take(MAX_PROMPT_BODY_CHARS))
        }

        fun parseGeminiJsonResponse(
            jsonString: String,
            content: ExtractedPageContent
        ): PageAiAnalysisResult {
            val json = JSONObject(jsonString)
            val displayTitle = content.title.ifBlank { content.url.ifBlank { "表示中のページ" } }
            val pageSummary = json.optString("pageSummary", "").trim()
                .ifBlank { "「$displayTitle」の内容を分析しました。" }

            val problemsArr = json.optJSONArray("problems")
            val problems = buildList {
                if (problemsArr != null) {
                    for (i in 0 until problemsArr.length()) {
                        val item = problemsArr.optJSONObject(i) ?: continue
                        val question = item.optString("question", "").trim()
                        val answer = item.optString("answer", "").trim()
                        val explanation = item.optString("explanation", "").trim()
                        if (question.isNotEmpty() || answer.isNotEmpty()) {
                            add(
                                SolvedProblem(
                                    number = size + 1,
                                    question = question.ifBlank { "設問 ${size + 1}" },
                                    answer = answer.ifBlank { "回答を生成できませんでした" },
                                    explanation = explanation
                                )
                            )
                        }
                    }
                }
            }

            if (problems.isEmpty()) {
                throw IllegalStateException("AIの応答から回答リストを抽出できませんでした。再分析をお試しください。")
            }

            return PageAiAnalysisResult(
                pageTitle = displayTitle,
                pageUrl = content.url,
                extractedCharCount = content.fullText.trim().length,
                pageSummary = pageSummary,
                problems = problems,
                isPlaceholder = false
            )
        }
    }
}

object PageContentExtractor {

    private const val MAX_EXTRACTED_CHARS = 24_000

    private val EXTRACT_PAGE_JS = """
        (function() {
            try {
                var title = document.title || '';
                var url = window.location.href || '';
                var headings = [];
                document.querySelectorAll('h1, h2, h3, legend').forEach(function(el) {
                    var t = (el.innerText || '').trim();
                    if (t && t.length <= 200 && headings.length < 25) {
                        headings.push(t);
                    }
                });
                var questions = [];
                document.querySelectorAll('p, li, dt, label, fieldset, .question, [class*="question"], [class*="quiz"]').forEach(function(el) {
                    var t = (el.innerText || '').replace(/\s+/g, ' ').trim();
                    if (t.length >= 6 && t.length <= 260 && questions.length < 15) {
                        if (/[?？]$/.test(t) || /^(問|問題|Q|Question|設問|第\d+問|\(\d+\)|（\d+）)/i.test(t)) {
                            if (questions.indexOf(t) === -1) {
                                questions.push(t);
                            }
                        }
                    }
                });
                var bodyText = (document.body && document.body.innerText) ? document.body.innerText : '';
                bodyText = bodyText.replace(/\n{3,}/g, '\n\n').trim().slice(0, $MAX_EXTRACTED_CHARS);
                return JSON.stringify({
                    title: title,
                    url: url,
                    headings: headings,
                    questions: questions,
                    text: bodyText
                });
            } catch (e) {
                return JSON.stringify({ title: document.title || '', url: window.location.href || '', headings: [], questions: [], text: '' });
            }
        })();
    """.trimIndent()

    suspend fun extractFromWebView(
        webView: WebView,
        fallbackTitle: String,
        fallbackUrl: String
    ): ExtractedPageContent = suspendCancellableCoroutine { cont ->
        webView.evaluateJavascript(EXTRACT_PAGE_JS) { rawResult ->
            if (!cont.isActive) return@evaluateJavascript
            val parsed = parseJsPayload(rawResult, fallbackTitle, fallbackUrl)
            cont.resume(parsed)
        }
    }

    fun parseJsPayload(
        rawResult: String?,
        fallbackTitle: String,
        fallbackUrl: String
    ): ExtractedPageContent {
        if (rawResult.isNullOrBlank() || rawResult == "null") {
            return ExtractedPageContent(
                title = fallbackTitle,
                url = fallbackUrl,
                fullText = ""
            )
        }
        return runCatching {
            // evaluateJavascript wraps string return values in JSON string quotes
            val jsonText = if (rawResult.startsWith("\"") && rawResult.endsWith("\"")) {
                JSONObject("{\"v\":$rawResult}").getString("v")
            } else {
                rawResult
            }
            val obj = JSONObject(jsonText)
            val title = obj.optString("title", "").ifBlank { fallbackTitle }
            val url = obj.optString("url", "").ifBlank { fallbackUrl }
            val text = obj.optString("text", "").take(MAX_EXTRACTED_CHARS)
            val headingsArr = obj.optJSONArray("headings")
            val headings = buildList {
                if (headingsArr != null) {
                    for (i in 0 until headingsArr.length()) {
                        val item = headingsArr.optString(i).trim()
                        if (item.isNotEmpty()) add(item)
                    }
                }
            }
            val questionsArr = obj.optJSONArray("questions")
            val questions = buildList {
                if (questionsArr != null) {
                    for (i in 0 until questionsArr.length()) {
                        val item = questionsArr.optString(i).trim()
                        if (item.isNotEmpty()) add(item)
                    }
                }
            }
            ExtractedPageContent(
                title = title,
                url = url,
                fullText = text,
                headings = headings,
                detectedQuestions = questions
            )
        }.getOrElse {
            ExtractedPageContent(
                title = fallbackTitle,
                url = fallbackUrl,
                fullText = ""
            )
        }
    }
}
