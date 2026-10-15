package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Segment
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRed
import com.example.ui.theme.TerminalBlack
import com.example.ui.theme.TerminalCardBg
import com.example.ui.theme.TerminalCardBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.ZipHelper

/**
 * Screen 1: Clean screen with ONLY GitHub Token input and Save button.
 */
@Composable
fun TokenCleanScreen(
    state: BuildUiState,
    onTokenChanged: (String) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = TerminalBlack
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(AccentCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "Token GitHub",
                        tint = AccentCyan,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Token do GitHub",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Insira seu Personal Access Token (escopo 'repo')",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(28.dp))

                OutlinedTextField(
                    value = state.tokenInput,
                    onValueChange = onTokenChanged,
                    placeholder = { Text("ghp_xxxxxxxxxxxxxxxxxxxx", color = TextMuted) },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { onSaveClick() }),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("github_token_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = TerminalCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = AccentCyan
                    )
                )

                AnimatedVisibility(visible = state.tokenError != null) {
                    Text(
                        text = state.tokenError ?: "",
                        color = AccentRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onSaveClick,
                    enabled = !state.isSavingToken && state.tokenInput.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentCyan,
                        contentColor = Color(0xFF04192B)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_token_button")
                ) {
                    if (state.isSavingToken) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFF04192B),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Criando repositório...", fontWeight = FontWeight.Bold)
                    } else {
                        Text("Salvar Token", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

/**
 * Screen 2: Clean screen with ONE primary button to select ZIP, plus History button in header.
 */
@Composable
fun SelectZipCleanScreen(
    state: BuildUiState,
    onSelectZipClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onChangeTokenClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = TerminalBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Minimal Header with History button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onHistoryClick,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF161E28),
                        contentColor = TextPrimary
                    ),
                    modifier = Modifier.testTag("open_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = AccentCyan
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.historyList.isNotEmpty()) "Histórico (${state.historyList.size})" else "Histórico",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Center Content: One big clean button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(AccentEmerald.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderZip,
                            contentDescription = "Selecionar arquivo",
                            tint = AccentEmerald,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = onSelectZipClick,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentEmerald,
                            contentColor = Color(0xFF042114)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .testTag("select_zip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderZip,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Selecionar Arquivo ZIP",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "A compilação roda em segundo plano mesmo se sair do app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            // Bottom subtle metadata & option to change token
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.tokenUser != null) {
                    Text(
                        text = "Conectado como @${state.tokenUser.login} • Repo: ${state.targetRepo}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }

                TextButton(
                    onClick = onChangeTokenClick,
                    modifier = Modifier.testTag("change_token_text_button")
                ) {
                    Text(
                        text = "Trocar Token do GitHub",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * Screen 3: Organized compilation logs screen.
 * Displays logs organized by clean, expandable STAGES so the user never gets confused,
 * with option to switch to raw terminal view.
 */
@Composable
fun LogsCleanScreen(
    state: BuildUiState,
    onBackClick: () -> Unit,
    onToggleViewMode: () -> Unit,
    onDownloadApkClick: () -> Unit,
    onCopyLogsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rawListState = rememberLazyListState()
    BackHandler { onBackClick() }

    val expandedStages = remember { mutableStateMapOf<String, Boolean>() }

    // Auto-expand stage in progress
    LaunchedEffect(state.stages) {
        state.stages.forEach { stage ->
            if (stage.status == StageStatus.IN_PROGRESS) {
                expandedStages[stage.key] = true
            }
        }
    }

    val logLines = remember(state.logsText) {
        if (state.logsText.isEmpty()) emptyList()
        else state.logsText.lines()
    }

    LaunchedEffect(logLines.size) {
        if (logLines.isNotEmpty() && state.logViewMode == LogViewMode.RAW_LOGS) {
            rawListState.animateScrollToItem(logLines.size - 1)
        }
    }

    val isFinished = state.buildPhase == BuildPhase.COMPLETED_SUCCESS ||
            state.buildPhase == BuildPhase.COMPLETED_FAILURE ||
            state.buildPhase == BuildPhase.ERROR

    Surface(
        modifier = modifier.fillMaxSize(),
        color = TerminalBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalCardBg)
                    .border(1.dp, TerminalCardBorder)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "Compilação Android",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = state.buildStatusMessage.ifEmpty { "Compilando..." },
                            style = MaterialTheme.typography.bodySmall,
                            color = when (state.buildPhase) {
                                BuildPhase.COMPLETED_SUCCESS -> AccentEmerald
                                BuildPhase.COMPLETED_FAILURE, BuildPhase.ERROR -> AccentRed
                                else -> AccentCyan
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Toggle View Mode (Stages vs Raw Logs)
                    FilterChip(
                        selected = state.logViewMode == LogViewMode.STAGES,
                        onClick = onToggleViewMode,
                        label = {
                            Text(
                                text = if (state.logViewMode == LogViewMode.STAGES) "Etapas" else "Bruto",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (state.logViewMode == LogViewMode.STAGES) Icons.Default.Segment else Icons.Default.Terminal,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentCyan.copy(alpha = 0.2f),
                            selectedLabelColor = AccentCyan,
                            selectedLeadingIconColor = AccentCyan,
                            containerColor = Color(0xFF161E28),
                            labelColor = TextMuted,
                            iconColor = TextMuted
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = state.logViewMode == LogViewMode.STAGES,
                            borderColor = if (state.logViewMode == LogViewMode.STAGES) AccentCyan else TerminalCardBorder
                        ),
                        modifier = Modifier.padding(end = 4.dp).testTag("toggle_view_mode_chip")
                    )

                    IconButton(
                        onClick = onCopyLogsClick,
                        modifier = Modifier.testTag("copy_logs_header_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copiar logs",
                            tint = TextSecondary
                        )
                    }
                }
            }

            // Progress bar
            if (!isFinished) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = AccentCyan,
                    trackColor = TerminalCardBorder
                )
            }

            // Main Body: Either Organized Stages or Raw Terminal
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(TerminalBlack)
            ) {
                if (state.logViewMode == LogViewMode.STAGES) {
                    // ORGANIZED STAGES VIEW
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("stages_list"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.stages, key = { it.key }) { stage ->
                            val isExpanded = expandedStages[stage.key] ?: (stage.status == StageStatus.IN_PROGRESS)
                            StageCard(
                                stage = stage,
                                isExpanded = isExpanded,
                                onToggleExpand = {
                                    expandedStages[stage.key] = !isExpanded
                                }
                            )
                        }
                    }
                } else {
                    // RAW TERMINAL VIEW
                    if (logLines.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = AccentCyan, strokeWidth = 3.dp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Iniciando compilação no GitHub Actions...",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = rawListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .testTag("raw_logs_list")
                        ) {
                            itemsIndexed(logLines) { index, line ->
                                val isError = line.contains("ERROR", ignoreCase = true) ||
                                        line.contains("FAILURE", ignoreCase = true) ||
                                        line.contains("FAILED", ignoreCase = true) ||
                                        line.startsWith("[FALHA]") || line.startsWith("[ERRO]")
                                val isSuccess = line.contains("SUCCESSFUL", ignoreCase = true) ||
                                        line.startsWith("[SUCESSO]")
                                val isHeader = line.startsWith(">>>")

                                val color = when {
                                    isError -> AccentRed
                                    isSuccess -> AccentEmerald
                                    isHeader -> AccentCyan
                                    else -> TextPrimary
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 1.dp)
                                ) {
                                    Text(
                                        text = String.format(java.util.Locale.US, "%4d ", index + 1),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = line,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = color,
                                        lineHeight = 15.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = TerminalCardBorder, thickness = 1.dp)

            // Bottom Action Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalCardBg)
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                // If finished and APK available: Download APK Button
                if (isFinished && state.validApkArtifact != null) {
                    val artifact = state.validApkArtifact
                    val sizeFormatted = ZipHelper.formatBytes(artifact.sizeInBytes)

                    Button(
                        onClick = onDownloadApkClick,
                        enabled = !state.isDownloadingApk,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentEmerald,
                            contentColor = Color(0xFF042114)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("download_apk_button")
                    ) {
                        if (state.isDownloadingApk) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFF042114),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (state.downloadPercent > 0) "Baixando APK (${state.downloadPercent}%)..." else "Baixando...",
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = if (state.downloadedApkFile != null) Icons.Default.Android else Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (state.downloadedApkFile != null) "Instalar APK" else "Baixar APK ($sizeFormatted)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Copy All Logs Button (Always visible!)
                Button(
                    onClick = onCopyLogsClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.validApkArtifact == null) AccentCyan else Color(0xFF212836),
                        contentColor = if (state.validApkArtifact == null) Color(0xFF04192B) else TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("copy_all_logs_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copiar Todos os Logs", fontWeight = FontWeight.Bold)
                }

                if (isFinished) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_project_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Selecionar Outro Arquivo",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Clean Stage Card that organizes logs by step.
 */
@Composable
private fun StageCard(
    stage: BuildStageItem,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val (statusIcon, statusColor, statusLabel) = when (stage.status) {
        StageStatus.SUCCESS -> Triple(Icons.Default.CheckCircle, AccentEmerald, "Concluído")
        StageStatus.IN_PROGRESS -> Triple(Icons.Default.HourglassEmpty, AccentCyan, "Em andamento")
        StageStatus.FAILURE -> Triple(Icons.Default.Error, AccentRed, "Falhou")
        StageStatus.PENDING -> Triple(Icons.Default.HourglassEmpty, TextMuted, "Pendente")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (stage.status == StageStatus.IN_PROGRESS) AccentCyan.copy(alpha = 0.5f) else TerminalCardBorder,
                RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = TerminalCardBg)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (stage.status == StageStatus.IN_PROGRESS) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = AccentCyan,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = stage.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${stage.subtitle} • $statusLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (stage.lines.isNotEmpty()) {
                        Text(
                            text = "${stage.lines.size} linhas",
                            fontSize = 11.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Recolher" else "Expandir",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Expanded Stage Log Content
            AnimatedVisibility(visible = isExpanded && stage.lines.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF090D12))
                        .border(1.dp, Color(0xFF1B232D))
                        .padding(12.dp)
                ) {
                    stage.lines.forEachIndexed { i, line ->
                        val isErr = line.contains("error", ignoreCase = true) ||
                                line.contains("fail", ignoreCase = true) ||
                                line.startsWith("[FALHA]") || line.startsWith("[ERRO]")
                        val isOk = line.contains("success", ignoreCase = true) ||
                                line.startsWith("[SUCESSO]")
                        val textColor = when {
                            isErr -> AccentRed
                            isOk -> AccentEmerald
                            else -> TextSecondary
                        }
                        Text(
                            text = line,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = textColor,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}
