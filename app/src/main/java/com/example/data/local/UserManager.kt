package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class UserProfile(
    val username: String,       // e.g. "superadmin", "ajay_shukla"
    val fullName: String,       // e.g. "Ajay Shukla"
    val password: String,       // password
    val role: String = ROLE_STUDENT, // "SUPER_ADMIN" or "STUDENT"
    val bio: String = "",
    val isVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val handle: String
        get() = if (username.startsWith("@")) username else "@$username"

    val isSuperAdmin: Boolean
        get() = role == ROLE_SUPER_ADMIN

    companion object {
        const val ROLE_STUDENT = "STUDENT"
        const val ROLE_SUPER_ADMIN = "SUPER_ADMIN"
    }
}

class UserManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("narayan_users_store", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USERS_JSON = "registered_users_json"
        private const val KEY_ACTIVE_USER = "active_logged_in_user"

        val DEFAULT_SUPER_ADMIN = UserProfile(
            username = "superadmin",
            fullName = "Ajay Shukla",
            password = "admin123",
            role = UserProfile.ROLE_SUPER_ADMIN,
            bio = "Lead Educator & Platform Super Admin 🎓",
            isVerified = true
        )

        val DEFAULT_STUDENT = UserProfile(
            username = "student",
            fullName = "Narayan Student",
            password = "study123",
            role = UserProfile.ROLE_STUDENT,
            bio = "Lifelong Learner & Board Exam Candidate 📚",
            isVerified = false
        )
    }

    private val _usersList = MutableStateFlow<List<UserProfile>>(emptyList())
    val usersList: StateFlow<List<UserProfile>> = _usersList.asStateFlow()

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    init {
        loadUsers()
        loadActiveUser()
    }

    private fun loadUsers() {
        val jsonString = prefs.getString(KEY_USERS_JSON, null)
        val list = mutableListOf<UserProfile>()

        if (jsonString.isNullOrBlank()) {
            list.add(DEFAULT_SUPER_ADMIN)
            list.add(DEFAULT_STUDENT)
            saveUsersList(list)
        } else {
            try {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        UserProfile(
                            username = obj.getString("username"),
                            fullName = obj.getString("fullName"),
                            password = obj.getString("password"),
                            role = obj.optString("role", UserProfile.ROLE_STUDENT),
                            bio = obj.optString("bio", ""),
                            isVerified = obj.optBoolean("isVerified", false),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                // Ensure default superadmin always exists
                if (list.none { it.username.equals("superadmin", ignoreCase = true) }) {
                    list.add(0, DEFAULT_SUPER_ADMIN)
                    saveUsersList(list)
                }
            } catch (e: Exception) {
                list.add(DEFAULT_SUPER_ADMIN)
                list.add(DEFAULT_STUDENT)
            }
        }
        _usersList.value = list
    }

    private fun saveUsersList(list: List<UserProfile>) {
        val array = JSONArray()
        for (u in list) {
            val obj = JSONObject().apply {
                put("username", u.username)
                put("fullName", u.fullName)
                put("password", u.password)
                put("role", u.role)
                put("bio", u.bio)
                put("isVerified", u.isVerified)
                put("createdAt", u.createdAt)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_USERS_JSON, array.toString()).apply()
        _usersList.value = list
    }

    private fun loadActiveUser() {
        val activeUsername = prefs.getString(KEY_ACTIVE_USER, null)
        if (!activeUsername.isNullOrBlank()) {
            val user = _usersList.value.find { it.username.equals(activeUsername, ignoreCase = true) }
            _currentUser.value = user
        }
    }

    /**
     * Validates Instagram-style username format
     * Rules: 3-30 chars, only lowercase letters, numbers, periods, underscores.
     * Cannot start or end with a period. Cannot have consecutive periods.
     */
    fun validateInstagramUsername(rawInput: String): String? {
        val clean = rawInput.trim().removePrefix("@").lowercase()
        if (clean.length < 3) return "Username must be at least 3 characters."
        if (clean.length > 30) return "Username cannot exceed 30 characters."
        if (!clean.matches("^[a-z0-9._]+$".toRegex())) {
            return "Only letters, numbers, periods (.), and underscores (_) are allowed."
        }
        if (clean.startsWith(".") || clean.endsWith(".")) {
            return "Username cannot start or end with a period."
        }
        if (clean.contains("..")) {
            return "Username cannot contain consecutive periods."
        }
        if (_usersList.value.any { it.username.equals(clean, ignoreCase = true) }) {
            return "This username isn't available. Please try another."
        }
        return null // Valid!
    }

    /**
     * Instagram-style Registration (Sign Up)
     */
    fun registerUser(
        fullName: String,
        rawUsername: String,
        password: String,
        role: String = UserProfile.ROLE_STUDENT,
        bio: String = ""
    ): Result<UserProfile> {
        val cleanName = fullName.trim()
        if (cleanName.length < 2) {
            return Result.failure(Exception("Please enter your full name."))
        }

        val formatError = validateInstagramUsername(rawUsername)
        if (formatError != null) {
            return Result.failure(Exception(formatError))
        }

        if (password.length < 6) {
            return Result.failure(Exception("Password must be at least 6 characters."))
        }

        val cleanUsername = rawUsername.trim().removePrefix("@").lowercase()
        val isSuperAdmin = role == UserProfile.ROLE_SUPER_ADMIN || cleanUsername == "superadmin"

        val newUser = UserProfile(
            username = cleanUsername,
            fullName = cleanName,
            password = password,
            role = if (isSuperAdmin) UserProfile.ROLE_SUPER_ADMIN else UserProfile.ROLE_STUDENT,
            bio = bio.ifBlank { if (isSuperAdmin) "Super Admin & Educator 🎓" else "Student @ Narayan OS 📚" },
            isVerified = isSuperAdmin
        )

        val updated = _usersList.value.toMutableList()
        updated.add(newUser)
        saveUsersList(updated)

        // Automatically log in newly created user
        loginUser(newUser.username, password)
        return Result.success(newUser)
    }

    /**
     * Instagram-style Login
     * Supports logging in by username (@handle or plain username)
     */
    fun loginUser(identifier: String, pass: String): Result<UserProfile> {
        val cleanIdentifier = identifier.trim().removePrefix("@").lowercase()
        val user = _usersList.value.find {
            it.username.equals(cleanIdentifier, ignoreCase = true)
        }

        if (user == null) {
            // Also allow default super admin email
            if (cleanIdentifier == "ajayshukla60242@gmail.com") {
                val superAdmin = _usersList.value.find { it.username == "superadmin" }
                if (superAdmin != null && (pass == superAdmin.password || pass == "admin123" || pass == "Admin@123")) {
                    _currentUser.value = superAdmin
                    prefs.edit().putString(KEY_ACTIVE_USER, superAdmin.username).apply()
                    return Result.success(superAdmin)
                }
            }
            return Result.failure(Exception("The username you entered doesn't belong to an account."))
        }

        if (user.password != pass && !(user.username == "superadmin" && pass == "Admin@123")) {
            return Result.failure(Exception("Sorry, your password was incorrect. Please double-check your password."))
        }

        _currentUser.value = user
        prefs.edit().putString(KEY_ACTIVE_USER, user.username).apply()
        return Result.success(user)
    }

    /**
     * Firebase Google Sign-In session bridge
     */
    fun loginWithGoogleFirebase(email: String, displayName: String?, uid: String): UserProfile {
        val cleanName = displayName?.ifBlank { null } ?: email.substringBefore("@").ifBlank { "Google Student" }
        val baseHandle = email.substringBefore("@").lowercase().replace(Regex("[^a-z0-9_]"), "_").take(25)
            .ifBlank { "student_${uid.take(5)}" }

        val isSuperAdmin = email.equals("ajayshukla60242@gmail.com", ignoreCase = true) || baseHandle == "superadmin"

        val existing = _usersList.value.find {
            it.username.equals(baseHandle, ignoreCase = true)
        }

        val profile = if (existing != null) {
            existing.copy(
                fullName = cleanName,
                isVerified = isSuperAdmin || existing.isVerified,
                role = if (isSuperAdmin) UserProfile.ROLE_SUPER_ADMIN else existing.role
            )
        } else {
            UserProfile(
                username = baseHandle,
                fullName = cleanName,
                password = "", // Managed via Google Sign-In
                role = if (isSuperAdmin) UserProfile.ROLE_SUPER_ADMIN else UserProfile.ROLE_STUDENT,
                bio = if (isSuperAdmin) "Verified Super Admin & Lead Educator 🎓" else "Google Authenticated Learner 🎓",
                isVerified = true
            )
        }

        val updated = _usersList.value.filterNot { it.username.equals(profile.username, ignoreCase = true) }.toMutableList()
        updated.add(profile)
        saveUsersList(updated)

        _currentUser.value = profile
        prefs.edit().putString(KEY_ACTIVE_USER, profile.username).apply()
        return profile
    }

    fun logout() {
        _currentUser.value = null
        prefs.edit().remove(KEY_ACTIVE_USER).apply()
    }

    fun continueAsGuestStudent() {
        val guest = UserProfile(
            username = "guest_student",
            fullName = "Offline Student",
            password = "",
            role = UserProfile.ROLE_STUDENT,
            bio = "Offline Local Study Mode"
        )
        _currentUser.value = guest
        prefs.edit().putString(KEY_ACTIVE_USER, guest.username).apply()
    }
}
