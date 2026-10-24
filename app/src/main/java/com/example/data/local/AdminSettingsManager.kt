package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppAnnouncement(
    val isEnabled: Boolean = true,
    val title: String = "Narayan OS AI Study Announcement",
    val message: String = "Welcome to Narayan OS AI! Explore curated public lectures, playlists, and study tools."
)

class AdminSettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("narayan_admin_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ADMIN_ID = "admin_id"
        private const val KEY_ADMIN_PASSWORD = "admin_password"
        private const val KEY_ANNOUNCEMENT_ENABLED = "announcement_enabled"
        private const val KEY_ANNOUNCEMENT_TITLE = "announcement_title"
        private const val KEY_ANNOUNCEMENT_MESSAGE = "announcement_message"
        private const val KEY_CUSTOM_SUBJECTS = "custom_subjects"
        private const val KEY_APP_MOTTO = "app_motto"

        const val DEFAULT_ADMIN_ID = "superadmin"
        const val BACKUP_ADMIN_EMAIL = "ajayshukla60242@gmail.com"
        const val DEFAULT_ADMIN_PASSWORD = "admin123"
        const val DEFAULT_SUBJECTS = "Physics, Mathematics, Biology, Chemistry, History, Computer Science"
    }

    private val _adminIdFlow = MutableStateFlow(getAdminId())
    val adminIdFlow: StateFlow<String> = _adminIdFlow.asStateFlow()

    private val _announcementFlow = MutableStateFlow(getAnnouncement())
    val announcementFlow: StateFlow<AppAnnouncement> = _announcementFlow.asStateFlow()

    private val _subjectsFlow = MutableStateFlow(getSubjectsList())
    val subjectsFlow: StateFlow<List<String>> = _subjectsFlow.asStateFlow()

    private val _appMottoFlow = MutableStateFlow(getAppMotto())
    val appMottoFlow: StateFlow<String> = _appMottoFlow.asStateFlow()

    fun getAdminId(): String {
        return prefs.getString(KEY_ADMIN_ID, DEFAULT_ADMIN_ID) ?: DEFAULT_ADMIN_ID
    }

    private fun getAdminPassword(): String {
        return prefs.getString(KEY_ADMIN_PASSWORD, DEFAULT_ADMIN_PASSWORD) ?: DEFAULT_ADMIN_PASSWORD
    }

    fun verifyCredentials(inputAdminId: String, inputPass: String): Boolean {
        val currentId = getAdminId().trim().lowercase()
        val currentPass = getAdminPassword()
        val cleanedInputId = inputAdminId.trim().lowercase()

        // Match primary ID, backup admin email, or case-insensitive match
        val idMatches = cleanedInputId == currentId ||
                cleanedInputId == DEFAULT_ADMIN_ID.lowercase() ||
                cleanedInputId == BACKUP_ADMIN_EMAIL.lowercase()

        val passMatches = inputPass == currentPass || (currentPass == DEFAULT_ADMIN_PASSWORD && inputPass == "Admin@123")
        return idMatches && passMatches
    }

    fun changeAdminCredentials(oldPass: String, newId: String, newPass: String): Result<Unit> {
        val currentPass = getAdminPassword()
        if (oldPass != currentPass && !(currentPass == DEFAULT_ADMIN_PASSWORD && oldPass == "Admin@123")) {
            return Result.failure(Exception("Incorrect current password"))
        }
        val trimmedId = newId.trim()
        val trimmedPass = newPass.trim()
        if (trimmedId.length < 3) {
            return Result.failure(Exception("Admin ID must be at least 3 characters"))
        }
        if (trimmedPass.length < 4) {
            return Result.failure(Exception("Password must be at least 4 characters"))
        }

        prefs.edit()
            .putString(KEY_ADMIN_ID, trimmedId)
            .putString(KEY_ADMIN_PASSWORD, trimmedPass)
            .apply()

        _adminIdFlow.value = trimmedId
        return Result.success(Unit)
    }

    fun getAnnouncement(): AppAnnouncement {
        val enabled = prefs.getBoolean(KEY_ANNOUNCEMENT_ENABLED, true)
        val title = prefs.getString(KEY_ANNOUNCEMENT_TITLE, "Narayan OS AI Study Announcement")
            ?: "Narayan OS AI Study Announcement"
        val message = prefs.getString(KEY_ANNOUNCEMENT_MESSAGE, "Welcome to Narayan OS AI! Explore curated public lectures, playlists, and study tools.")
            ?: "Welcome to Narayan OS AI! Explore curated public lectures, playlists, and study tools."
        return AppAnnouncement(isEnabled = enabled, title = title, message = message)
    }

    fun updateAnnouncement(enabled: Boolean, title: String, message: String) {
        prefs.edit()
            .putBoolean(KEY_ANNOUNCEMENT_ENABLED, enabled)
            .putString(KEY_ANNOUNCEMENT_TITLE, title.trim())
            .putString(KEY_ANNOUNCEMENT_MESSAGE, message.trim())
            .apply()

        _announcementFlow.value = AppAnnouncement(
            isEnabled = enabled,
            title = title.trim(),
            message = message.trim()
        )
    }

    fun getSubjectsList(): List<String> {
        val raw = prefs.getString(KEY_CUSTOM_SUBJECTS, DEFAULT_SUBJECTS) ?: DEFAULT_SUBJECTS
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun updateSubjects(subjects: List<String>) {
        val joined = subjects.joinToString(", ")
        prefs.edit().putString(KEY_CUSTOM_SUBJECTS, joined).apply()
        _subjectsFlow.value = subjects
    }

    fun addSubject(subject: String): Boolean {
        val current = getSubjectsList().toMutableList()
        val trimmed = subject.trim()
        if (trimmed.isEmpty() || current.any { it.equals(trimmed, ignoreCase = true) }) {
            return false
        }
        current.add(trimmed)
        updateSubjects(current)
        return true
    }

    fun getAppMotto(): String {
        return prefs.getString(KEY_APP_MOTTO, "Your Personal Intelligent Study Assistant")
            ?: "Your Personal Intelligent Study Assistant"
    }

    fun updateAppMotto(motto: String) {
        val trimmed = motto.trim()
        prefs.edit().putString(KEY_APP_MOTTO, trimmed).apply()
        _appMottoFlow.value = trimmed
    }
}
