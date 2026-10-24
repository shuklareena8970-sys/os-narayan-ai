package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiClient
import com.example.data.local.AdminSettingsManager
import com.example.data.local.AlarmItem
import com.example.data.local.AppAnnouncement
import com.example.data.local.AppDatabase
import com.example.data.local.ChessRecord
import com.example.data.local.Note
import com.example.data.local.Playlist
import com.example.data.local.SavedSelfie
import com.example.data.local.SelfieManager
import com.example.data.local.StudySession
import com.example.data.local.UserManager
import com.example.data.local.UserProfile
import com.example.data.local.VideoRecord
import com.example.data.sync.SyncManager
import com.example.data.sync.SyncQueueItem
import com.example.data.sync.SyncUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatMessage(
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false
)

enum class Screen {
    HOME,
    SELFIE,
    AI_ASSISTANT,
    LECTURES,
    LECTURE_DETAIL,
    MY_NOTES,
    PUBLISH_VIDEO,
    STUDY_TIMER,
    ALARM,
    YOUTUBE_PLAYER,
    SUPER_ADMIN,
    CHESS
}

class NarayanViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val noteDao = db.noteDao()
    private val videoDao = db.videoRecordDao()
    private val timerAlarmDao = db.timerAndAlarmDao()
    private val chessDao = db.chessRecordDao()
    private val playlistDao = db.playlistDao()
    private val selfieManager = SelfieManager(application)
    val adminSettings = AdminSettingsManager(application)
    val userManager = UserManager(application)
    val syncManager = SyncManager(application, db.syncQueueDao())

    val currentUserProfile: StateFlow<UserProfile?> = userManager.currentUser

    val syncUiState: StateFlow<SyncUiState> = syncManager.syncUiState

    fun triggerSync() {
        syncManager.triggerManualSync()
    }

    val chessStats: StateFlow<ChessRecord?> = chessDao.getStats()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun recordChessGameResult(isPlayerWin: Boolean, isAiWin: Boolean, score: Int) {
        viewModelScope.launch {
            val current = chessDao.getStatsDirect()
            if (current == null) {
                chessDao.insert(
                    ChessRecord(
                        playerWins = if (isPlayerWin) 1 else 0,
                        aiWins = if (isAiWin) 1 else 0,
                        draws = if (!isPlayerWin && !isAiWin) 1 else 0,
                        bestScore = score,
                        lastPlayedAt = System.currentTimeMillis()
                    )
                )
            } else {
                chessDao.update(
                    current.copy(
                        playerWins = current.playerWins + (if (isPlayerWin) 1 else 0),
                        aiWins = current.aiWins + (if (isAiWin) 1 else 0),
                        draws = current.draws + (if (!isPlayerWin && !isAiWin) 1 else 0),
                        bestScore = maxOf(current.bestScore, score),
                        lastPlayedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    val chessOnlineManager = com.example.data.chess.ChessOnlineManager(application)
    private val _onlineRoomState = MutableStateFlow<com.example.data.chess.OnlineChessRoom?>(null)
    val onlineRoomState: StateFlow<com.example.data.chess.OnlineChessRoom?> = _onlineRoomState.asStateFlow()

    private var onlineRoomJob: Job? = null

    fun createOnlineChessRoom(asWhite: Boolean, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val user = currentUserProfile.value
        val userId = user?.username ?: "guest_${System.currentTimeMillis() % 10000}"
        val displayName = user?.fullName ?: "Player"
        chessOnlineManager.createRoom(
            userId = userId,
            username = displayName,
            asWhite = asWhite,
            onSuccess = { room ->
                _onlineRoomState.value = room
                subscribeToOnlineRoom(room.roomId)
                onSuccess(room.roomId)
            },
            onError = onError
        )
    }

    fun joinOnlineChessRoom(code: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = currentUserProfile.value
        val userId = user?.username ?: "guest_${System.currentTimeMillis() % 10000}"
        val displayName = user?.fullName ?: "Player"
        chessOnlineManager.joinRoom(
            roomCodeInput = code,
            userId = userId,
            username = displayName,
            onSuccess = { room ->
                _onlineRoomState.value = room
                subscribeToOnlineRoom(room.roomId)
                onSuccess()
            },
            onError = onError
        )
    }

    fun subscribeToOnlineRoom(roomId: String) {
        onlineRoomJob?.cancel()
        onlineRoomJob = viewModelScope.launch {
            chessOnlineManager.observeRoom(roomId).collect { room ->
                _onlineRoomState.value = room
            }
        }
    }

    fun leaveOnlineRoom() {
        onlineRoomJob?.cancel()
        onlineRoomJob = null
        _onlineRoomState.value = null
    }

    fun transmitOnlineMove(
        roomId: String,
        fromRow: Int,
        fromCol: Int,
        toRow: Int,
        toCol: Int,
        nextTurn: String,
        moveCount: Int,
        isCheckmate: Boolean = false,
        winnerColor: String? = null
    ) {
        chessOnlineManager.transmitMove(
            roomId = roomId,
            fromRow = fromRow,
            fromCol = fromCol,
            toRow = toRow,
            toCol = toCol,
            nextTurn = nextTurn,
            newMoveCount = moveCount,
            isCheckmate = isCheckmate,
            winnerColor = winnerColor
        )
    }

    fun resignOnlineMatch(roomId: String, playerColor: String) {
        chessOnlineManager.resignMatch(roomId, playerColor)
    }

    fun sendOnlineReaction(roomId: String, reaction: String) {
        chessOnlineManager.sendReaction(roomId, reaction)
    }

    fun resetOnlineMatch(roomId: String) {
        chessOnlineManager.resetOnlineMatch(roomId)
    }


    // Current navigation screen
    private val _currentScreen = MutableStateFlow(Screen.HOME)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun navigateBack(): Boolean {
        return if (_currentScreen.value != Screen.HOME) {
            _currentScreen.value = Screen.HOME
            true
        } else {
            false
        }
    }

    // ==========================================
    // NOTES STATE & ACTIONS
    // ==========================================
    private val _notesSearchQuery = MutableStateFlow("")
    val notesSearchQuery: StateFlow<String> = _notesSearchQuery.asStateFlow()

    private val _notesSelectedCategory = MutableStateFlow("All")
    val notesSelectedCategory: StateFlow<String> = _notesSelectedCategory.asStateFlow()

    val notes: StateFlow<List<Note>> = combine(
        noteDao.getAllNotes(),
        _notesSearchQuery,
        _notesSelectedCategory
    ) { list, query, category ->
        list.filter { note ->
            val matchesQuery = query.isBlank() ||
                note.title.contains(query, ignoreCase = true) ||
                note.content.contains(query, ignoreCase = true) ||
                note.subject.contains(query, ignoreCase = true)
            val matchesCategory = category == "All" || note.subject.equals(category, ignoreCase = true)
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setNotesSearchQuery(q: String) {
        _notesSearchQuery.value = q
    }

    fun setNotesCategory(cat: String) {
        _notesSelectedCategory.value = cat
    }

    fun saveNote(title: String, content: String, subject: String, existingId: Long = 0) {
        viewModelScope.launch {
            val noteId = if (existingId > 0) {
                noteDao.updateNote(
                    Note(
                        id = existingId,
                        title = title.trim(),
                        content = content.trim(),
                        subject = subject.trim(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
                existingId
            } else {
                noteDao.insertNote(
                    Note(
                        title = title.trim(),
                        content = content.trim(),
                        subject = subject.trim(),
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            // Enqueue into persistent offline sync queue
            syncManager.queueOperation(
                entityType = "NOTE",
                entityId = noteId.toString(),
                operation = if (existingId > 0) SyncQueueItem.OP_UPDATE else SyncQueueItem.OP_CREATE,
                payloadJson = "{\"title\":\"${title.trim()}\",\"subject\":\"${subject.trim()}\"}"
            )
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteDao.deleteNote(note)
            syncManager.queueOperation(
                entityType = "NOTE",
                entityId = note.id.toString(),
                operation = SyncQueueItem.OP_DELETE,
                payloadJson = "{\"title\":\"${note.title}\"}"
            )
        }
    }

    // ==========================================
    // SELFIE STATE & ACTIONS
    // ==========================================
    private val _savedSelfies = MutableStateFlow<List<SavedSelfie>>(emptyList())
    val savedSelfies: StateFlow<List<SavedSelfie>> = _savedSelfies.asStateFlow()

    private val _selfieStatusMessage = MutableStateFlow<String?>(null)
    val selfieStatusMessage: StateFlow<String?> = _selfieStatusMessage.asStateFlow()

    fun loadSelfies() {
        viewModelScope.launch {
            _savedSelfies.value = selfieManager.getSavedSelfies()
        }
    }

    fun saveSelfieBitmap(bitmap: Bitmap, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = selfieManager.saveBitmap(bitmap)
            result.onSuccess {
                _selfieStatusMessage.value = "Selfie saved to private app storage!"
                loadSelfies()
                onComplete(true)
            }.onFailure {
                _selfieStatusMessage.value = "Failed to save: ${it.message}"
                onComplete(false)
            }
        }
    }

    fun saveSelfieUri(uri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = selfieManager.saveFromUri(uri)
            result.onSuccess {
                _selfieStatusMessage.value = "Photo saved to private app storage!"
                loadSelfies()
                onComplete(true)
            }.onFailure {
                _selfieStatusMessage.value = "Failed to save: ${it.message}"
                onComplete(false)
            }
        }
    }

    fun deleteSelfie(selfie: SavedSelfie) {
        viewModelScope.launch {
            selfieManager.deleteSelfie(selfie.file)
            loadSelfies()
        }
    }

    fun clearSelfieStatusMessage() {
        _selfieStatusMessage.value = null
    }

    // ==========================================
    // AI ASSISTANT STATE & ACTIONS
    // ==========================================
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "ai",
                text = "Hello! I am your Narayan OS AI Study Assistant. Ask me any school or college question in English or हिंदी (Physics, Math, Chemistry, History, etc.)."
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _aiErrorMessage = MutableStateFlow<String?>(null)
    val aiErrorMessage: StateFlow<String?> = _aiErrorMessage.asStateFlow()

    fun sendAiQuestion(question: String) {
        val trimmed = question.trim()
        if (trimmed.isBlank() || _isAiLoading.value) return

        _aiErrorMessage.value = null
        val userMsg = ChatMessage(sender = "user", text = trimmed)
        _chatMessages.value = _chatMessages.value + userMsg

        // Build history for conversational follow-ups
        val history = mutableListOf<Pair<String, String>>()
        val currentMsgs = _chatMessages.value
        for (i in 0 until currentMsgs.size - 1 step 2) {
            val u = currentMsgs.getOrNull(i)
            val a = currentMsgs.getOrNull(i + 1)
            if (u != null && a != null && u.sender == "user" && a.sender == "ai") {
                history.add(Pair(u.text, a.text))
            }
        }

        viewModelScope.launch {
            _isAiLoading.value = true
            val result = GeminiClient.askAssistant(trimmed, history)
            _isAiLoading.value = false
            result.onSuccess { reply ->
                _chatMessages.value = _chatMessages.value + ChatMessage(sender = "ai", text = reply)
            }.onFailure { error ->
                _aiErrorMessage.value = error.message ?: "Failed to get AI response"
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    sender = "ai",
                    text = "⚠️ Error: ${error.message}\n(Please verify your GEMINI_API_KEY in Google AI Studio Secrets).",
                    isError = true
                )
            }
        }
    }

    fun clearChat() {
        _aiErrorMessage.value = null
        _chatMessages.value = listOf(
            ChatMessage(
                sender = "ai",
                text = "New conversation started! Ask me any study question."
            )
        )
    }

    // ==========================================
    // LECTURES & VIDEO PUBLISHING & ADMIN
    // ==========================================
    val publishedLectures: StateFlow<List<VideoRecord>> = videoDao.getPublishedVideos()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val userPrivateVideos: StateFlow<List<VideoRecord>> = videoDao.getUserVideos("current_user")
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val adminAllSubmissions: StateFlow<List<VideoRecord>> = videoDao.getAllVideosForAdmin()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedLecture = MutableStateFlow<VideoRecord?>(null)
    val selectedLecture: StateFlow<VideoRecord?> = _selectedLecture.asStateFlow()

    fun selectLecture(lecture: VideoRecord) {
        _selectedLecture.value = lecture
        _currentScreen.value = Screen.LECTURE_DETAIL
    }

    // ==========================================
    // PLAYLISTS (SUPER ADMIN & CURATED)
    // ==========================================
    val playlists: StateFlow<List<Playlist>> = playlistDao.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    fun selectPlaylist(playlist: Playlist?) {
        _selectedPlaylist.value = playlist
    }

    fun createPlaylist(title: String, description: String, subject: String, videoIds: List<Long> = emptyList()) {
        viewModelScope.launch {
            val playlist = Playlist(
                title = title.trim(),
                description = description.trim(),
                subject = subject.trim().ifEmpty { "General" },
                videoIds = videoIds.joinToString(","),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            playlistDao.insertPlaylist(playlist)
        }
    }

    fun updatePlaylist(playlist: Playlist, title: String, description: String, subject: String) {
        viewModelScope.launch {
            playlistDao.updatePlaylist(
                playlist.copy(
                    title = title.trim(),
                    description = description.trim(),
                    subject = subject.trim(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            playlistDao.deletePlaylist(playlist)
        }
    }

    fun addVideoToPlaylist(playlist: Playlist, videoId: Long) {
        viewModelScope.launch {
            val list = playlist.getVideoIdList().toMutableList()
            if (!list.contains(videoId)) {
                list.add(videoId)
                playlistDao.updatePlaylist(
                    playlist.copy(
                        videoIds = list.joinToString(","),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun removeVideoFromPlaylist(playlist: Playlist, videoId: Long) {
        viewModelScope.launch {
            val list = playlist.getVideoIdList().toMutableList()
            list.remove(videoId)
            playlistDao.updatePlaylist(
                playlist.copy(
                    videoIds = list.joinToString(","),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    // ==========================================
    // SUPER ADMIN CREDENTIALS & APP SETTINGS
    // ==========================================
    val announcement: StateFlow<AppAnnouncement> = adminSettings.announcementFlow
    val customSubjects: StateFlow<List<String>> = adminSettings.subjectsFlow
    val appMotto: StateFlow<String> = adminSettings.appMottoFlow
    val superAdminId: StateFlow<String> = adminSettings.adminIdFlow

    private val _isSuperAdmin = MutableStateFlow(false)
    val isSuperAdmin: StateFlow<Boolean> = _isSuperAdmin.asStateFlow()
    private val superAdminEmail = "ajayshukla60242@gmail.com"

    fun updateAdminStatus(email: String?) {
        if (email?.lowercase() == superAdminEmail.lowercase()) {
            _isSuperAdmin.value = true
        }
    }

    fun toggleSuperAdminForTesting(enabled: Boolean) {
        _isSuperAdmin.value = enabled
    }

    fun loginSuperAdmin(adminIdInput: String, passwordInput: String): Boolean {
        val success = adminSettings.verifyCredentials(adminIdInput, passwordInput)
        if (success) {
            _isSuperAdmin.value = true
        }
        return success
    }

    fun logoutSuperAdmin() {
        _isSuperAdmin.value = false
    }

    fun loginWithInstagramId(identifier: String, pass: String): Result<UserProfile> {
        val result = userManager.loginUser(identifier, pass)
        result.onSuccess { user ->
            if (user.isSuperAdmin) {
                _isSuperAdmin.value = true
            }
        }
        return result
    }

    fun registerInstagramId(
        fullName: String,
        username: String,
        pass: String,
        role: String = UserProfile.ROLE_STUDENT,
        bio: String = ""
    ): Result<UserProfile> {
        val result = userManager.registerUser(fullName, username, pass, role, bio)
        result.onSuccess { user ->
            if (user.isSuperAdmin) {
                _isSuperAdmin.value = true
            }
        }
        return result
    }

    fun loginWithGoogleFirebase(email: String, displayName: String?, uid: String): UserProfile {
        val user = userManager.loginWithGoogleFirebase(email, displayName, uid)
        if (user.isSuperAdmin) {
            _isSuperAdmin.value = true
        }
        return user
    }

    fun validateInstagramUsername(username: String): String? {
        return userManager.validateInstagramUsername(username)
    }

    fun logoutAccount() {
        userManager.logout()
        _isSuperAdmin.value = false
    }

    fun continueAsOfflineStudent() {
        userManager.continueAsGuestStudent()
    }

    fun changeSuperAdminCredentials(oldPass: String, newId: String, newPass: String): Result<Unit> {
        return adminSettings.changeAdminCredentials(oldPass, newId, newPass)
    }

    fun updateAnnouncement(enabled: Boolean, title: String, message: String) {
        adminSettings.updateAnnouncement(enabled, title, message)
    }

    fun addCustomSubject(subject: String): Boolean {
        return adminSettings.addSubject(subject)
    }

    fun updateAppMotto(motto: String) {
        adminSettings.updateAppMotto(motto)
    }

    fun addPrivateVideo(title: String, description: String, subject: String, videoUri: String) {
        viewModelScope.launch {
            videoDao.insertVideo(
                VideoRecord(
                    title = title.trim(),
                    description = description.trim(),
                    subject = subject.trim(),
                    videoUri = videoUri.trim(),
                    videoType = if (videoUri.contains("youtube.com") || videoUri.contains("youtu.be")) "YOUTUBE" else "LOCAL_FILE",
                    status = VideoRecord.STATUS_PRIVATE,
                    ownerId = "current_user",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun requestPublicPublishing(
        title: String,
        description: String,
        subject: String,
        chapter: String,
        language: String,
        videoUri: String,
        keyPoints: String = "",
        definitions: String = ""
    ) {
        viewModelScope.launch {
            videoDao.insertVideo(
                VideoRecord(
                    title = title.trim(),
                    description = description.trim(),
                    subject = subject.trim(),
                    chapter = chapter.trim(),
                    language = language.trim(),
                    videoUri = videoUri.trim(),
                    videoType = "YOUTUBE",
                    status = VideoRecord.STATUS_PENDING_APPROVAL,
                    ownerId = "current_user",
                    keyPoints = keyPoints.trim(),
                    definitions = definitions.trim(),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun adminApproveVideo(video: VideoRecord) {
        if (!_isSuperAdmin.value) return
        viewModelScope.launch {
            videoDao.updateVideo(
                video.copy(
                    status = VideoRecord.STATUS_APPROVED,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun adminPublishVideo(video: VideoRecord) {
        if (!_isSuperAdmin.value) return
        viewModelScope.launch {
            videoDao.updateVideo(
                video.copy(
                    status = VideoRecord.STATUS_PUBLISHED,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun adminRejectVideo(video: VideoRecord, reason: String) {
        if (!_isSuperAdmin.value) return
        viewModelScope.launch {
            videoDao.updateVideo(
                video.copy(
                    status = VideoRecord.STATUS_REJECTED,
                    rejectionReason = reason.trim(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun adminUnpublishVideo(video: VideoRecord) {
        if (!_isSuperAdmin.value) return
        viewModelScope.launch {
            videoDao.updateVideo(
                video.copy(
                    status = VideoRecord.STATUS_UNPUBLISHED,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun adminDirectPublishVideo(
        title: String,
        description: String,
        subject: String,
        chapter: String,
        language: String,
        videoUri: String,
        keyPoints: String = "",
        definitions: String = ""
    ) {
        if (!_isSuperAdmin.value) return
        viewModelScope.launch {
            videoDao.insertVideo(
                VideoRecord(
                    title = title.trim(),
                    description = description.trim(),
                    subject = subject.trim(),
                    chapter = chapter.trim(),
                    language = language.trim(),
                    videoUri = videoUri.trim(),
                    videoType = if (videoUri.contains("youtube.com") || videoUri.contains("youtu.be")) "YOUTUBE" else "LOCAL_FILE",
                    status = VideoRecord.STATUS_PUBLISHED,
                    ownerId = "super_admin",
                    keyPoints = keyPoints.trim(),
                    definitions = definitions.trim(),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun adminEditVideo(
        video: VideoRecord,
        title: String,
        description: String,
        subject: String,
        chapter: String,
        language: String,
        videoUri: String,
        keyPoints: String,
        definitions: String
    ) {
        if (!_isSuperAdmin.value) return
        viewModelScope.launch {
            videoDao.updateVideo(
                video.copy(
                    title = title.trim(),
                    description = description.trim(),
                    subject = subject.trim(),
                    chapter = chapter.trim(),
                    language = language.trim(),
                    videoUri = videoUri.trim(),
                    keyPoints = keyPoints.trim(),
                    definitions = definitions.trim(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteVideo(video: VideoRecord) {
        viewModelScope.launch {
            videoDao.deleteVideo(video)
        }
    }

    // ==========================================
    // STUDY TIMER STATE & ACTIONS
    // ==========================================
    private val _timerMode = MutableStateFlow("Study") // "Study" or "Break"
    val timerMode: StateFlow<String> = _timerMode.asStateFlow()

    private val _timerTotalSeconds = MutableStateFlow(25 * 60)
    val timerTotalSeconds: StateFlow<Int> = _timerTotalSeconds.asStateFlow()

    private val _timerRemainingSeconds = MutableStateFlow(25 * 60)
    val timerRemainingSeconds: StateFlow<Int> = _timerRemainingSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    val studySessions: StateFlow<List<StudySession>> = timerAlarmDao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private var timerJob: Job? = null

    fun setTimerMode(mode: String, minutes: Int) {
        if (_isTimerRunning.value) pauseTimer()
        _timerMode.value = mode
        _timerTotalSeconds.value = minutes * 60
        _timerRemainingSeconds.value = minutes * 60
    }

    fun startTimer() {
        if (_isTimerRunning.value) return
        _isTimerRunning.value = true
        timerJob = viewModelScope.launch {
            while (_timerRemainingSeconds.value > 0 && _isTimerRunning.value) {
                delay(1000L)
                _timerRemainingSeconds.value -= 1
            }
            if (_timerRemainingSeconds.value <= 0) {
                _isTimerRunning.value = false
                // Record session
                val mode = _timerMode.value
                val duration = _timerTotalSeconds.value / 60
                timerAlarmDao.insertSession(StudySession(mode = mode, durationMinutes = duration))
                // Reset for next
                _timerRemainingSeconds.value = _timerTotalSeconds.value
            }
        }
    }

    fun pauseTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
        timerJob = null
    }

    fun resetTimer() {
        pauseTimer()
        _timerRemainingSeconds.value = _timerTotalSeconds.value
    }

    // ==========================================
    // ALARM STATE & ACTIONS
    // ==========================================
    val alarms: StateFlow<List<AlarmItem>> = timerAlarmDao.getAllAlarms()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addAlarm(hour: Int, minute: Int, label: String, repeatDays: String = "Daily") {
        viewModelScope.launch {
            timerAlarmDao.insertAlarm(
                AlarmItem(
                    hour = hour,
                    minute = minute,
                    label = label.trim().ifEmpty { "Study Session" },
                    repeatDays = repeatDays
                )
            )
        }
    }

    fun toggleAlarm(alarm: AlarmItem) {
        viewModelScope.launch {
            timerAlarmDao.updateAlarm(alarm.copy(isEnabled = !alarm.isEnabled))
        }
    }

    fun deleteAlarm(alarm: AlarmItem) {
        viewModelScope.launch {
            timerAlarmDao.deleteAlarm(alarm)
        }
    }

    init {
        loadSelfies()
        seedInitialLecturesIfEmpty()
        seedInitialPlaylistsIfEmpty()
        viewModelScope.launch {
            userManager.currentUser.collect { user ->
                if (user != null) {
                    _isSuperAdmin.value = user.isSuperAdmin
                }
            }
        }
    }

    private fun seedInitialPlaylistsIfEmpty() {
        viewModelScope.launch {
            if (playlistDao.getCount() == 0) {
                val seedPlaylists = listOf(
                    Playlist(
                        title = "Mastering Classical Mechanics",
                        description = "Complete video series covering Newton's laws, force dynamics, and kinematics.",
                        subject = "Physics",
                        videoIds = "1"
                    ),
                    Playlist(
                        title = "Algebra & High School Mathematics",
                        description = "Essential algebraic methods, quadratic equations, and root analysis.",
                        subject = "Mathematics",
                        videoIds = "2"
                    ),
                    Playlist(
                        title = "Cellular Biology & Life Sciences",
                        description = "Deep dive into animal and plant cells, cellular organelle structures, and ATP energy.",
                        subject = "Biology",
                        videoIds = "3"
                    )
                )
                playlistDao.insertAll(seedPlaylists)
            }
        }
    }

    private fun seedInitialLecturesIfEmpty() {
        viewModelScope.launch {
            if (videoDao.getCount() == 0) {
                val seedLectures = listOf(
                    VideoRecord(
                        title = "Introduction to Newton's Laws of Motion",
                        description = "Complete fundamental guide to Newton's First, Second, and Third laws of motion with real-world examples.",
                        subject = "Physics",
                        chapter = "Laws of Motion",
                        language = "English",
                        videoUri = "https://www.youtube.com/watch?v=kKKM8Y-u7ds",
                        videoType = "YOUTUBE",
                        status = VideoRecord.STATUS_PUBLISHED,
                        keyPoints = "• 1st Law (Inertia): Objects at rest stay at rest unless acted on by force.\n• 2nd Law (F = ma): Acceleration is proportional to net force.\n• 3rd Law: Action & Reaction are equal and opposite.",
                        definitions = "Inertia: Tendency of an object to resist change in motion.\nForce (N): An interaction causing an object to accelerate."
                    ),
                    VideoRecord(
                        title = "Quadratic Equations: Complete Step-by-Step",
                        description = "Learn how to solve quadratic equations by factoring, completing the square, and using the quadratic formula.",
                        subject = "Mathematics",
                        chapter = "Algebra",
                        language = "Hindi",
                        videoUri = "https://www.youtube.com/watch?v=IlNAwlXPrG8",
                        videoType = "YOUTUBE",
                        status = VideoRecord.STATUS_PUBLISHED,
                        keyPoints = "• General form: ax² + bx + c = 0\n• Discriminant D = b² - 4ac determines nature of roots.\n• Quadratic formula: x = (-b ± √D) / 2a",
                        definitions = "Roots: Values of x that satisfy the equation.\nDiscriminant: D > 0 (two distinct real roots), D = 0 (two equal roots), D < 0 (no real roots)."
                    ),
                    VideoRecord(
                        title = "Cell Structure & Organelles Overview",
                        description = "A thorough visual tour of plant and animal cells, cell membranes, nucleus, and mitochondria.",
                        subject = "Biology",
                        chapter = "Cell Biology",
                        language = "English",
                        videoUri = "https://www.youtube.com/watch?v=URUJD5NEXC8",
                        videoType = "YOUTUBE",
                        status = VideoRecord.STATUS_PUBLISHED,
                        keyPoints = "• Nucleus houses DNA and controls cellular activity.\n• Mitochondria produces ATP energy via cellular respiration.\n• Chloroplasts conduct photosynthesis in plant cells.",
                        definitions = "Organelle: Subcellular structure with specific vital functions.\nATP: Adenosine triphosphate, the cellular energy currency."
                    )
                )
                videoDao.insertAll(seedLectures)
            }
        }
    }
}
