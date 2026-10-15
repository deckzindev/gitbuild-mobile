package com.example.data

import android.util.Base64
import com.example.util.WorkflowGenerator
import com.example.util.ZipProjectFile
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

class GitHubApiClient {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun baseRequestBuilder(token: String, url: String): Request.Builder {
        return Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${token.trim()}")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
    }

    suspend fun verifyToken(token: String): Result<GitHubUser> = withContext(Dispatchers.IO) {
        runCatching {
            val request = baseRequestBuilder(token, "https://api.github.com/user").build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    when (response.code) {
                        401 -> throw IllegalStateException("Token inválido ou expirado. Verifique seu token pessoal do GitHub.")
                        403 -> throw IllegalStateException("Acesso não autorizado. Verifique os escopos do token ('repo').")
                        else -> throw IllegalStateException("Erro ao verificar token GitHub (HTTP ${response.code}): $body")
                    }
                }
                val adapter = moshi.adapter(GitHubUser::class.java)
                adapter.fromJson(body) ?: throw IllegalStateException("Falha ao processar perfil do GitHub.")
            }
        }
    }

    suspend fun getOrCreateRepo(token: String, owner: String, repoName: String): Result<GitHubRepo> = withContext(Dispatchers.IO) {
        runCatching {
            val checkUrl = "https://api.github.com/repos/$owner/$repoName"
            val checkRequest = baseRequestBuilder(token, checkUrl).build()

            client.newCall(checkRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    val adapter = moshi.adapter(GitHubRepo::class.java)
                    return@runCatching adapter.fromJson(body)
                        ?: throw IllegalStateException("Falha ao analisar repositório existente.")
                }
            }

            // Create private repo if not found
            val createUrl = "https://api.github.com/user/repos"
            val payload = JSONObject().apply {
                put("name", repoName)
                put("description", "Repositório privado gerenciado pelo GitBuilder para compilações de APK")
                put("private", true)
                put("auto_init", true)
            }
            val createRequest = baseRequestBuilder(token, createUrl)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(createRequest).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IllegalStateException("Erro ao criar repositório '$repoName' (HTTP ${response.code}): $body")
                }
                val adapter = moshi.adapter(GitHubRepo::class.java)
                adapter.fromJson(body) ?: throw IllegalStateException("Falha ao analisar repositório criado.")
            }
        }
    }

    suspend fun createRandomRepo(token: String, repoName: String): Result<GitHubRepo> = withContext(Dispatchers.IO) {
        runCatching {
            val createUrl = "https://api.github.com/user/repos"
            val payload = JSONObject().apply {
                put("name", repoName)
                put("description", "Repositório para compilação gerado pelo GitBuilder")
                put("private", true)
                put("auto_init", true)
            }
            val createRequest = baseRequestBuilder(token, createUrl)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(createRequest).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IllegalStateException("Erro ao criar repositório '$repoName' (HTTP ${response.code}): $body")
                }
                val adapter = moshi.adapter(GitHubRepo::class.java)
                adapter.fromJson(body) ?: throw IllegalStateException("Falha ao processar repositório criado.")
            }
        }
    }

    suspend fun uploadProjectAndTriggerBuild(
        token: String,
        owner: String,
        repo: String,
        projectFiles: List<ZipProjectFile>,
        onProgress: (status: String, current: Int, total: Int) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val allFiles = projectFiles.toMutableList()

            // Check if workflow exists, otherwise inject our proven Android build workflow
            val hasWorkflow = allFiles.any {
                it.path.startsWith(".github/workflows/") && (it.path.endsWith(".yml") || it.path.endsWith(".yaml"))
            }
            if (!hasWorkflow) {
                val workflowYaml = WorkflowGenerator.generateAndroidBuildWorkflow()
                allFiles.add(
                    ZipProjectFile(
                        path = WorkflowGenerator.DEFAULT_WORKFLOW_PATH,
                        bytes = workflowYaml.toByteArray(Charsets.UTF_8),
                        size = workflowYaml.length.toLong()
                    )
                )
            }

            val totalFiles = allFiles.size
            onProgress("Preparando envio de arquivos...", 0, totalFiles)

            // Step 1: Upload blobs with limited concurrency (batches of 6)
            val treeEntries = mutableListOf<JSONObject>()
            var uploadedCount = 0

            val batches = allFiles.chunked(6)
            for (batch in batches) {
                val deferredBlobs = batch.map { file ->
                    async(Dispatchers.IO) {
                        val base64Content = Base64.encodeToString(file.bytes, Base64.NO_WRAP)
                        val blobPayload = JSONObject().apply {
                            put("content", base64Content)
                            put("encoding", "base64")
                        }
                        val request = baseRequestBuilder(token, "https://api.github.com/repos/$owner/$repo/git/blobs")
                            .post(blobPayload.toString().toRequestBody(jsonMediaType))
                            .build()

                        client.newCall(request).execute().use { response ->
                            val body = response.body?.string().orEmpty()
                            if (!response.isSuccessful) {
                                throw IllegalStateException("Falha ao enviar arquivo '${file.path}' (HTTP ${response.code}): $body")
                            }
                            val blobObj = JSONObject(body)
                            val sha = blobObj.getString("sha")

                            val isExecutable = file.path == "gradlew" || file.path.endsWith(".sh")
                            JSONObject().apply {
                                put("path", file.path)
                                put("mode", if (isExecutable) "100755" else "100644")
                                put("type", "blob")
                                put("sha", sha)
                            }
                        }
                    }
                }

                val results = deferredBlobs.awaitAll()
                treeEntries.addAll(results)
                uploadedCount += results.size
                onProgress("Enviando código-fonte para o GitHub ($uploadedCount/$totalFiles)...", uploadedCount, totalFiles)
            }

            // Step 2: Create Tree
            onProgress("Criando árvore Git no repositório...", uploadedCount, totalFiles)
            val treePayload = JSONObject().apply {
                put("tree", JSONArray(treeEntries))
            }

            val treeRequest = baseRequestBuilder(token, "https://api.github.com/repos/$owner/$repo/git/trees")
                .post(treePayload.toString().toRequestBody(jsonMediaType))
                .build()

            val treeSha = client.newCall(treeRequest).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IllegalStateException("Erro ao criar árvore Git (HTTP ${response.code}): $body")
                }
                JSONObject(body).getString("sha")
            }

            // Step 3: Get parent commit if main branch exists
            val parentSha = getBranchCommitSha(token, owner, repo, "main")
                ?: getBranchCommitSha(token, owner, repo, "master")

            // Step 4: Create Commit
            onProgress("Criando commit de compilação...", uploadedCount, totalFiles)
            val timeStamp = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
            val commitPayload = JSONObject().apply {
                put("message", "GitBuilder: Compilação Android iniciada em $timeStamp")
                put("tree", treeSha)
                if (parentSha != null) {
                    put("parents", JSONArray().put(parentSha))
                } else {
                    put("parents", JSONArray())
                }
            }

            val commitRequest = baseRequestBuilder(token, "https://api.github.com/repos/$owner/$repo/git/commits")
                .post(commitPayload.toString().toRequestBody(jsonMediaType))
                .build()

            val commitSha = client.newCall(commitRequest).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IllegalStateException("Erro ao criar commit (HTTP ${response.code}): $body")
                }
                JSONObject(body).getString("sha")
            }

            // Step 5: Update or create branch ref
            onProgress("Disparando GitHub Actions na branch main...", uploadedCount, totalFiles)
            updateOrCreateBranchRef(token, owner, repo, "main", commitSha)

            commitSha
        }
    }

    private fun getBranchCommitSha(token: String, owner: String, repo: String, branch: String): String? {
        val url = "https://api.github.com/repos/$owner/$repo/git/ref/heads/$branch"
        val request = baseRequestBuilder(token, url).build()
        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    JSONObject(body).getJSONObject("object").getString("sha")
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun updateOrCreateBranchRef(token: String, owner: String, repo: String, branch: String, commitSha: String) {
        val patchUrl = "https://api.github.com/repos/$owner/$repo/git/refs/heads/$branch"
        val patchPayload = JSONObject().apply {
            put("sha", commitSha)
            put("force", true)
        }
        val patchRequest = baseRequestBuilder(token, patchUrl)
            .patch(patchPayload.toString().toRequestBody(jsonMediaType))
            .build()

        var updated = false
        try {
            client.newCall(patchRequest).execute().use { response ->
                if (response.isSuccessful) updated = true
            }
        } catch (_: Exception) {}

        if (!updated) {
            val postUrl = "https://api.github.com/repos/$owner/$repo/git/refs"
            val postPayload = JSONObject().apply {
                put("ref", "refs/heads/$branch")
                put("sha", commitSha)
            }
            val postRequest = baseRequestBuilder(token, postUrl)
                .post(postPayload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(postRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    throw IllegalStateException("Não foi possível atualizar a branch $branch (HTTP ${response.code}): $body")
                }
            }
        }
    }

    suspend fun findWorkflowRunForCommit(
        token: String,
        owner: String,
        repo: String,
        commitSha: String
    ): Result<WorkflowRun?> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://api.github.com/repos/$owner/$repo/actions/runs?head_sha=$commitSha&per_page=5"
            val request = baseRequestBuilder(token, url).build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@runCatching null
                }
                val adapter = moshi.adapter(WorkflowRunsResponse::class.java)
                val runsResponse = adapter.fromJson(body)
                runsResponse?.workflowRuns?.firstOrNull()
            }
        }
    }

    suspend fun getWorkflowRun(
        token: String,
        owner: String,
        repo: String,
        runId: Long
    ): Result<WorkflowRun> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://api.github.com/repos/$owner/$repo/actions/runs/$runId"
            val request = baseRequestBuilder(token, url).build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IllegalStateException("Erro ao obter status da execução $runId (HTTP ${response.code}): $body")
                }
                val adapter = moshi.adapter(WorkflowRun::class.java)
                adapter.fromJson(body) ?: throw IllegalStateException("Falha ao analisar WorkflowRun.")
            }
        }
    }

    suspend fun getWorkflowJobs(
        token: String,
        owner: String,
        repo: String,
        runId: Long
    ): Result<List<WorkflowJob>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://api.github.com/repos/$owner/$repo/actions/runs/$runId/jobs"
            val request = baseRequestBuilder(token, url).build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@runCatching emptyList()
                }
                val adapter = moshi.adapter(WorkflowJobsResponse::class.java)
                adapter.fromJson(body)?.jobs ?: emptyList()
            }
        }
    }

    suspend fun getJobLogs(
        token: String,
        owner: String,
        repo: String,
        jobId: Long
    ): Result<String?> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://api.github.com/repos/$owner/$repo/actions/jobs/$jobId/logs"
            val request = baseRequestBuilder(token, url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    // 404 is normal while GitHub Actions is still executing the job
                    return@runCatching null
                }
                response.body?.string()
            }
        }
    }

    suspend fun getArtifacts(
        token: String,
        owner: String,
        repo: String,
        runId: Long
    ): Result<List<ArtifactItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://api.github.com/repos/$owner/$repo/actions/runs/$runId/artifacts"
            val request = baseRequestBuilder(token, url).build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@runCatching emptyList()
                }
                val adapter = moshi.adapter(ArtifactsResponse::class.java)
                adapter.fromJson(body)?.artifacts ?: emptyList()
            }
        }
    }

    suspend fun downloadArtifactZip(
        token: String,
        downloadUrl: String,
        destinationFile: File,
        onProgress: (downloaded: Long, total: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val request = baseRequestBuilder(token, downloadUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Erro ao baixar artefato (HTTP ${response.code})")
                }
                val body = response.body ?: throw IllegalStateException("Corpo da resposta vazio ao baixar artefato.")
                val totalLength = body.contentLength()
                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(destinationFile)

                val buffer = ByteArray(8192)
                var downloaded = 0L
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                    downloaded += read
                    onProgress(downloaded, totalLength)
                }
                outputStream.flush()
                outputStream.close()
                inputStream.close()

                destinationFile
            }
        }
    }

    fun extractApkFromArtifactZip(zipFile: File, outputDirectory: File): Result<File?> = runCatching {
        if (!outputDirectory.exists()) outputDirectory.mkdirs()

        var extractedApk: File? = null
        ZipInputStream(zipFile.inputStream()).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                val name = entry.name.replace('\\', '/')
                if (!entry.isDirectory && name.lowercase().endsWith(".apk")) {
                    val apkFileName = name.substringAfterLast('/')
                    val targetApk = File(outputDirectory, apkFileName)
                    FileOutputStream(targetApk).use { fos ->
                        zipIn.copyTo(fos)
                    }
                    extractedApk = targetApk
                    break
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
        extractedApk
    }
}
