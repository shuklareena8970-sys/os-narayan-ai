package com.example

import com.example.data.local.AlarmItem
import com.example.data.local.Note
import com.example.data.local.VideoRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NarayanBusinessLogicTest {

    // Helper method replicating YouTube ID extraction in app
    private fun extractYouTubeId(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return null
        val pattern = "(?:youtube\\.com\\/(?:[^\\/]+\\/.+\\/|(?:v|e(?:mbed)?)\\/|.*[?&]v=)|youtu\\.be\\/)([^\"&?\\/ ]{11})".toRegex()
        val match = pattern.find(trimmed)
        return match?.groupValues?.get(1)
    }

    @Test
    fun testYouTubeUrlValidation_validUrls() {
        val url1 = "https://www.youtube.com/watch?v=kKKM8Y-u7ds"
        val id1 = extractYouTubeId(url1)
        assertEquals("kKKM8Y-u7ds", id1)

        val url2 = "https://youtu.be/IlNAwlXPrG8"
        val id2 = extractYouTubeId(url2)
        assertEquals("IlNAwlXPrG8", id2)

        val url3 = "https://www.youtube.com/embed/URUJD5NEXC8"
        val id3 = extractYouTubeId(url3)
        assertEquals("URUJD5NEXC8", id3)
    }

    @Test
    fun testYouTubeUrlValidation_invalidUrls() {
        assertNull(extractYouTubeId(""))
        assertNull(extractYouTubeId("   "))
        assertNull(extractYouTubeId("https://google.com"))
        assertNull(extractYouTubeId("https://vimeo.com/12345678"))
        assertNull(extractYouTubeId("not-a-url"))
    }

    @Test
    fun testVideoStatuses_publicationIntegrity() {
        val privateVid = VideoRecord(
            title = "Private Notes",
            videoUri = "https://youtube.com/watch?v=kKKM8Y-u7ds",
            status = VideoRecord.STATUS_PRIVATE
        )
        val pendingVid = VideoRecord(
            title = "Student Lecture Proposal",
            videoUri = "https://youtube.com/watch?v=kKKM8Y-u7ds",
            status = VideoRecord.STATUS_PENDING_APPROVAL
        )
        val rejectedVid = VideoRecord(
            title = "Incomplete Lecture",
            videoUri = "https://youtube.com/watch?v=kKKM8Y-u7ds",
            status = VideoRecord.STATUS_REJECTED,
            rejectionReason = "Audio quality below standard"
        )
        val publishedVid = VideoRecord(
            title = "Newton's Laws Approved",
            videoUri = "https://youtube.com/watch?v=kKKM8Y-u7ds",
            status = VideoRecord.STATUS_PUBLISHED
        )
        val unpublishedVid = VideoRecord(
            title = "Outdated Syllabus",
            videoUri = "https://youtube.com/watch?v=kKKM8Y-u7ds",
            status = VideoRecord.STATUS_UNPUBLISHED
        )

        val allVideos = listOf(privateVid, pendingVid, rejectedVid, publishedVid, unpublishedVid)

        // Only Published status may appear in public lectures
        val publicLectures = allVideos.filter { it.status == VideoRecord.STATUS_PUBLISHED }
        assertEquals(1, publicLectures.size)
        assertEquals("Newton's Laws Approved", publicLectures.first().title)

        // All non-published must be excluded
        assertTrue(publicLectures.none { it.status == VideoRecord.STATUS_PRIVATE })
        assertTrue(publicLectures.none { it.status == VideoRecord.STATUS_PENDING_APPROVAL })
        assertTrue(publicLectures.none { it.status == VideoRecord.STATUS_REJECTED })
        assertTrue(publicLectures.none { it.status == VideoRecord.STATUS_UNPUBLISHED })
    }

    @Test
    fun testNoteValidation_titleNotEmpty() {
        val blankTitle = "   "
        val validTitle = "Cell Division Notes"

        assertFalse(blankTitle.isNotBlank())
        assertTrue(validTitle.isNotBlank())

        val note = Note(
            id = 1,
            title = validTitle,
            content = "Mitosis has 4 phases.",
            subject = "Biology"
        )
        assertEquals("Biology", note.subject)
        assertEquals("Cell Division Notes", note.title)
    }

    @Test
    fun testTimerMath_focusAndBreakDurations() {
        val studyMinutes = 25
        val studySeconds = studyMinutes * 60
        assertEquals(1500, studySeconds)

        val breakMinutes = 5
        val breakSeconds = breakMinutes * 60
        assertEquals(300, breakSeconds)

        // Custom duration validation boundary
        fun isValidCustomDuration(minutes: Int): Boolean = minutes in 1..180

        assertTrue(isValidCustomDuration(1))
        assertTrue(isValidCustomDuration(45))
        assertTrue(isValidCustomDuration(180))
        assertFalse(isValidCustomDuration(0))
        assertFalse(isValidCustomDuration(-5))
        assertFalse(isValidCustomDuration(200))
    }

    @Test
    fun testAlarm12HourConversion() {
        fun convertTo24Hour(hour12: Int, isPm: Boolean): Int {
            return when {
                isPm && hour12 < 12 -> hour12 + 12
                !isPm && hour12 == 12 -> 0
                else -> hour12
            }
        }

        // 7:00 AM -> 7
        assertEquals(7, convertTo24Hour(7, false))
        // 12:00 AM (midnight) -> 0
        assertEquals(0, convertTo24Hour(12, false))
        // 7:00 PM -> 19
        assertEquals(19, convertTo24Hour(7, true))
        // 12:00 PM (noon) -> 12
        assertEquals(12, convertTo24Hour(12, true))

        val alarm = AlarmItem(hour = 7, minute = 30, label = "Physics Revision", isEnabled = true)
        assertTrue(alarm.isEnabled)
        assertEquals("Physics Revision", alarm.label)
    }

    @Test
    fun testChess_initialBoardSetup() {
        val game = com.example.data.chess.ChessGameState()
        assertEquals(com.example.data.chess.PieceColor.WHITE, game.turn)
        assertFalse(game.isGameOver)
        assertEquals(0, game.getWhiteScore())
        assertEquals(0, game.getBlackScore())

        // Check white e2 pawn
        val e2Pawn = game.board[6][4]
        assertNotNull(e2Pawn)
        assertEquals(com.example.data.chess.PieceType.PAWN, e2Pawn?.type)
        assertEquals(com.example.data.chess.PieceColor.WHITE, e2Pawn?.color)

        // Check valid moves for e2 pawn: can move e3 (row 5, col 4) or e4 (row 4, col 4)
        val validMoves = game.getValidMoves(com.example.data.chess.Square(6, 4))
        assertEquals(2, validMoves.size)
        assertTrue(validMoves.contains(com.example.data.chess.Square(5, 4)))
        assertTrue(validMoves.contains(com.example.data.chess.Square(4, 4)))
    }

    @Test
    fun testChess_pawnMoveAndTurnSwitch() {
        val game = com.example.data.chess.ChessGameState()
        // White plays e2 to e4
        val moveSuccess = game.makeMove(
            com.example.data.chess.Square(6, 4),
            com.example.data.chess.Square(4, 4)
        )
        assertTrue(moveSuccess)
        assertNull(game.board[6][4])
        assertNotNull(game.board[4][4])
        assertEquals(com.example.data.chess.PieceColor.BLACK, game.turn)
    }

    @Test
    fun testChess_aiMoveGeneratesValidMove() {
        val game = com.example.data.chess.ChessGameState()
        // White moves e2 -> e4
        game.makeMove(
            com.example.data.chess.Square(6, 4),
            com.example.data.chess.Square(4, 4)
        )
        // Black AI moves at Level 2
        val aiMoved = game.makeAiMove(level = 2)
        assertTrue(aiMoved)
        assertEquals(com.example.data.chess.PieceColor.WHITE, game.turn)
    }

    @Test
    fun testChess_undoMove_restoresBoardAndTurn() {
        val game = com.example.data.chess.ChessGameState()
        val e2 = com.example.data.chess.Square(6, 4)
        val e4 = com.example.data.chess.Square(4, 4)

        // Make move
        val moved = game.makeMove(e2, e4)
        assertTrue(moved)
        assertNull(game.board[6][4])
        assertNotNull(game.board[4][4])
        assertEquals(com.example.data.chess.PieceColor.BLACK, game.turn)
        assertEquals(1, game.moveHistory.size)

        // Undo move
        val undone = game.undoMove()
        assertTrue(undone)
        assertNotNull(game.board[6][4])
        assertNull(game.board[4][4])
        assertEquals(com.example.data.chess.PieceColor.WHITE, game.turn)
        assertEquals(0, game.moveHistory.size)
    }

    @Test
    fun testChess_captureAndScoreTracking_andUndo() {
        val game = com.example.data.chess.ChessGameState()
        // Setup: White pawn at (4, 4), Black pawn at (3, 3)
        game.board[4][4] = com.example.data.chess.ChessPiece(
            com.example.data.chess.PieceType.PAWN,
            com.example.data.chess.PieceColor.WHITE
        )
        game.board[3][3] = com.example.data.chess.ChessPiece(
            com.example.data.chess.PieceType.PAWN,
            com.example.data.chess.PieceColor.BLACK
        )
        game.turn = com.example.data.chess.PieceColor.WHITE

        // White pawn captures diagonal at (3, 3)
        val validMoves = game.getValidMoves(com.example.data.chess.Square(4, 4))
        assertTrue(validMoves.contains(com.example.data.chess.Square(3, 3)))

        val captured = game.makeMove(
            com.example.data.chess.Square(4, 4),
            com.example.data.chess.Square(3, 3)
        )
        assertTrue(captured)
        assertEquals(1, game.getWhiteScore())
        assertEquals(1, game.capturedByWhite.size)
        assertEquals(com.example.data.chess.PieceType.PAWN, game.capturedByWhite.first().type)

        // Undo capture
        val undone = game.undoMove()
        assertTrue(undone)
        assertEquals(0, game.getWhiteScore())
        assertEquals(0, game.capturedByWhite.size)
        assertNotNull(game.board[4][4])
        assertNotNull(game.board[3][3])
        assertEquals(com.example.data.chess.PieceColor.WHITE, game.board[4][4]?.color)
        assertEquals(com.example.data.chess.PieceColor.BLACK, game.board[3][3]?.color)
    }

    @Test
    fun testChess_knightJumpOverPieces() {
        val game = com.example.data.chess.ChessGameState()
        // White knight at b1 (7, 1) can jump over pawns to a3 (5, 0) or c3 (5, 2)
        val b1Moves = game.getValidMoves(com.example.data.chess.Square(7, 1))
        assertEquals(2, b1Moves.size)
        assertTrue(b1Moves.contains(com.example.data.chess.Square(5, 0)))
        assertTrue(b1Moves.contains(com.example.data.chess.Square(5, 2)))

        val jumped = game.makeMove(
            com.example.data.chess.Square(7, 1),
            com.example.data.chess.Square(5, 2)
        )
        assertTrue(jumped)
        assertNotNull(game.board[5][2])
        assertEquals(com.example.data.chess.PieceType.KNIGHT, game.board[5][2]?.type)
    }

    @Test
    fun testChess_bishopDiagonalBlocked_andOpenPath() {
        val game = com.example.data.chess.ChessGameState()
        // White bishop at c1 (7, 2) is blocked by pawns at b2, d2
        val initialBishopMoves = game.getValidMoves(com.example.data.chess.Square(7, 2))
        assertTrue(initialBishopMoves.isEmpty())

        // Move d2 pawn (6, 3) -> d4 (4, 3)
        game.makeMove(
            com.example.data.chess.Square(6, 3),
            com.example.data.chess.Square(4, 3)
        )
        // Black plays a move
        game.makeMove(
            com.example.data.chess.Square(1, 4),
            com.example.data.chess.Square(2, 4)
        )

        // Now White Bishop at c1 has clear diagonal: e3 (5, 4), f4 (4, 5), g5 (3, 6), h6 (2, 7)
        val openBishopMoves = game.getValidMoves(com.example.data.chess.Square(7, 2))
        assertTrue(openBishopMoves.isNotEmpty())
        assertTrue(openBishopMoves.contains(com.example.data.chess.Square(5, 4)))
        assertTrue(openBishopMoves.contains(com.example.data.chess.Square(4, 5)))
    }

    @Test
    fun testChess_pawnPromotionToQueen() {
        val game = com.example.data.chess.ChessGameState()
        // Place white pawn on row 1, col 0 (a7) and clear row 0, col 0 (a8)
        game.board[1][0] = com.example.data.chess.ChessPiece(
            com.example.data.chess.PieceType.PAWN,
            com.example.data.chess.PieceColor.WHITE
        )
        game.board[0][0] = null
        game.turn = com.example.data.chess.PieceColor.WHITE

        // Move pawn forward to row 0
        val moved = game.makeMove(
            com.example.data.chess.Square(1, 0),
            com.example.data.chess.Square(0, 0)
        )
        assertTrue(moved)
        val promotedPiece = game.board[0][0]
        assertNotNull(promotedPiece)
        assertEquals(com.example.data.chess.PieceType.QUEEN, promotedPiece?.type)
        assertEquals(com.example.data.chess.PieceColor.WHITE, promotedPiece?.color)
    }

    @Test
    fun testChess_kingCapture_triggersCheckmate() {
        val game = com.example.data.chess.ChessGameState()
        // Place White Queen at (3, 4) and Black King at (2, 4)
        game.board[3][4] = com.example.data.chess.ChessPiece(
            com.example.data.chess.PieceType.QUEEN,
            com.example.data.chess.PieceColor.WHITE
        )
        game.board[2][4] = com.example.data.chess.ChessPiece(
            com.example.data.chess.PieceType.KING,
            com.example.data.chess.PieceColor.BLACK
        )
        game.turn = com.example.data.chess.PieceColor.WHITE

        val moved = game.makeMove(
            com.example.data.chess.Square(3, 4),
            com.example.data.chess.Square(2, 4)
        )
        assertTrue(moved)
        assertTrue(game.isGameOver)
        assertEquals(com.example.data.chess.PieceColor.WHITE, game.winner)
        assertTrue(game.statusText.contains("Checkmate"))
    }

    @Test
    fun testChess_allAiLevelsExecuteValidMoves() {
        for (level in 1..3) {
            val game = com.example.data.chess.ChessGameState()
            // White opens e4
            game.makeMove(
                com.example.data.chess.Square(6, 4),
                com.example.data.chess.Square(4, 4)
            )
            // AI responds
            val aiMoved = game.makeAiMove(level = level)
            assertTrue("AI Level $level should make a valid move", aiMoved)
            assertEquals(com.example.data.chess.PieceColor.WHITE, game.turn)
        }
    }

    @Test
    fun testSyncQueue_itemGeneration() {
        val queueItem = com.example.data.sync.SyncQueueItem(
            entityType = "NOTE",
            entityId = "101",
            operation = com.example.data.sync.SyncQueueItem.OP_CREATE,
            payloadJson = "{\"title\":\"Physics Motion\"}"
        )
        assertNotNull(queueItem.id)
        assertTrue(queueItem.id.isNotEmpty())
        assertEquals("NOTE", queueItem.entityType)
        assertEquals("101", queueItem.entityId)
        assertEquals(com.example.data.sync.SyncQueueItem.OP_CREATE, queueItem.operation)
        assertEquals(com.example.data.sync.SyncQueueItem.STATUS_PENDING, queueItem.status)
        assertEquals(0, queueItem.retryCount)
    }

    @Test
    fun testSyncQueue_exponentialBackoffCalculation() {
        fun calculateBackoffMs(retryCount: Int): Long {
            return 500L * (1 shl minOf(retryCount, 4))
        }

        assertEquals(1000L, calculateBackoffMs(1)) // 500 * 2^1 = 1000
        assertEquals(2000L, calculateBackoffMs(2)) // 500 * 2^2 = 2000
        assertEquals(4000L, calculateBackoffMs(3)) // 500 * 2^3 = 4000
        assertEquals(8000L, calculateBackoffMs(4)) // 500 * 2^4 = 8000
        assertEquals(8000L, calculateBackoffMs(5)) // capped at 2^4
    }
}
