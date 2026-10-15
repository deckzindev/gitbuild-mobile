package com.example.ui

enum class StageStatus {
    PENDING,
    IN_PROGRESS,
    SUCCESS,
    FAILURE
}

data class BuildStageItem(
    val key: String,
    val title: String,
    val subtitle: String,
    val status: StageStatus = StageStatus.PENDING,
    val lines: List<String> = emptyList()
)

object StageParser {

    fun parseStages(
        rawLogs: String,
        buildPhase: BuildPhase
    ): List<BuildStageItem> {
        val allLines = rawLogs.lines()

        val prepLines = mutableListOf<String>()
        val uploadLines = mutableListOf<String>()
        val queueLines = mutableListOf<String>()
        val envLines = mutableListOf<String>()
        val compileLines = mutableListOf<String>()
        val artifactLines = mutableListOf<String>()

        var currentCategory = 0 // 0: prep, 1: upload, 2: queue, 3: env, 4: compile, 5: artifact

        for (line in allLines) {
            val lower = line.lowercase()
            when {
                lower.contains("arquivo zip selecionado") || lower.contains("processando código-fonte") ||
                        lower.contains("projeto:") || lower.contains("estrutura android") -> {
                    currentCategory = 0
                }
                lower.contains("enviando código-fonte") || lower.contains("enviando arquivos") ||
                        lower.contains("criando árvore git") || lower.contains("criando commit") ||
                        lower.contains("commit criado") -> {
                    currentCategory = 1
                }
                lower.contains("aguardando github actions") || lower.contains("alocando máquina virtual") ||
                        lower.contains("iniciou o build") -> {
                    currentCategory = 2
                }
                lower.contains("checkout") || lower.contains("setup jdk") || lower.contains("set up jdk") ||
                        lower.contains("prepare gradle") || lower.contains("preparando gradle") ||
                        lower.contains("setup-java") -> {
                    currentCategory = 3
                }
                lower.contains("assembledebug") || lower.contains("compile debug apk") ||
                        lower.contains("iniciando compilação do apk") || lower.contains("> task :app:") ||
                        lower.contains("compiledebugkotlin") || lower.contains("build successful") ||
                        lower.contains("build failed") -> {
                    currentCategory = 4
                }
                lower.contains("upload apk artifact") || lower.contains("verificando artefato") ||
                        lower.contains("apk disponível") || lower.contains("upload-artifact") -> {
                    currentCategory = 5
                }
            }

            when (currentCategory) {
                0 -> prepLines.add(line)
                1 -> uploadLines.add(line)
                2 -> queueLines.add(line)
                3 -> envLines.add(line)
                4 -> compileLines.add(line)
                5 -> artifactLines.add(line)
            }
        }

        // Determine statuses based on buildPhase and contents
        val hasFailed = buildPhase == BuildPhase.COMPLETED_FAILURE || buildPhase == BuildPhase.ERROR
        val isSuccess = buildPhase == BuildPhase.COMPLETED_SUCCESS

        val prepStatus = when {
            uploadLines.isNotEmpty() || queueLines.isNotEmpty() || envLines.isNotEmpty() || compileLines.isNotEmpty() || isSuccess -> StageStatus.SUCCESS
            buildPhase == BuildPhase.PREPARING -> StageStatus.IN_PROGRESS
            hasFailed && uploadLines.isEmpty() -> StageStatus.FAILURE
            else -> if (prepLines.isNotEmpty()) StageStatus.SUCCESS else StageStatus.PENDING
        }

        val uploadStatus = when {
            queueLines.isNotEmpty() || envLines.isNotEmpty() || compileLines.isNotEmpty() || isSuccess -> StageStatus.SUCCESS
            buildPhase == BuildPhase.UPLOADING -> StageStatus.IN_PROGRESS
            hasFailed && queueLines.isEmpty() -> StageStatus.FAILURE
            else -> if (uploadLines.isNotEmpty()) StageStatus.SUCCESS else StageStatus.PENDING
        }

        val queueStatus = when {
            envLines.isNotEmpty() || compileLines.isNotEmpty() || isSuccess -> StageStatus.SUCCESS
            buildPhase == BuildPhase.QUEUED -> StageStatus.IN_PROGRESS
            hasFailed && envLines.isEmpty() -> StageStatus.FAILURE
            else -> if (queueLines.isNotEmpty()) StageStatus.SUCCESS else StageStatus.PENDING
        }

        val envStatus = when {
            compileLines.isNotEmpty() || isSuccess -> StageStatus.SUCCESS
            buildPhase == BuildPhase.IN_PROGRESS && compileLines.isEmpty() -> StageStatus.IN_PROGRESS
            hasFailed && compileLines.isEmpty() -> StageStatus.FAILURE
            else -> if (envLines.isNotEmpty()) StageStatus.SUCCESS else StageStatus.PENDING
        }

        val compileStatus = when {
            isSuccess -> StageStatus.SUCCESS
            hasFailed -> StageStatus.FAILURE
            buildPhase == BuildPhase.IN_PROGRESS && compileLines.isNotEmpty() -> StageStatus.IN_PROGRESS
            else -> StageStatus.PENDING
        }

        val artifactStatus = when {
            isSuccess -> StageStatus.SUCCESS
            hasFailed -> StageStatus.FAILURE
            else -> StageStatus.PENDING
        }

        return listOf(
            BuildStageItem(
                key = "prep",
                title = "1. Leitura e Preparação do Projeto",
                subtitle = "Análise do arquivo ZIP e validação da estrutura Gradle",
                status = prepStatus,
                lines = prepLines.filter { it.isNotBlank() }
            ),
            BuildStageItem(
                key = "upload",
                title = "2. Envio do Código ao GitHub",
                subtitle = "Criação de blobs Git e commit na branch main",
                status = uploadStatus,
                lines = uploadLines.filter { it.isNotBlank() }
            ),
            BuildStageItem(
                key = "queue",
                title = "3. Enfileiramento no GitHub Actions",
                subtitle = "Alocação do runner Ubuntu em nuvem",
                status = queueStatus,
                lines = queueLines.filter { it.isNotBlank() }
            ),
            BuildStageItem(
                key = "env",
                title = "4. Configuração do Ambiente",
                subtitle = "Checkout do projeto, JDK 17 e Gradle Wrapper",
                status = envStatus,
                lines = envLines.filter { it.isNotBlank() }
            ),
            BuildStageItem(
                key = "compile",
                title = "5. Compilação do APK (assembleDebug)",
                subtitle = "Execução do Gradle, compilação Kotlin/Java e recursos",
                status = compileStatus,
                lines = compileLines.filter { it.isNotBlank() }
            ),
            BuildStageItem(
                key = "artifact",
                title = "6. Empacotamento do APK",
                subtitle = "Geração e upload do artefato final (.apk)",
                status = artifactStatus,
                lines = artifactLines.filter { it.isNotBlank() }
            )
        )
    }
}
