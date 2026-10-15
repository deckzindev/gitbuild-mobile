package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64

/**
 * Manages secure local persistence for GitHub credentials and build preferences.
 */
class TokenStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("gitbuilder_secure_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "enc_github_token"
        private const val KEY_USERNAME = "github_username"
        private const val KEY_USER_NAME = "github_user_name"
        private const val KEY_AVATAR = "github_avatar_url"
        private const val KEY_REPO = "github_target_repo"
        const val DEFAULT_REPO = "android-apk-builder"
    }

    fun saveToken(token: String) {
        val encoded = Base64.encodeToString(token.trim().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        prefs.edit().putString(KEY_TOKEN, encoded).apply()
    }

    fun getToken(): String? {
        val encoded = prefs.getString(KEY_TOKEN, null) ?: return null
        return try {
            String(Base64.decode(encoded, Base64.NO_WRAP), Charsets.UTF_8).trim()
        } catch (e: Exception) {
            null
        }
    }

    fun saveUserInfo(user: GitHubUser) {
        prefs.edit()
            .putString(KEY_USERNAME, user.login)
            .putString(KEY_USER_NAME, user.name ?: user.login)
            .putString(KEY_AVATAR, user.avatarUrl)
            .apply()
    }

    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)
    fun getUserDisplayName(): String? = prefs.getString(KEY_USER_NAME, null)
    fun getAvatarUrl(): String? = prefs.getString(KEY_AVATAR, null)

    fun saveTargetRepo(repoName: String) {
        val cleanName = repoName.trim().ifEmpty { DEFAULT_REPO }
        prefs.edit().putString(KEY_REPO, cleanName).apply()
    }

    fun getTargetRepo(): String = prefs.getString(KEY_REPO, DEFAULT_REPO) ?: DEFAULT_REPO

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    fun hasToken(): Boolean = !getToken().isNullOrBlank()
}
