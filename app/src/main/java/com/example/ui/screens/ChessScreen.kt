package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.chess.ChessGameState
import com.example.data.chess.PieceColor
import com.example.data.chess.Square
import com.example.ui.NarayanViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class BoardTheme(val title: String, val lightColor: Color, val darkColor: Color) {
    CLASSIC_WOOD("Wood", Color(0xFFF0D9B5), Color(0xFFB58863)),
    FOREST_EMERALD("Emerald", Color(0xFFE8EDCC), Color(0xFF779954)),
    OBSIDIAN_SLATE("Slate", Color(0xFFECEFF1), Color(0xFF546E7A))
}

enum class GamePlayMode {
    VS_AI,
    LOCAL_PASS_AND_PLAY,
    ONLINE_1V1
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChessScreen(viewModel: NarayanViewModel) {
    BackHandler {
        viewModel.leaveOnlineRoom()
        viewModel.navigateBack()
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val chessStats by viewModel.chessStats.collectAsState()
    val onlineRoom by viewModel.onlineRoomState.collectAsState()
    val currentUser by viewModel.currentUserProfile.collectAsState()

    // Board game state
    val gameState = remember { ChessGameState() }
    var selectedSquare by remember { mutableStateOf<Square?>(null) }
    var validMovesForSelected by remember { mutableStateOf<List<Square>>(emptyList()) }
    var lastMoveFrom by remember { mutableStateOf<Square?>(null) }
    var lastMoveTo by remember { mutableStateOf<Square?>(null) }

    var playMode by remember { mutableStateOf(GamePlayMode.VS_AI) }
    var aiLevel by remember { mutableIntStateOf(1) } // 1: Easy, 2: Medium, 3: Hard
    var isAiThinking by remember { mutableStateOf(false) }
    var boardTheme by remember { mutableStateOf(BoardTheme.CLASSIC_WOOD) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var showGameOverDialog by remember { mutableStateOf(false) }
    var gameOverMessage by remember { mutableStateOf("") }

    // Online multiplayer dialogs
    var showOnlineDialog by remember { mutableStateOf(false) }
    var joinRoomCodeInput by remember { mutableStateOf("") }
    var onlineErrorMsg by remember { mutableStateOf<String?>(null) }
    var isConnectingOnline by remember { mutableStateOf(false) }
    var floatingReaction by remember { mutableStateOf<String?>(null) }

    // Check online room sync
    LaunchedEffect(onlineRoom) {
        val room = onlineRoom ?: return@LaunchedEffect
        // Show opponent reactions
        if (!room.lastReaction.isNullOrBlank()) {
            floatingReaction = room.lastReaction
            delay(2500L)
            floatingReaction = null
        }

        // Apply opponent move if new move count
        if (room.status == "PLAYING" && room.lastMoveFromRow != null && room.lastMoveToRow != null) {
            val from = Square(room.lastMoveFromRow, room.lastMoveFromCol ?: 0)
            val to = Square(room.lastMoveToRow, room.lastMoveToCol ?: 0)

            // Check if this move is not yet on our board
            val expectedTurn = if (room.currentTurn == "WHITE") PieceColor.WHITE else PieceColor.BLACK
            if (gameState.turn != expectedTurn) {
                gameState.makeMove(from, to)
                lastMoveFrom = from
                lastMoveTo = to
                refreshKey++
            }
        }

        // Handle online match finish
        if (room.status == "FINISHED" && !showGameOverDialog) {
            val winText = when (room.winner) {
                "WHITE" -> "White Won!"
                "BLACK" -> "Black Won!"
                else -> "Game Drawn!"
            }
            val reasonText = when (room.endReason) {
                "CHECKMATE" -> "Checkmate! Victory achieved."
                "RESIGNED" -> "Opponent resigned the game."
                else -> "Game concluded."
            }
            gameOverMessage = "$winText\n$reasonText"
            showGameOverDialog = true
        }
    }

    // Determine current player's side in online mode
    val myOnlineColor: PieceColor? = remember(onlineRoom, currentUser) {
        val room = onlineRoom ?: return@remember null
        val myId = currentUser?.username ?: ""
        if (room.hostUserId == myId) {
            if (room.hostColor == "WHITE") PieceColor.WHITE else PieceColor.BLACK
        } else {
            if (room.hostColor == "WHITE") PieceColor.BLACK else PieceColor.WHITE
        }
    }

    val isMyTurnToMove: Boolean = remember(playMode, gameState.turn, myOnlineColor, isAiThinking) {
        when (playMode) {
            GamePlayMode.VS_AI -> gameState.turn == PieceColor.WHITE && !isAiThinking
            GamePlayMode.LOCAL_PASS_AND_PLAY -> true
            GamePlayMode.ONLINE_1V1 -> {
                val room = onlineRoom
                if (room == null || room.status != "PLAYING") false
                else gameState.turn == myOnlineColor
            }
        }
    }

    fun handleSquareClick(row: Int, col: Int) {
        if (gameState.isGameOver || isAiThinking) return
        if (!isMyTurnToMove) {
            if (playMode == GamePlayMode.ONLINE_1V1 && onlineRoom?.status == "WAITING") {
                Toast.makeText(context, "Waiting for friend to join with room code!", Toast.LENGTH_SHORT).show()
            } else if (playMode == GamePlayMode.ONLINE_1V1) {
                Toast.makeText(context, "Opponent's turn to move...", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val clickedSq = Square(row, col)
        val currentPiece = gameState.board[row][col]

        if (selectedSquare != null) {
            val from = selectedSquare!!
            if (from == clickedSq) {
                // Deselect
                selectedSquare = null
                validMovesForSelected = emptyList()
            } else if (validMovesForSelected.contains(clickedSq)) {
                // Execute move
                val moveSuccess = gameState.makeMove(from, clickedSq)
                if (moveSuccess) {
                    lastMoveFrom = from
                    lastMoveTo = clickedSq
                    selectedSquare = null
                    validMovesForSelected = emptyList()
                    refreshKey++

                    // Check if online mode -> transmit move
                    if (playMode == GamePlayMode.ONLINE_1V1 && onlineRoom != null) {
                        val room = onlineRoom!!
                        val nextTurnStr = if (gameState.turn == PieceColor.WHITE) "WHITE" else "BLACK"
                        val winnerStr = if (gameState.winner == PieceColor.WHITE) "WHITE" else if (gameState.winner == PieceColor.BLACK) "BLACK" else null
                        viewModel.transmitOnlineMove(
                            roomId = room.roomId,
                            fromRow = from.row,
                            fromCol = from.col,
                            toRow = clickedSq.row,
                            toCol = clickedSq.col,
                            nextTurn = nextTurnStr,
                            moveCount = room.moveCount + 1,
                            isCheckmate = gameState.isGameOver,
                            winnerColor = winnerStr
                        )
                    }

                    if (gameState.isGameOver) {
                        gameOverMessage = "🏆 Checkmate!\n${if (gameState.winner == PieceColor.WHITE) "White" else "Black"} Wins!"
                        showGameOverDialog = true
                        val isPlayerWin = gameState.winner == PieceColor.WHITE
                        val isAiWin = gameState.winner == PieceColor.BLACK
                        viewModel.recordChessGameResult(isPlayerWin, isAiWin, gameState.getWhiteScore())
                    } else if (playMode == GamePlayMode.VS_AI && gameState.turn == PieceColor.BLACK) {
                        // AI thinking
                        isAiThinking = true
                        scope.launch {
                            delay(350L) // snappy, natural thinking duration
                            gameState.makeAiMove(aiLevel)
                            isAiThinking = false
                            refreshKey++
                            if (gameState.moveHistory.isNotEmpty()) {
                                val lastMove = gameState.moveHistory.last()
                                lastMoveFrom = lastMove.from
                                lastMoveTo = lastMove.to
                            }
                            if (gameState.isGameOver) {
                                gameOverMessage = "🏆 Checkmate!\n${if (gameState.winner == PieceColor.WHITE) "White" else "Black"} Wins!"
                                showGameOverDialog = true
                                val isPlayerWin = gameState.winner == PieceColor.WHITE
                                val isAiWin = gameState.winner == PieceColor.BLACK
                                viewModel.recordChessGameResult(isPlayerWin, isAiWin, gameState.getWhiteScore())
                            }
                        }
                    }
                }
            } else if (currentPiece != null && currentPiece.color == gameState.turn) {
                // Switch piece
                selectedSquare = clickedSq
                validMovesForSelected = gameState.getValidMoves(clickedSq)
            } else {
                selectedSquare = null
                validMovesForSelected = emptyList()
            }
        } else {
            // Select piece
            if (currentPiece != null && currentPiece.color == gameState.turn) {
                selectedSquare = clickedSq
                validMovesForSelected = gameState.getValidMoves(clickedSq)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Chess Mind Master (शतरंज)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = when (playMode) {
                                GamePlayMode.VS_AI -> "vs Smart AI (Level $aiLevel)"
                                GamePlayMode.LOCAL_PASS_AND_PLAY -> "Pass & Play (2 Players)"
                                GamePlayMode.ONLINE_1V1 -> "Live 1v1 Online Room: ${onlineRoom?.roomId ?: "Not Connected"}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.leaveOnlineRoom()
                            viewModel.navigateBack()
                        },
                        modifier = Modifier.testTag("chess_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showOnlineDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Online 1v1 Room",
                            tint = if (playMode == GamePlayMode.ONLINE_1V1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(onClick = {
                        val nextTheme = when (boardTheme) {
                            BoardTheme.CLASSIC_WOOD -> BoardTheme.FOREST_EMERALD
                            BoardTheme.FOREST_EMERALD -> BoardTheme.OBSIDIAN_SLATE
                            BoardTheme.OBSIDIAN_SLATE -> BoardTheme.CLASSIC_WOOD
                        }
                        boardTheme = nextTheme
                    }) {
                        Icon(Icons.Default.Palette, contentDescription = "Change Board Theme")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // -------------------------------------------------------------
            // PLAY MODE SELECTION ROW
            // -------------------------------------------------------------
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = playMode == GamePlayMode.VS_AI,
                        onClick = {
                            playMode = GamePlayMode.VS_AI
                            viewModel.leaveOnlineRoom()
                        },
                        label = { Text("🤖 vs AI") },
                        leadingIcon = { Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = playMode == GamePlayMode.LOCAL_PASS_AND_PLAY,
                        onClick = {
                            playMode = GamePlayMode.LOCAL_PASS_AND_PLAY
                            viewModel.leaveOnlineRoom()
                        },
                        label = { Text("👥 2 Players") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = playMode == GamePlayMode.ONLINE_1V1,
                        onClick = {
                            playMode = GamePlayMode.ONLINE_1V1
                            showOnlineDialog = true
                        },
                        label = { Text("🌐 Live 1v1") },
                        leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0095F6),
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // -------------------------------------------------------------
            // AI DIFFICULTY CHIPS (ONLY IN VS_AI MODE)
            // -------------------------------------------------------------
            if (playMode == GamePlayMode.VS_AI) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Difficulty:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        FilterChip(
                            selected = aiLevel == 1,
                            onClick = { aiLevel = 1 },
                            label = { Text("Easy (L1)") }
                        )
                        FilterChip(
                            selected = aiLevel == 2,
                            onClick = { aiLevel = 2 },
                            label = { Text("Medium (L2)") }
                        )
                        FilterChip(
                            selected = aiLevel == 3,
                            onClick = { aiLevel = 3 },
                            label = { Text("Hard (L3)") }
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // ONLINE ROOM STATUS BAR (WHEN IN ONLINE MODE)
            // -------------------------------------------------------------
            if (playMode == GamePlayMode.ONLINE_1V1 && onlineRoom != null) {
                item {
                    val room = onlineRoom!!
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Room: ${room.roomId}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Chess Room Code", room.roomId))
                                            Toast.makeText(context, "Room Code Copied!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code", modifier = Modifier.size(16.dp))
                                    }
                                }
                                Text(
                                    text = if (room.status == "WAITING") "Waiting for friend to join..." else "Playing: ${room.hostUsername} vs ${room.guestUsername ?: "Guest"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Your Side: ${if (myOnlineColor == PieceColor.WHITE) "⚪ White" else "⚫ Black"}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.resignOnlineMatch(room.roomId, if (myOnlineColor == PieceColor.WHITE) "WHITE" else "BLACK")
                                    },
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Resign", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // TURN STATUS & CLOCK BANNER
            // -------------------------------------------------------------
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isMyTurnToMove) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(if (gameState.turn == PieceColor.WHITE) Color.White else Color.Black)
                                    .border(1.5.dp, Color.Gray, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = when {
                                    gameState.isGameOver -> "Game Concluded"
                                    isAiThinking -> "🤖 AI is thinking..."
                                    playMode == GamePlayMode.ONLINE_1V1 -> {
                                        if (isMyTurnToMove) "🟢 Your Turn (${if (myOnlineColor == PieceColor.WHITE) "White" else "Black"})"
                                        else "⏳ Friend is thinking..."
                                    }
                                    else -> "${if (gameState.turn == PieceColor.WHITE) "White" else "Black"} to move"
                                },
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        if (isAiThinking) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.5.dp)
                        } else {
                            Text(
                                text = "Theme: ${boardTheme.title}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // BLACK CAPTURED PIECES
            // -------------------------------------------------------------
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Captured by White: " + gameState.capturedByWhite.joinToString("") { it.type.symbolBlack },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Score: +${gameState.getWhiteScore()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // -------------------------------------------------------------
            // HIGH-FIDELITY CHESS BOARD WITH COORDINATES
            // -------------------------------------------------------------
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2E1C0C)) // Tournament Wooden Rim
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        for (r in 0..7) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left Rank Number (8 down to 1)
                                Text(
                                    text = (8 - r).toString(),
                                    color = Color(0xFFD7CCC8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(12.dp),
                                    textAlign = TextAlign.Center
                                )

                                for (c in 0..7) {
                                    val isLight = (r + c) % 2 == 0
                                    val sq = Square(r, c)
                                    val piece = gameState.board[r][c]
                                    val isSelected = selectedSquare == sq
                                    val isValidMove = validMovesForSelected.contains(sq)
                                    val isLastMove = lastMoveFrom == sq || lastMoveTo == sq

                                    val squareBg = when {
                                        isSelected -> Color(0xFFFFF176) // Bright golden selection
                                        isValidMove -> if (isLight) Color(0xFFC5E1A5) else Color(0xFF9CCC65)
                                        isLastMove -> if (isLight) Color(0xFFFFE082) else Color(0xFFFFD54F)
                                        isLight -> boardTheme.lightColor
                                        else -> boardTheme.darkColor
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .background(squareBg)
                                            .border(
                                                width = if (isSelected) 2.5.dp else 0.dp,
                                                color = if (isSelected) Color(0xFFF57F17) else Color.Transparent
                                            )
                                            .clickable { handleSquareClick(r, c) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Move target indicators
                                        if (isValidMove && piece == null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(11.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0x66000000))
                                            )
                                        } else if (isValidMove && piece != null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .border(2.5.dp, Color(0xFFD32F2F), CircleShape)
                                                    .background(Color(0x22D32F2F))
                                            )
                                        }

                                        // Premium crisp Chess Piece Rendering
                                        if (piece != null) {
                                            Text(
                                                text = if (piece.color == PieceColor.WHITE) piece.type.symbolWhite else piece.type.symbolBlack,
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (piece.color == PieceColor.WHITE) Color.White else Color(0xFF1B1B1B),
                                                style = LocalTextStyle.current.copy(
                                                    shadow = if (piece.color == PieceColor.WHITE) {
                                                        Shadow(
                                                            color = Color(0xAA000000),
                                                            offset = Offset(1.5f, 1.5f),
                                                            blurRadius = 3f
                                                        )
                                                    } else {
                                                        Shadow(
                                                            color = Color(0x55FFFFFF),
                                                            offset = Offset(1f, 1f),
                                                            blurRadius = 2f
                                                        )
                                                    }
                                                ),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom File Letters (a to h)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.width(12.dp))
                            listOf("a", "b", "c", "d", "e", "f", "g", "h").forEach { fileLetter ->
                                Text(
                                    text = fileLetter,
                                    color = Color(0xFFD7CCC8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Floating Reaction Animation overlay
                    AnimatedVisibility(
                        visible = floatingReaction != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shadowElevation = 8.dp
                        ) {
                            Text(
                                text = floatingReaction ?: "",
                                fontSize = 42.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // WHITE CAPTURED PIECES
            // -------------------------------------------------------------
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Captured by Black: " + gameState.capturedByBlack.joinToString("") { it.type.symbolWhite },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Score: +${gameState.getBlackScore()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // -------------------------------------------------------------
            // ONLINE QUICK EMOJI REACTIONS (WHEN IN ONLINE MODE)
            // -------------------------------------------------------------
            if (playMode == GamePlayMode.ONLINE_1V1 && onlineRoom != null) {
                item {
                    val room = onlineRoom!!
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("🤝", "👏", "🔥", "👑", "🎯", "⚡", "😮").forEach { emoji ->
                            OutlinedButton(
                                onClick = {
                                    viewModel.sendOnlineReaction(room.roomId, emoji)
                                    floatingReaction = emoji
                                    scope.launch {
                                        delay(2000L)
                                        floatingReaction = null
                                    }
                                },
                                modifier = Modifier.size(42.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text(emoji, fontSize = 18.sp)
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // PERSISTENT STATS CARD
            // -------------------------------------------------------------
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Chess Record & Performance",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            StatPill(label = "Player Wins", value = (chessStats?.playerWins ?: 0).toString())
                            StatPill(label = "AI Wins", value = (chessStats?.aiWins ?: 0).toString())
                            StatPill(label = "Draws", value = (chessStats?.draws ?: 0).toString())
                            StatPill(label = "Best Score", value = (chessStats?.bestScore ?: 0).toString())
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // CONTROLS (UNDO & RESET)
            // -------------------------------------------------------------
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (playMode != GamePlayMode.ONLINE_1V1) {
                        OutlinedButton(
                            onClick = {
                                if (isAiThinking || gameState.moveHistory.isEmpty()) return@OutlinedButton
                                if (playMode == GamePlayMode.VS_AI) {
                                    if (gameState.moveHistory.size >= 2) {
                                        gameState.undoMove()
                                        gameState.undoMove()
                                    } else {
                                        gameState.undoMove()
                                    }
                                } else {
                                    gameState.undoMove()
                                }
                                selectedSquare = null
                                validMovesForSelected = emptyList()
                                if (gameState.moveHistory.isNotEmpty()) {
                                    val last = gameState.moveHistory.last()
                                    lastMoveFrom = last.from
                                    lastMoveTo = last.to
                                } else {
                                    lastMoveFrom = null
                                    lastMoveTo = null
                                }
                                refreshKey++
                            },
                            enabled = !isAiThinking && gameState.moveHistory.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Undo")
                        }
                    }

                    Button(
                        onClick = {
                            gameState.resetBoard()
                            selectedSquare = null
                            validMovesForSelected = emptyList()
                            lastMoveFrom = null
                            lastMoveTo = null
                            if (playMode == GamePlayMode.ONLINE_1V1 && onlineRoom != null) {
                                viewModel.resetOnlineMatch(onlineRoom!!.roomId)
                            }
                            refreshKey++
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (playMode == GamePlayMode.ONLINE_1V1) "Rematch" else "New Game")
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // ONLINE 1V1 MULTIPLAYER MODAL (CREATE / JOIN)
    // -------------------------------------------------------------
    if (showOnlineDialog) {
        AlertDialog(
            onDismissRequest = { showOnlineDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Public, contentDescription = null, tint = Color(0xFF0095F6))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Live 1v1 Online with Friend", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Play chess in real-time synchronized over Firebase Cloud. Create a game room or enter your friend's code to start!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (onlineErrorMsg != null) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Text(
                                text = onlineErrorMsg!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Option A: Create Room
                    Button(
                        onClick = {
                            isConnectingOnline = true
                            onlineErrorMsg = null
                            viewModel.createOnlineChessRoom(
                                asWhite = true,
                                onSuccess = { code ->
                                    isConnectingOnline = false
                                    playMode = GamePlayMode.ONLINE_1V1
                                    showOnlineDialog = false
                                    gameState.resetBoard()
                                    Toast.makeText(context, "Room Created: $code! Share with your friend.", Toast.LENGTH_LONG).show()
                                },
                                onError = { err ->
                                    isConnectingOnline = false
                                    onlineErrorMsg = err
                                }
                            )
                        },
                        enabled = !isConnectingOnline,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0095F6)),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Match Room", fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.HorizontalDivider(modifier = Modifier.weight(1f))
                        Text("  OR JOIN CODE  ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        androidx.compose.material3.HorizontalDivider(modifier = Modifier.weight(1f))
                    }

                    // Option B: Join Room
                    OutlinedTextField(
                        value = joinRoomCodeInput,
                        onValueChange = {
                            joinRoomCodeInput = it.uppercase()
                            onlineErrorMsg = null
                        },
                        label = { Text("Friend's Room Code") },
                        placeholder = { Text("e.g. CHESS-7K2X") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(
                        onClick = {
                            if (joinRoomCodeInput.isBlank()) {
                                onlineErrorMsg = "Please enter room code."
                                return@OutlinedButton
                            }
                            isConnectingOnline = true
                            onlineErrorMsg = null
                            viewModel.joinOnlineChessRoom(
                                code = joinRoomCodeInput.trim(),
                                onSuccess = {
                                    isConnectingOnline = false
                                    playMode = GamePlayMode.ONLINE_1V1
                                    showOnlineDialog = false
                                    gameState.resetBoard()
                                    Toast.makeText(context, "Connected to match room!", Toast.LENGTH_SHORT).show()
                                },
                                onError = { err ->
                                    isConnectingOnline = false
                                    onlineErrorMsg = err
                                }
                            )
                        },
                        enabled = !isConnectingOnline && joinRoomCodeInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text("Join Friend's Room", fontWeight = FontWeight.SemiBold)
                    }

                    if (isConnectingOnline) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showOnlineDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // -------------------------------------------------------------
    // GAME OVER DIALOG
    // -------------------------------------------------------------
    if (showGameOverDialog) {
        AlertDialog(
            onDismissRequest = { showGameOverDialog = false },
            title = { Text("Game Result", fontWeight = FontWeight.Bold) },
            text = { Text(gameOverMessage, style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                Button(onClick = {
                    showGameOverDialog = false
                    gameState.resetBoard()
                    selectedSquare = null
                    validMovesForSelected = emptyList()
                    lastMoveFrom = null
                    lastMoveTo = null
                    if (playMode == GamePlayMode.ONLINE_1V1 && onlineRoom != null) {
                        viewModel.resetOnlineMatch(onlineRoom!!.roomId)
                    }
                    refreshKey++
                }) {
                    Text("Play Again")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGameOverDialog = false }) {
                    Text("Review Board")
                }
            }
        )
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
