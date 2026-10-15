package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubUser(
    val login: String,
    val id: Long? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    val name: String? = null,
    @Json(name = "html_url") val htmlUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubRepo(
    val name: String,
    @Json(name = "full_name") val fullName: String,
    val private: Boolean = true,
    @Json(name = "html_url") val htmlUrl: String? = null,
    @Json(name = "default_branch") val defaultBranch: String? = "main"
)

@JsonClass(generateAdapter = true)
data class WorkflowRun(
    val id: Long,
    val name: String? = null,
    val status: String? = null, // "queued", "in_progress", "completed"
    val conclusion: String? = null, // "success", "failure", "neutral", "cancelled", "timed_out", "action_required"
    @Json(name = "html_url") val htmlUrl: String? = null,
    @Json(name = "head_sha") val headSha: String? = null,
    @Json(name = "run_number") val runNumber: Int? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class WorkflowRunsResponse(
    @Json(name = "total_count") val totalCount: Int = 0,
    @Json(name = "workflow_runs") val workflowRuns: List<WorkflowRun> = emptyList()
)

@JsonClass(generateAdapter = true)
data class JobStep(
    val name: String,
    val status: String? = null, // "queued", "in_progress", "completed"
    val conclusion: String? = null, // "success", "failure", "skipped"
    val number: Int = 0
)

@JsonClass(generateAdapter = true)
data class WorkflowJob(
    val id: Long,
    @Json(name = "run_id") val runId: Long,
    val name: String,
    val status: String? = null,
    val conclusion: String? = null,
    val steps: List<JobStep> = emptyList()
)

@JsonClass(generateAdapter = true)
data class WorkflowJobsResponse(
    @Json(name = "total_count") val totalCount: Int = 0,
    val jobs: List<WorkflowJob> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ArtifactItem(
    val id: Long,
    val name: String,
    @Json(name = "size_in_bytes") val sizeInBytes: Long = 0,
    @Json(name = "archive_download_url") val archiveDownloadUrl: String,
    val expired: Boolean = false
)

@JsonClass(generateAdapter = true)
data class ArtifactsResponse(
    @Json(name = "total_count") val totalCount: Int = 0,
    val artifacts: List<ArtifactItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitBlobResponse(
    val sha: String,
    val url: String? = null
)

@JsonClass(generateAdapter = true)
data class GitTreeResponse(
    val sha: String,
    val url: String? = null
)

@JsonClass(generateAdapter = true)
data class GitCommitResponse(
    val sha: String,
    val url: String? = null
)

@JsonClass(generateAdapter = true)
data class GitRefObject(
    val sha: String,
    val type: String? = null,
    val url: String? = null
)

@JsonClass(generateAdapter = true)
data class GitRefResponse(
    val ref: String,
    @Json(name = "object") val refObject: GitRefObject? = null
)
