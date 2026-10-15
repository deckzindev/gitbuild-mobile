package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ArtifactItem
import com.example.data.GitHubApiClient
import com.example.data.GitHubUser
import com.example.data.TokenStorage
import com.example.data.WorkflowJob
import com.example.data.WorkflowRun
import com.example.util.ApkHelper
import com.example.util.ProjectAnalysis
import com.example.util.ZipHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

enum class AppScreen {
    TOKEN,
    SELECT_ZIP,
    LOGS
}

enum class BuildPhase {
    IDLE,
    PREPARING,
    UPLOADING,
    QUEUED,
    IN_PROGRESS,
    COMPLETED_SUCCESS,
    COMPLETED_FAILURE,
    ERROR
}

data class BuildUiState(
    val currentScreen: AppScreen = AppScreen.TOKEN,
    val tokenInput: String = "",
    val isSavingToken: Boolean = false,
    val tokenUser: GitHubUser? = null,
    val tokenError: String? = null,
    val targetRepo: String = "",

    val isReadingZip: Boolean = false,
    val selectedProject: ProjectAnalysis? = null,

    val buildPhase: BuildPhase = BuildPhase.IDLE,
    val buildStatusMessage: String = "",
    val uploadCurrent: Int = 0,
    val uploadTotal: Int = 0,
    val commitSha: String? = null,
    val workflowRun: WorkflowRun? = null,
    val jobs: List<WorkflowJob> = emptyList(),
    val logsText: String = "",
    val elapsedSeconds: Long = 0,

    val artifacts: List<ArtifactItem> = emptyList(),
    val validApkArtifact: ArtifactItem? = null,
    val isDownloadingApk: Boolean = false,
    val downloadPercent: Int = 0,
    val downloadedApkFile: File? = null,
    val generalError: String? = null
)

class BuildViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenStorage = TokenStorage(application)
    private val gitHubClient = GitHubApiClient()

    private val _uiState = MutableStateFlow(BuildUiState())
    val uiState: StateFlow<BuildUiState> = _uiState.asStateFlow()

    private var buildPollingJob: Job? = null
    private var timerJob: Job? = null
    private val baseLogs = StringBuilder()

    init {
        val savedToken = tokenStorage.getToken()
        val savedUsername = tokenStorage.getUsername()
        val savedAvatar = tokenStorage.getAvatarUrl()
        val savedDisplayName = tokenStorage.getUserDisplayName()
        val savedRepo = tokenStorage.getTargetRepo()

        if (!savedToken.isNullOrBlank()) {
            val cachedUser = if (!savedUsername.isNullOrBlank()) {
                GitHubUser(
                    login = savedUsername,
                    name = savedDisplayName,
                    avatarUrl = savedAvatar
                )
            } else null

            _uiState.update {
                it.copy(
                    currentScreen = AppScreen.SELECT_ZIP,
                    tokenInput = savedToken,
                    tokenUser = cachedUser,
                    targetRepo = savedRepo
                )
            }
        } else {
            _uiState.update { it.copy(currentScreen = AppScreen.TOKEN) }
        }
    }

    fun onTokenInputChanged(token: String) {
        _uiState.update { it.copy(tokenInput = token, tokenError = null) }
    }

    fun saveTokenAndCreateRepo() {
        val token = _uiState.value.tokenInput.trim()
        if (token.isBlank()) {
            _uiState.update { it.copy(tokenError = "Insira seu token do GitHub.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingToken = true, tokenError = null) }

            // 1. Verify token
            val verifyResult = gitHubClient.verifyToken(token)
            if (verifyResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isSavingToken = false,
                        tokenError = verifyResult.exceptionOrNull()?.localizedMessage ?: "Token inválido."
                    )
                }
                return@launch
            }

            val user = verifyResult.getOrThrow()

            // 2. Generate random repo name and create it automatically on GitHub
            val randomSuffix = Random.nextInt(100000, 999999)
            val randomRepoName = "apk-builder-$randomSuffix"

            val repoResult = gitHubClient.createRandomRepo(token, randomRepoName)
            if (repoResult.isFailure) {
                // If random creation failed (e.g., name collision or fine-grained token scope), fallback to getOrCreateRepo
                val fallbackResult = gitHubClient.getOrCreateRepo(token, user.login, randomRepoName)
                if (fallbackResult.isFailure) {
                    _uiState.update {
                        it.copy(
                            isSavingToken = false,
                            tokenError = "Erro ao criar repositório no GitHub: ${fallbackResult.exceptionOrNull()?.localizedMessage}"
                        )
                    }
                    return@launch
                }
            }

            // 3. Save credentials and transition to Select File Screen
            tokenStorage.saveToken(token)
            tokenStorage.saveUserInfo(user)
            tokenStorage.saveTargetRepo(randomRepoName)

            _uiState.update {
                it.copy(
                    isSavingToken = false,
                    tokenUser = user,
                    targetRepo = randomRepoName,
                    currentScreen = AppScreen.SELECT_ZIP,
                    tokenError = null
                )
            }
        }
    }

    fun goToTokenScreen() {
        _uiState.update { it.copy(currentScreen = AppScreen.TOKEN) }
    }

    fun goToSelectZipScreen() {
        _uiState.update { it.copy(currentScreen = AppScreen.SELECT_ZIP) }
    }

    fun onZipSelected(uri: Uri) {
        viewModelScope.launch {
            baseLogs.clear()
            baseLogs.append(">>> Arquivo ZIP selecionado. Processando código-fonte...\n")
            _uiState.update {
                it.copy(
                    isReadingZip = true,
                    currentScreen = AppScreen.LOGS,
                    buildPhase = BuildPhase.PREPARING,
                    buildStatusMessage = "Lendo arquivo ZIP...",
                    logsText = baseLogs.toString(),
                    elapsedSeconds = 0,
                    workflowRun = null,
                    jobs = emptyList(),
                    artifacts = emptyList(),
                    validApkArtifact = null,
                    downloadedApkFile = null
                )
            }

            val context = getApplication<Application>()
            val result = ZipHelper.readZipFromUri(context, uri)

            if (result.isFailure) {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Falha ao ler o arquivo ZIP."
                appendLog("[ERRO]: $err")
                _uiState.update {
                    it.copy(
                        isReadingZip = false,
                        buildPhase = BuildPhase.ERROR,
                        buildStatusMessage = err
                    )
                }
                return@launch
            }

            val project = result.getOrThrow()
            _uiState.update {
                it.copy(
                    isReadingZip = false,
                    selectedProject = project
                )
            }

            appendLog(">>> Projeto: ${project.fileName} (${ZipHelper.formatBytes(project.fileSizeBytes)}, ${project.totalFilesCount} arquivos)")
            if (project.isAndroidProject) {
                appendLog(">>> Estrutura Android detectada.")
            } else {
                appendLog(">>> Aviso: Arquivo Gradle não encontrado na raiz; tentando compilação...")
            }

            // Immediately launch build!
            startBuild()
        }
    }

    private fun startBuild() {
        val state = _uiState.value
        val token = tokenStorage.getToken()
        val user = state.tokenUser
        val project = state.selectedProject
        val repoName = state.targetRepo.ifBlank { tokenStorage.getTargetRepo() }

        if (token.isNullOrBlank() || user == null || project == null) {
            appendLog("[ERRO]: Credenciais ou projeto não disponíveis.")
            _uiState.update {
                it.copy(
                    buildPhase = BuildPhase.ERROR,
                    buildStatusMessage = "Configuração incompleta."
                )
            }
            return
        }

        buildPollingJob?.cancel()
        timerJob?.cancel()

        startTimer()

        viewModelScope.launch {
            runBuildFlow(token, user.login, repoName, project)
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    private suspend fun runBuildFlow(
        token: String,
        owner: String,
        repoName: String,
        project: ProjectAnalysis
    ) {
        try {
            appendLog(">>> Repositório alvo: $owner/$repoName")
            _uiState.update {
                it.copy(
                    buildPhase = BuildPhase.UPLOADING,
                    buildStatusMessage = "Enviando código-fonte para o GitHub..."
                )
            }

            appendLog(">>> Enviando ${project.totalFilesCount} arquivos via Git Blobs...")
            val uploadResult = gitHubClient.uploadProjectAndTriggerBuild(
                token = token,
                owner = owner,
                repo = repoName,
                projectFiles = project.files,
                onProgress = { status, current, total ->
                    _uiState.update {
                        it.copy(
                            buildStatusMessage = status,
                            uploadCurrent = current,
                            uploadTotal = total
                        )
                    }
                }
            )

            val commitSha = uploadResult.getOrThrow()
            appendLog(">>> Commit criado com sucesso: ${commitSha.take(8)}")
            appendLog(">>> Disparando GitHub Actions na branch main...")

            _uiState.update {
                it.copy(
                    commitSha = commitSha,
                    buildPhase = BuildPhase.QUEUED,
                    buildStatusMessage = "Aguardando GitHub Actions enfileirar a compilação..."
                )
            }

            // Start polling for workflow run and streaming logs
            pollWorkflowAndStreamLogs(token, owner, repoName, commitSha)

        } catch (e: Exception) {
            timerJob?.cancel()
            val msg = e.localizedMessage ?: "Erro durante o envio ao GitHub."
            appendLog("\n[FALHA]: $msg")
            _uiState.update {
                it.copy(
                    buildPhase = BuildPhase.ERROR,
                    buildStatusMessage = "Falha: $msg"
                )
            }
        }
    }

    private fun pollWorkflowAndStreamLogs(
        token: String,
        owner: String,
        repoName: String,
        commitSha: String
    ) {
        buildPollingJob = viewModelScope.launch {
            var runId: Long? = null
            var retryCount = 0

            // 1. Wait for workflow run to appear for this commit
            while (isActive && runId == null && retryCount < 30) {
                delay(2500)
                val runResult = gitHubClient.findWorkflowRunForCommit(token, owner, repoName, commitSha)
                val run = runResult.getOrNull()
                if (run != null) {
                    runId = run.id
                    _uiState.update {
                        it.copy(
                            workflowRun = run,
                            buildStatusMessage = "Execução #${run.runNumber ?: run.id} iniciada no GitHub Actions..."
                        )
                    }
                    appendLog(">>> GitHub Actions iniciou o build #${run.runNumber ?: run.id} (Run ID: ${run.id}).")
                    break
                }
                retryCount++
                appendLog("... aguardando GitHub Actions (${retryCount}/30)")
            }

            if (runId == null) {
                timerJob?.cancel()
                appendLog("\n[ERRO]: Tempo limite excedido aguardando o GitHub Actions.")
                _uiState.update {
                    it.copy(
                        buildPhase = BuildPhase.ERROR,
                        buildStatusMessage = "Tempo limite excedido."
                    )
                }
                return@launch
            }

            // 2. Poll workflow run status and job logs
            var completed = false
            var activeJobId: Long? = null

            while (isActive && !completed) {
                delay(3000)

                // Get run status
                val runResult = gitHubClient.getWorkflowRun(token, owner, repoName, runId)
                val currentRun = runResult.getOrNull() ?: continue
                _uiState.update { it.copy(workflowRun = currentRun) }

                // Get jobs
                val jobsResult = gitHubClient.getWorkflowJobs(token, owner, repoName, runId)
                val jobs = jobsResult.getOrDefault(emptyList())
                if (jobs.isNotEmpty()) {
                    _uiState.update { it.copy(jobs = jobs) }
                    activeJobId = jobs.firstOrNull()?.id ?: activeJobId
                }

                val status = currentRun.status?.lowercase()
                val conclusion = currentRun.conclusion?.lowercase()

                if (status == "in_progress") {
                    _uiState.update {
                        it.copy(
                            buildPhase = BuildPhase.IN_PROGRESS,
                            buildStatusMessage = "Compilando APK no runner Ubuntu..."
                        )
                    }
                }

                // If job ID is available, try fetching live logs
                var fetchedLiveLogs = false
                if (activeJobId != null) {
                    val logsResult = gitHubClient.getJobLogs(token, owner, repoName, activeJobId)
                    val rawLogs = logsResult.getOrNull()
                    if (!rawLogs.isNullOrBlank()) {
                        fetchedLiveLogs = true
                        _uiState.update { it.copy(logsText = cleanAnsiCodes(rawLogs)) }
                    }
                }

                // While logs are not yet available from GitHub, show live step progress
                if (!fetchedLiveLogs && jobs.isNotEmpty()) {
                    val liveStepsText = buildLiveStepsLog(baseLogs.toString(), currentRun, jobs)
                    _uiState.update { it.copy(logsText = liveStepsText) }
                }

                if (status == "completed") {
                    completed = true
                    timerJob?.cancel()

                    // Final fetch of logs (retry for up to 12s as GitHub finalizes the log file)
                    if (activeJobId != null) {
                        var finalLogs: String? = null
                        for (attempt in 1..8) {
                            val finalLogsResult = gitHubClient.getJobLogs(token, owner, repoName, activeJobId)
                            finalLogs = finalLogsResult.getOrNull()
                            if (!finalLogs.isNullOrBlank()) break
                            delay(1500)
                        }
                        if (!finalLogs.isNullOrBlank()) {
                            _uiState.update { it.copy(logsText = cleanAnsiCodes(finalLogs)) }
                        } else {
                            val finalSummary = buildLiveStepsLog(baseLogs.toString(), currentRun, jobs)
                            _uiState.update { it.copy(logsText = finalSummary) }
                        }
                    }

                    // Check artifacts
                    appendLog("\n>>> Verificando artefato APK gerado...")
                    val artifactsResult = gitHubClient.getArtifacts(token, owner, repoName, runId)
                    val artifacts = artifactsResult.getOrDefault(emptyList())
                    val apkArtifact = artifacts.firstOrNull {
                        it.name.contains("apk", ignoreCase = true) && !it.expired
                    }

                    if (conclusion == "success") {
                        appendLog("\n[SUCESSO] Compilação concluída com sucesso!")
                        if (apkArtifact != null) {
                            appendLog(">>> APK disponível: ${apkArtifact.name} (${ZipHelper.formatBytes(apkArtifact.sizeInBytes)})")
                        } else {
                            appendLog(">>> Nenhum arquivo APK foi gerado na saída.")
                        }

                        _uiState.update {
                            it.copy(
                                buildPhase = BuildPhase.COMPLETED_SUCCESS,
                                buildStatusMessage = "Compilação concluída com sucesso!",
                                artifacts = artifacts,
                                validApkArtifact = apkArtifact
                            )
                        }
                    } else {
                        appendLog("\n[FALHA NA COMPILAÇÃO] Conclusão: $conclusion")
                        _uiState.update {
                            it.copy(
                                buildPhase = BuildPhase.COMPLETED_FAILURE,
                                buildStatusMessage = "Compilação falhou ($conclusion).",
                                artifacts = artifacts,
                                validApkArtifact = apkArtifact
                            )
                        }
                    }
                }
            }
        }
    }

    fun downloadAndOpenApk(context: Context) {
        val token = tokenStorage.getToken() ?: return
        val artifact = _uiState.value.validApkArtifact ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isDownloadingApk = true, downloadPercent = 0) }
            try {
                val tempZip = File(context.cacheDir, "build_artifact_${artifact.id}.zip")
                if (tempZip.exists()) tempZip.delete()

                val result = gitHubClient.downloadArtifactZip(
                    token = token,
                    downloadUrl = artifact.archiveDownloadUrl,
                    destinationFile = tempZip,
                    onProgress = { downloaded, total ->
                        if (total > 0) {
                            val percent = ((downloaded * 100) / total).toInt()
                            _uiState.update { it.copy(downloadPercent = percent) }
                        }
                    }
                )

                val downloadedZip = result.getOrThrow()
                val apkDir = File(context.filesDir, "downloaded_apks")
                val extractedResult = gitHubClient.extractApkFromArtifactZip(downloadedZip, apkDir)
                val apkFile = extractedResult.getOrNull()

                if (apkFile != null && apkFile.exists()) {
                    _uiState.update {
                        it.copy(
                            isDownloadingApk = false,
                            downloadedApkFile = apkFile
                        )
                    }
                    ApkHelper.installApk(context, apkFile)
                } else {
                    throw IllegalStateException("Nenhum arquivo .apk encontrado no arquivo baixado.")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isDownloadingApk = false) }
                appendLog("\n[ERRO AO BAIXAR APK]: ${e.localizedMessage}")
            }
        }
    }

    fun copyLogsToClipboard(context: Context) {
        ApkHelper.copyToClipboard(context, _uiState.value.logsText)
    }

    private fun appendLog(line: String) {
        baseLogs.append(line).append("\n")
        _uiState.update {
            it.copy(logsText = baseLogs.toString())
        }
    }

    private fun buildLiveStepsLog(
        base: String,
        currentRun: WorkflowRun?,
        jobs: List<WorkflowJob>
    ): String {
        val sb = StringBuilder(base.trimEnd())
        sb.append("\n\n------------------------------------------------------------\n")
        val runNum = currentRun?.runNumber ?: currentRun?.id ?: 0
        val runStatus = currentRun?.status ?: "in_progress"
        sb.append(">>> GITHUB ACTIONS: Execução #$runNum (Status: $runStatus)\n")

        val firstJob = jobs.firstOrNull()
        if (firstJob != null) {
            sb.append(">>> Runner: Ubuntu 24.04 (GitHub Cloud) • Job: ${firstJob.name}\n\n")
            if (firstJob.steps.isNotEmpty()) {
                sb.append(">>> ETAPAS DA COMPILAÇÃO:\n")
                firstJob.steps.forEach { step ->
                    val icon = when (step.conclusion) {
                        "success" -> "[✓]"
                        "failure" -> "[✗]"
                        "skipped" -> "[-]"
                        else -> if (step.status == "in_progress") "[▶]" else "[ ]"
                    }
                    val statusText = when {
                        step.conclusion != null -> step.conclusion
                        step.status == "in_progress" -> "executando..."
                        else -> "aguardando"
                    }
                    sb.append(String.format(java.util.Locale.US, "  %-3s %-32s (%s)\n", icon, step.name, statusText))
                }
            } else {
                sb.append(">>> Inicializando ambiente de compilação...\n")
            }
        } else {
            sb.append(">>> Alocando máquina virtual runner no GitHub Actions...\n")
        }
        sb.append("------------------------------------------------------------\n")
        return sb.toString()
    }

    private fun cleanAnsiCodes(rawText: String): String {
        return rawText.replace(Regex("\u001B\\[[;\\d]*[ -/]*[@-~]"), "")
    }

    override fun onCleared() {
        super.onCleared()
        buildPollingJob?.cancel()
        timerJob?.cancel()
    }
}
