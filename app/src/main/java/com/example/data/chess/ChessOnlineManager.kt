package com.example.data.chess

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale
import kotlin.random.Random

data class OnlineChessRoom(
    val roomId: String = "",
    val hostUserId: String = "",
    val hostUsername: String = "",
    val guestUserId: String? = null,
    val guestUsername: String? = null,
    val hostColor: String = "WHITE", // "WHITE" or "BLACK"
    val currentTurn: String = "WHITE", // "WHITE" or "BLACK"
    val status: String = "WAITING", // "WAITING", "PLAYING", "FINISHED"
    val lastMoveFromRow: Int? = null,
    val lastMoveFromCol: Int? = null,
    val lastMoveToRow: Int? = null,
    val lastMoveToCol: Int? = null,
    val moveCount: Int = 0,
    val winner: String? = null, // "WHITE", "BLACK", "DRAW"
    val endReason: String? = null, // "CHECKMATE", "RESIGNED", "DRAW_AGREED"
    val lastReaction: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isFull: Boolean
        get() = !guestUserId.isNullOrBlank()
}

class ChessOnlineManager(private val context: Context) {
    private val db: FirebaseFirestore by lazy {
        try {
            val dbId = context.getString(R.string.firestore_database_id)
            FirebaseFirestore.getInstance(dbId)
        } catch (e: Exception) {
            Log.e("ChessOnlineManager", "Failed to get custom firestore instance, fallback to default", e)
            FirebaseFirestore.getInstance()
        }
    }

    companion object {
        private const val COLLECTION_ROOMS = "chess_rooms"
        private const val TAG = "ChessOnlineManager"

        fun generateRoomCode(): String {
            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            val sb = StringBuilder("CHESS-")
            repeat(4) {
                sb.append(chars[Random.nextInt(chars.length)])
            }
            return sb.toString()
        }
    }

    /**
     * Creates a new online 1v1 match room
     */
    fun createRoom(
        userId: String,
        username: String,
        asWhite: Boolean = true,
        onSuccess: (OnlineChessRoom) -> Unit,
        onError: (String) -> Unit
    ) {
        val roomId = generateRoomCode()
        val room = OnlineChessRoom(
            roomId = roomId,
            hostUserId = userId,
            hostUsername = username,
            hostColor = if (asWhite) "WHITE" else "BLACK",
            currentTurn = "WHITE",
            status = "WAITING",
            moveCount = 0,
            updatedAt = System.currentTimeMillis()
        )

        db.collection(COLLECTION_ROOMS).document(roomId)
            .set(room)
            .addOnSuccessListener {
                Log.d(TAG, "Room created: $roomId")
                onSuccess(room)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error creating room", e)
                onError(e.localizedMessage ?: "Failed to create room")
            }
    }

    /**
     * Joins an existing room by code
     */
    fun joinRoom(
        roomCodeInput: String,
        userId: String,
        username: String,
        onSuccess: (OnlineChessRoom) -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanCode = roomCodeInput.trim().uppercase(Locale.ROOT)
        val docRef = db.collection(COLLECTION_ROOMS).document(cleanCode)

        docRef.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    onError("Room code not found. Check the code and try again.")
                    return@addOnSuccessListener
                }

                val room = snapshot.toObject(OnlineChessRoom::class.java)
                if (room == null) {
                    onError("Invalid room data.")
                    return@addOnSuccessListener
                }

                if (room.hostUserId == userId) {
                    // Rejoining own room
                    onSuccess(room)
                    return@addOnSuccessListener
                }

                if (room.isFull && room.guestUserId != userId) {
                    onError("This room is already full with 2 players.")
                    return@addOnSuccessListener
                }

                // Join room and set status to PLAYING
                val updates = mapOf(
                    "guestUserId" to userId,
                    "guestUsername" to username,
                    "status" to "PLAYING",
                    "updatedAt" to System.currentTimeMillis()
                )

                docRef.update(updates)
                    .addOnSuccessListener {
                        val updatedRoom = room.copy(
                            guestUserId = userId,
                            guestUsername = username,
                            status = "PLAYING"
                        )
                        onSuccess(updatedRoom)
                    }
                    .addOnFailureListener { e ->
                        onError(e.localizedMessage ?: "Failed to join room")
                    }
            }
            .addOnFailureListener { e ->
                onError(e.localizedMessage ?: "Network error finding room")
            }
    }

    /**
     * Subscribes to live room updates (real-time moves, player joins, game over)
     */
    fun observeRoom(roomId: String): Flow<OnlineChessRoom?> = callbackFlow {
        val docRef = db.collection(COLLECTION_ROOMS).document(roomId)
        val registration: ListenerRegistration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Listen failed for room: $roomId", error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val room = snapshot.toObject(OnlineChessRoom::class.java)
                trySend(room)
            } else {
                trySend(null)
            }
        }
        awaitClose {
            registration.remove()
        }
    }

    /**
     * Transmits a player move to the Firestore room
     */
    fun transmitMove(
        roomId: String,
        fromRow: Int,
        fromCol: Int,
        toRow: Int,
        toCol: Int,
        nextTurn: String,
        newMoveCount: Int,
        isCheckmate: Boolean = false,
        winnerColor: String? = null,
        onError: (String) -> Unit = {}
    ) {
        val docRef = db.collection(COLLECTION_ROOMS).document(roomId)
        val updates = mutableMapOf<String, Any>(
            "lastMoveFromRow" to fromRow,
            "lastMoveFromCol" to fromCol,
            "lastMoveToRow" to toRow,
            "lastMoveToCol" to toCol,
            "currentTurn" to nextTurn,
            "moveCount" to newMoveCount,
            "updatedAt" to System.currentTimeMillis()
        )

        if (isCheckmate && winnerColor != null) {
            updates["status"] = "FINISHED"
            updates["winner"] = winnerColor
            updates["endReason"] = "CHECKMATE"
        }

        docRef.set(updates, SetOptions.merge())
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to transmit move", e)
                onError(e.localizedMessage ?: "Move sync failed")
            }
    }

    /**
     * Resign match
     */
    fun resignMatch(roomId: String, resigningColor: String) {
        val winner = if (resigningColor == "WHITE") "BLACK" else "WHITE"
        val docRef = db.collection(COLLECTION_ROOMS).document(roomId)
        docRef.update(
            mapOf(
                "status" to "FINISHED",
                "winner" to winner,
                "endReason" to "RESIGNED",
                "updatedAt" to System.currentTimeMillis()
            )
        )
    }

    /**
     * Send in-game reaction emoji (🤝, 🔥, 👑, 👏, 😮)
     */
    fun sendReaction(roomId: String, reaction: String) {
        val docRef = db.collection(COLLECTION_ROOMS).document(roomId)
        docRef.update(
            mapOf(
                "lastReaction" to reaction,
                "updatedAt" to System.currentTimeMillis()
            )
        )
    }

    /**
     * Rematch / Reset Board for online room
     */
    fun resetOnlineMatch(roomId: String) {
        val docRef = db.collection(COLLECTION_ROOMS).document(roomId)
        docRef.update(
            mapOf(
                "currentTurn" to "WHITE",
                "status" to "PLAYING",
                "lastMoveFromRow" to null,
                "lastMoveFromCol" to null,
                "lastMoveToRow" to null,
                "lastMoveToCol" to null,
                "moveCount" to 0,
                "winner" to null,
                "endReason" to null,
                "updatedAt" to System.currentTimeMillis()
            )
        )
    }
}
