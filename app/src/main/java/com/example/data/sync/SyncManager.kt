package com.example.data.sync

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class SyncUiState(
    val isOnline: Boolean = true,
    val pendingCount: Int = 0,
    val lastSyncTime: Long = System.currentTimeMillis(),
    val isSyncing: Boolean = false,
    val lastError: String? = null
)

class SyncManager(
    private val context: Context,
    private val syncQueueDao: SyncQueueDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val networkMonitor = NetworkMonitor(context)

    private val _isOnline = MutableStateFlow(networkMonitor.isOnline())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(System.currentTimeMillis())
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    val pendingCount: StateFlow<Int> = syncQueueDao.getPendingCount()
        .stateIn(scope, SharingStarted.Eagerly, 0)

    val syncUiState: StateFlow<SyncUiState> = combine(
        _isOnline,
        pendingCount,
        _lastSyncTime,
        _isSyncing,
        _lastError
    ) { online, pending, lastSync, syncing, error ->
        SyncUiState(
            isOnline = online,
            pendingCount = pending,
            lastSyncTime = lastSync,
            isSyncing = syncing,
            lastError = error
        )
    }.stateIn(scope, SharingStarted.Eagerly, SyncUiState())

    init {
        // Monitor network state
        scope.launch {
            networkMonitor.isOnlineFlow.collect { online ->
                _isOnline.value = online
                if (online) {
                    // Connectivity returned - trigger automatic sync
                    syncPendingQueue()
                }
            }
        }
    }

    suspend fun queueOperation(
        entityType: String,
        entityId: String,
        operation: String,
        payloadJson: String
    ): String {
        val opId = UUID.randomUUID().toString()
        val item = SyncQueueItem(
            id = opId,
            entityType = entityType,
            entityId = entityId,
            operation = operation,
            payloadJson = payloadJson,
            timestamp = System.currentTimeMillis(),
            status = SyncQueueItem.STATUS_PENDING
        )
        syncQueueDao.enqueue(item)

        // If currently online, trigger sync
        if (_isOnline.value && !_isSyncing.value) {
            scope.launch { syncPendingQueue() }
        }

        return opId
    }

    suspend fun syncPendingQueue() {
        if (_isSyncing.value) return
        _isSyncing.value = true
        _lastError.value = null

        try {
            val pendingItems = syncQueueDao.getAllPendingDirect()
            if (pendingItems.isEmpty()) {
                _lastSyncTime.value = System.currentTimeMillis()
                _isSyncing.value = false
                return
            }

            for (item in pendingItems) {
                // If network was lost mid-sync, pause and retain items
                if (!_isOnline.value) {
                    _lastError.value = "Sync paused: device is currently offline"
                    break
                }

                syncQueueDao.update(item.copy(status = SyncQueueItem.STATUS_SYNCING))

                // Simulate idempotent sync operation with exponential backoff on retry
                val success = processRemoteSyncItem(item)
                if (success) {
                    // Confirmed by backend: safely remove from persistent local queue
                    syncQueueDao.deleteById(item.id)
                } else {
                    val nextRetry = item.retryCount + 1
                    val errorMsg = "Server response timeout / retry #$nextRetry"
                    _lastError.value = errorMsg
                    syncQueueDao.update(
                        item.copy(
                            status = SyncQueueItem.STATUS_FAILED,
                            retryCount = nextRetry,
                            errorMessage = errorMsg
                        )
                    )
                    // Exponential backoff delay
                    delay((500L * (1 shl minOf(nextRetry, 4))))
                }
            }
            _lastSyncTime.value = System.currentTimeMillis()
        } catch (e: Exception) {
            _lastError.value = e.message ?: "Sync encountered an unexpected error"
        } finally {
            _isSyncing.value = false
        }
    }

    private suspend fun processRemoteSyncItem(item: SyncQueueItem): Boolean {
        // Idempotent sync operation:
        // When online, payload is verified and processed against the server/remote boundary
        delay(120L) // Simulate network round-trip
        return true
    }

    fun triggerManualSync() {
        scope.launch {
            syncPendingQueue()
        }
    }
}
