package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * Screen 2: Clean screen with ONLY ONE button to select ZIP file.
 */
@Composable
fun SelectZipCleanScreen(
    state: BuildUiState,
    onSelectZipClick: () -> Unit,
    onChangeTokenClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = TerminalBlack
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Center Content: One big clean button
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
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
                    text = "A compilação iniciará automaticamente após a seleção.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            // Bottom subtle metadata & option to change token
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
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
 * Screen 3: Fullscreen logs showing all compilation logs in real time.
 * In the end:
 * - "Baixar APK" button in case of success and valid artifact
 * - "Copiar Todos os Logs" button in case of success or failure
 */
@Composable
fun LogsCleanScreen(
    state: BuildUiState,
    onBackClick: () -> Unit,
    onDownloadApkClick: () -> Unit,
    onCopyLogsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    BackHandler { onBackClick() }

    val logLines = remember(state.logsText) {
        if (state.logsText.isEmpty()) emptyList()
        else state.logsText.lines()
    }

    LaunchedEffect(logLines.size) {
        if (logLines.isNotEmpty()) {
            listState.animateScrollToItem(logLines.size - 1)
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
                            text = "Logs da Compilação",
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

            // Progress bar
            if (!isFinished) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = AccentCyan,
                    trackColor = TerminalCardBorder
                )
            }

            // Terminal Log Viewer
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(TerminalBlack)
            ) {
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
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .testTag("logs_list")
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
