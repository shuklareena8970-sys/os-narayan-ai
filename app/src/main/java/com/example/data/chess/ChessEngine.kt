package com.example.data.chess

enum class PieceColor {
    WHITE, BLACK
}

enum class PieceType(val symbolWhite: String, val symbolBlack: String, val value: Int) {
    PAWN("♙", "♟", 1),
    KNIGHT("♘", "♞", 3),
    BISHOP("♗", "♝", 3),
    ROOK("♖", "♜", 5),
    QUEEN("♕", "♛", 9),
    KING("♔", "♚", 1000)
}

data class ChessPiece(
    val type: PieceType,
    val color: PieceColor
)

data class Square(val row: Int, val col: Int)

data class ChessMove(
    val from: Square,
    val to: Square,
    val piece: ChessPiece,
    val captured: ChessPiece? = null
)

class ChessGameState {
    val board = Array<Array<ChessPiece?>>(8) { arrayOfNulls(8) }
    var turn: PieceColor = PieceColor.WHITE
    val moveHistory = mutableListOf<ChessMove>()
    val capturedByWhite = mutableListOf<ChessPiece>()
    val capturedByBlack = mutableListOf<ChessPiece>()
    var isGameOver: Boolean = false
    var winner: PieceColor? = null
    var statusText: String = "White's turn to move"

    init {
        resetBoard()
    }

    fun resetBoard() {
        for (r in 0..7) {
            for (c in 0..7) {
                board[r][c] = null
            }
        }
        // Black pieces (row 0 & 1)
        board[0][0] = ChessPiece(PieceType.ROOK, PieceColor.BLACK)
        board[0][1] = ChessPiece(PieceType.KNIGHT, PieceColor.BLACK)
        board[0][2] = ChessPiece(PieceType.BISHOP, PieceColor.BLACK)
        board[0][3] = ChessPiece(PieceType.QUEEN, PieceColor.BLACK)
        board[0][4] = ChessPiece(PieceType.KING, PieceColor.BLACK)
        board[0][5] = ChessPiece(PieceType.BISHOP, PieceColor.BLACK)
        board[0][6] = ChessPiece(PieceType.KNIGHT, PieceColor.BLACK)
        board[0][7] = ChessPiece(PieceType.ROOK, PieceColor.BLACK)
        for (c in 0..7) {
            board[1][c] = ChessPiece(PieceType.PAWN, PieceColor.BLACK)
        }

        // White pieces (row 6 & 7)
        for (c in 0..7) {
            board[6][c] = ChessPiece(PieceType.PAWN, PieceColor.WHITE)
        }
        board[7][0] = ChessPiece(PieceType.ROOK, PieceColor.WHITE)
        board[7][1] = ChessPiece(PieceType.KNIGHT, PieceColor.WHITE)
        board[7][2] = ChessPiece(PieceType.BISHOP, PieceColor.WHITE)
        board[7][3] = ChessPiece(PieceType.QUEEN, PieceColor.WHITE)
        board[7][4] = ChessPiece(PieceType.KING, PieceColor.WHITE)
        board[7][5] = ChessPiece(PieceType.BISHOP, PieceColor.WHITE)
        board[7][6] = ChessPiece(PieceType.KNIGHT, PieceColor.WHITE)
        board[7][7] = ChessPiece(PieceType.ROOK, PieceColor.WHITE)

        turn = PieceColor.WHITE
        moveHistory.clear()
        capturedByWhite.clear()
        capturedByBlack.clear()
        isGameOver = false
        winner = null
        statusText = "White to move"
    }

    fun getValidMoves(sq: Square): List<Square> {
        val piece = board[sq.row][sq.col] ?: return emptyList()
        if (piece.color != turn) return emptyList()

        val moves = mutableListOf<Square>()
        val (r, c) = sq.row to sq.col

        fun inBounds(row: Int, col: Int) = row in 0..7 && col in 0..7

        fun addIfValid(row: Int, col: Int): Boolean {
            if (!inBounds(row, col)) return false
            val dest = board[row][col]
            if (dest == null) {
                moves.add(Square(row, col))
                return true
            } else if (dest.color != piece.color) {
                moves.add(Square(row, col))
                return false
            }
            return false
        }

        when (piece.type) {
            PieceType.PAWN -> {
                val dir = if (piece.color == PieceColor.WHITE) -1 else 1
                val startRow = if (piece.color == PieceColor.WHITE) 6 else 1

                // 1 forward
                if (inBounds(r + dir, c) && board[r + dir][c] == null) {
                    moves.add(Square(r + dir, c))
                    // 2 forward from start
                    if (r == startRow && board[r + 2 * dir][c] == null) {
                        moves.add(Square(r + 2 * dir, c))
                    }
                }
                // Diagonals capture
                for (dc in listOf(-1, 1)) {
                    val nr = r + dir
                    val nc = c + dc
                    if (inBounds(nr, nc)) {
                        val target = board[nr][nc]
                        if (target != null && target.color != piece.color) {
                            moves.add(Square(nr, nc))
                        }
                    }
                }
            }

            PieceType.KNIGHT -> {
                val knightOffsets = listOf(
                    -2 to -1, -2 to 1, -1 to -2, -1 to 2,
                    1 to -2, 1 to 2, 2 to -1, 2 to 1
                )
                for ((dr, dc) in knightOffsets) {
                    addIfValid(r + dr, c + dc)
                }
            }

            PieceType.BISHOP -> {
                val dirs = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
                for ((dr, dc) in dirs) {
                    var currR = r + dr
                    var currC = c + dc
                    while (inBounds(currR, currC)) {
                        val empty = addIfValid(currR, currC)
                        if (!empty) break
                        currR += dr
                        currC += dc
                    }
                }
            }

            PieceType.ROOK -> {
                val dirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
                for ((dr, dc) in dirs) {
                    var currR = r + dr
                    var currC = c + dc
                    while (inBounds(currR, currC)) {
                        val empty = addIfValid(currR, currC)
                        if (!empty) break
                        currR += dr
                        currC += dc
                    }
                }
            }

            PieceType.QUEEN -> {
                val dirs = listOf(
                    -1 to 0, 1 to 0, 0 to -1, 0 to 1,
                    -1 to -1, -1 to 1, 1 to -1, 1 to 1
                )
                for ((dr, dc) in dirs) {
                    var currR = r + dr
                    var currC = c + dc
                    while (inBounds(currR, currC)) {
                        val empty = addIfValid(currR, currC)
                        if (!empty) break
                        currR += dr
                        currC += dc
                    }
                }
            }

            PieceType.KING -> {
                for (dr in -1..1) {
                    for (dc in -1..1) {
                        if (dr != 0 || dc != 0) {
                            addIfValid(r + dr, c + dc)
                        }
                    }
                }
            }
        }

        return moves
    }

    fun makeMove(from: Square, to: Square): Boolean {
        if (isGameOver) return false
        val piece = board[from.row][from.col] ?: return false
        if (piece.color != turn) return false

        val validMoves = getValidMoves(from)
        if (!validMoves.contains(to)) return false

        val captured = board[to.row][to.col]
        if (captured != null) {
            if (piece.color == PieceColor.WHITE) {
                capturedByWhite.add(captured)
            } else {
                capturedByBlack.add(captured)
            }
            if (captured.type == PieceType.KING) {
                isGameOver = true
                winner = piece.color
                statusText = "Checkmate! ${piece.color.name} wins!"
            }
        }

        // Move piece
        board[to.row][to.col] = piece
        board[from.row][from.col] = null

        // Pawn promotion to Queen
        if (piece.type == PieceType.PAWN) {
            if (piece.color == PieceColor.WHITE && to.row == 0) {
                board[to.row][to.col] = ChessPiece(PieceType.QUEEN, PieceColor.WHITE)
            } else if (piece.color == PieceColor.BLACK && to.row == 7) {
                board[to.row][to.col] = ChessPiece(PieceType.QUEEN, PieceColor.BLACK)
            }
        }

        moveHistory.add(ChessMove(from, to, piece, captured))

        // Switch turn if game is still active
        if (!isGameOver) {
            turn = if (turn == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
            val nextMoves = getAllLegalMoves(turn)
            if (nextMoves.isEmpty()) {
                isGameOver = true
                winner = if (turn == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
                statusText = "Checkmate! ${winner?.name} wins!"
            } else {
                statusText = "${turn.name}'s turn"
            }
        }

        return true
    }

    fun undoMove(): Boolean {
        if (moveHistory.isEmpty()) return false
        val lastMove = moveHistory.removeAt(moveHistory.size - 1)
        board[lastMove.from.row][lastMove.from.col] = lastMove.piece
        board[lastMove.to.row][lastMove.to.col] = lastMove.captured
        if (lastMove.captured != null) {
            if (lastMove.piece.color == PieceColor.WHITE) {
                capturedByWhite.remove(lastMove.captured)
            } else {
                capturedByBlack.remove(lastMove.captured)
            }
        }
        turn = lastMove.piece.color
        isGameOver = false
        winner = null
        statusText = "${turn.name}'s turn"
        return true
    }

    fun getWhiteScore(): Int = capturedByWhite.sumOf { it.type.value }
    fun getBlackScore(): Int = capturedByBlack.sumOf { it.type.value }

    fun getAllLegalMoves(color: PieceColor): List<Pair<Square, Square>> {
        val result = mutableListOf<Pair<Square, Square>>()
        val savedTurn = turn
        turn = color
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c]
                if (piece != null && piece.color == color) {
                    val from = Square(r, c)
                    val moves = getValidMoves(from)
                    for (to in moves) {
                        result.add(from to to)
                    }
                }
            }
        }
        turn = savedTurn
        return result
    }

    // AI Move Generator (Levels 1 to 3)
    fun makeAiMove(level: Int): Boolean {
        if (isGameOver || turn != PieceColor.BLACK) return false

        val legalMoves = getAllLegalMoves(PieceColor.BLACK)
        if (legalMoves.isEmpty()) {
            isGameOver = true
            winner = PieceColor.WHITE
            statusText = "Checkmate! White wins!"
            return false
        }

        val chosenMove: Pair<Square, Square> = when (level) {
            1 -> {
                // Easy: Random or simple capture
                val captureMoves = legalMoves.filter { (_, to) -> board[to.row][to.col] != null }
                if (captureMoves.isNotEmpty() && Math.random() < 0.6) {
                    captureMoves.random()
                } else {
                    legalMoves.random()
                }
            }
            2 -> {
                // Medium: Highest capture value + center preference
                legalMoves.maxByOrNull { (from, to) ->
                    val captured = board[to.row][to.col]
                    val captureVal = (captured?.type?.value ?: 0) * 10
                    val centerBonus = 4 - Math.abs(to.row - 3.5).toInt() - Math.abs(to.col - 3.5).toInt()
                    captureVal + centerBonus
                } ?: legalMoves.random()
            }
            else -> {
                // Hard / Master: Evaluate tactical threats and king safety
                legalMoves.maxByOrNull { (from, to) ->
                    val movingPiece = board[from.row][from.col]
                    val captured = board[to.row][to.col]
                    var score = (captured?.type?.value ?: 0) * 20
                    // Check if capture is king
                    if (captured?.type == PieceType.KING) score += 10000
                    // Center dominance
                    val centerDist = Math.abs(to.row - 3.5) + Math.abs(to.col - 3.5)
                    score += ((7.0 - centerDist) * 2).toInt()
                    // Avoid moving high value pieces into immediate pawn capture
                    if (movingPiece?.type == PieceType.QUEEN && (to.row == 5 || to.row == 6)) {
                        score -= 5
                    }
                    score
                } ?: legalMoves.random()
            }
        }

        return makeMove(chosenMove.first, chosenMove.second)
    }
}
