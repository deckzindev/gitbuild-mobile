package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class ZipProjectFile(
    val path: String,
    val bytes: ByteArray,
    val size: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ZipProjectFile
        return path == other.path
    }

    override fun hashCode(): Int = path.hashCode()
}

data class ProjectAnalysis(
    val fileName: String,
    val fileSizeBytes: Long,
    val totalFilesCount: Int,
    val isAndroidProject: Boolean,
    val hasGradleWrapper: Boolean,
    val hasWorkflow: Boolean,
    val detectedBuildFiles: List<String>,
    val files: List<ZipProjectFile>
)

object ZipHelper {

    private val IGNORED_PREFIXES = listOf(
        "__MACOSX/",
        ".git/",
        ".gradle/",
        ".idea/",
        ".fleet/",
        ".vscode/",
        "build/",
        "app/build/"
    )

    private val IGNORED_FILENAMES = listOf(
        ".DS_Store",
        "Thumbs.db",
        "desktop.ini"
    )

    fun readZipFromUri(context: Context, uri: Uri): Result<ProjectAnalysis> = runCatching {
        var displayName = "projeto.zip"
        var fileSize = 0L

        // Query file metadata from ContentResolver
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) displayName = cursor.getString(nameIndex) ?: displayName
                if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
            }
        }

        val rawEntries = mutableListOf<Pair<String, ByteArray>>()

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            readZipStream(inputStream, rawEntries)
        } ?: throw IllegalStateException("Não foi possível abrir o arquivo ZIP selecionado.")

        if (rawEntries.isEmpty()) {
            throw IllegalStateException("O arquivo ZIP está vazio ou não contém arquivos legíveis.")
        }

        // Determine if all files share a common single root folder (e.g. MyProject/...)
        val normalizedEntries = normalizeEntryPaths(rawEntries)

        // Project inspection
        val detectedBuildFiles = mutableListOf<String>()
        var hasGradleWrapper = false
        var hasWorkflow = false

        normalizedEntries.forEach { file ->
            val path = file.path
            val lower = path.lowercase()
            if (lower == "settings.gradle" || lower == "settings.gradle.kts" ||
                lower == "build.gradle" || lower == "build.gradle.kts" ||
                lower == "app/build.gradle" || lower == "app/build.gradle.kts" ||
                lower.endsWith("/build.gradle") || lower.endsWith("/build.gradle.kts")
            ) {
                detectedBuildFiles.add(path)
            }
            if (path == "gradlew" || path == "gradle/wrapper/gradle-wrapper.jar") {
                hasGradleWrapper = true
            }
            if (path.startsWith(".github/workflows/") && (path.endsWith(".yml") || path.endsWith(".yaml"))) {
                hasWorkflow = true
            }
        }

        val isAndroidProject = detectedBuildFiles.isNotEmpty() ||
                normalizedEntries.any { it.path.contains("AndroidManifest.xml", ignoreCase = true) }

        ProjectAnalysis(
            fileName = displayName,
            fileSizeBytes = if (fileSize > 0) fileSize else normalizedEntries.sumOf { it.size },
            totalFilesCount = normalizedEntries.size,
            isAndroidProject = isAndroidProject,
            hasGradleWrapper = hasGradleWrapper,
            hasWorkflow = hasWorkflow,
            detectedBuildFiles = detectedBuildFiles,
            files = normalizedEntries
        )
    }

    private fun readZipStream(
        inputStream: InputStream,
        destination: MutableList<Pair<String, ByteArray>>
    ) {
        val zipIn = ZipInputStream(inputStream)
        var entry: ZipEntry? = zipIn.nextEntry

        val buffer = ByteArray(8192)

        while (entry != null) {
            val name = entry.name.replace('\\', '/')
            val isDirectory = entry.isDirectory || name.endsWith('/')

            if (!isDirectory) {
                val shouldIgnore = IGNORED_PREFIXES.any { name.startsWith(it) || name.contains("/$it") } ||
                        IGNORED_FILENAMES.any { name.endsWith("/$it") || name == it }

                if (!shouldIgnore) {
                    val byteOutput = ByteArrayOutputStream()
                    var bytesRead: Int
                    while (zipIn.read(buffer).also { bytesRead = it } != -1) {
                        byteOutput.write(buffer, 0, bytesRead)
                    }
                    destination.add(Pair(name, byteOutput.toByteArray()))
                }
            }
            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }
    }

    private fun normalizeEntryPaths(rawEntries: List<Pair<String, ByteArray>>): List<ZipProjectFile> {
        if (rawEntries.isEmpty()) return emptyList()

        // Check if all entries share a single root folder (e.g., "my-app-master/...")
        val firstSlashIndices = rawEntries.map { it.first.indexOf('/') }
        val allHaveSlash = firstSlashIndices.all { it > 0 }

        val commonRootPrefix: String? = if (allHaveSlash) {
            val candidate = rawEntries.first().first.substring(0, firstSlashIndices.first() + 1)
            val allShareRoot = rawEntries.all { it.first.startsWith(candidate) }
            if (allShareRoot) candidate else null
        } else {
            null
        }

        return rawEntries.map { (originalPath, bytes) ->
            val cleanPath = if (commonRootPrefix != null && originalPath.startsWith(commonRootPrefix)) {
                originalPath.removePrefix(commonRootPrefix)
            } else {
                originalPath.trimStart('/')
            }
            ZipProjectFile(
                path = cleanPath,
                bytes = bytes,
                size = bytes.size.toLong()
            )
        }.filter { it.path.isNotBlank() }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(java.util.Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(java.util.Locale.US, "%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format(java.util.Locale.US, "%.2f GB", gb)
    }
}
