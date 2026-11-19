package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.adblock.AdBlockEngine
import com.example.ui.browser.PageAiAnalysisResult
import com.example.ui.browser.PageAiUiState
import com.example.ui.browser.SolvedProblem
import com.example.ui.theme.AegisTheme
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun AiAnswerPopupDialog(
    state: PageAiUiState,
    onDismiss: () -> Unit,
    onReanalyze: () -> Unit,
    onCopyText: (String) -> Unit
) {
    if (state is PageAiUiState.Hidden) return

    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors

    val headerTitle = when (state) {
        is PageAiUiState.Loading -> state.pageTitle
        is PageAiUiState.Success -> state.result.pageTitle
        is PageAiUiState.Error -> state.pageTitle
        PageAiUiState.Hidden -> ""
    }
    val headerUrl = when (state) {
        is PageAiUiState.Loading -> state.pageUrl
        is PageAiUiState.Success -> state.result.pageUrl
        is PageAiUiState.Error -> state.pageUrl
        PageAiUiState.Hidden -> ""
    }
    val hostLabel = AdBlockEngine.extractHost(headerUrl).ifBlank { headerTitle }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .widthIn(max = 560.dp)
                .heightIn(max = 680.dp)
                .testTag("ai_answer_popup_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 10.dp, top = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = aegisColors.shieldActiveContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = aegisColors.shieldActive,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.ai_solver_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        if (hostLabel.isNotBlank()) {
                            Text(
                                text = hostLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("ai_popup_close_icon")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.ai_solver_close),
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)

                // Body
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    when (state) {
                        is PageAiUiState.Loading -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = aegisColors.shieldActive,
                                    strokeWidth = 3.dp
                                )
                                Text(
                                    text = stringResource(R.string.ai_solver_analyzing),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "表示中のページ全体から設問と本文を抽出しています",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        is PageAiUiState.Error -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ErrorOutline,
                                    contentDescription = null,
                                    tint = colorScheme.error,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "分析に失敗しました",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        is PageAiUiState.Success -> {
                            AiAnalysisSuccessBody(
                                result = state.result,
                                onCopyAnswer = onCopyText
                            )
                        }

                        PageAiUiState.Hidden -> Unit
                    }
                }

                HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)

                // Footer Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state is PageAiUiState.Success) {
                        TextButton(
                            onClick = {
                                val allText = state.result.problems.joinToString("\n\n") { item ->
                                    "Q${item.number}: ${item.question}\n回答: ${item.answer}\n解説: ${item.explanation}"
                                }
                                onCopyText(allText)
                            },
                            modifier = Modifier.testTag("ai_popup_copy_all_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.ai_solver_copy_all))
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    OutlinedButton(
                        onClick = onReanalyze,
                        enabled = state !is PageAiUiState.Loading,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("ai_popup_reanalyze_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.ai_solver_reanalyze))
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = aegisColors.shieldActive,
                            contentColor = colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("ai_popup_close_button")
                    ) {
                        Text(
                            text = stringResource(R.string.ai_solver_close),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AiAnalysisSuccessBody(
    result: PageAiAnalysisResult,
    onCopyAnswer: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (result.isPlaceholder) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, colorScheme.tertiary.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_popup_firebase_notice")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = colorScheme.onTertiaryContainer,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(18.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = stringResource(R.string.ai_solver_firebase_notice_title),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = stringResource(R.string.ai_solver_firebase_notice_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }

        // Page Summary Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ページ全体の解析概要",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = aegisColors.shieldActive
                    )
                    Text(
                        text = "読取 ${result.extractedCharCount}文字・${result.problems.size}件",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = result.pageSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurface
                )
            }
        }

        // Problem & Answer Cards
        result.problems.forEach { problem ->
            SolvedProblemCard(
                problem = problem,
                isPlaceholder = result.isPlaceholder,
                onCopyAnswer = { onCopyAnswer(problem.answer) }
            )
        }
    }
}

@Composable
private fun SolvedProblemCard(
    problem: SolvedProblem,
    isPlaceholder: Boolean,
    onCopyAnswer: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val aegisColors = AegisTheme.colors

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_problem_card_${problem.number}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = aegisColors.shieldActiveContainer
                ) {
                    Text(
                        text = "Q${problem.number}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = aegisColors.shieldActive,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = problem.question,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            // Highlighted Answer Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = aegisColors.shieldActiveContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isPlaceholder) "サンプル回答" else "回答",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = aegisColors.shieldActive
                    )
                    IconButton(
                        onClick = onCopyAnswer,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("ai_copy_answer_${problem.number}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = stringResource(R.string.ai_solver_copy_answer),
                            tint = aegisColors.shieldActive,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = problem.answer,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = colorScheme.onSurface
                )
            }

            // Explanation
            if (problem.explanation.isNotBlank()) {
                Text(
                    text = "解説: ${problem.explanation}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
