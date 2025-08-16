package com.marishutka.chess.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marishutka.chess.presentation.viewmodel.GameViewModel
import com.marishutka.chess.domain.model.*

@Composable
fun GameScreen(vm: GameViewModel = viewModel()) {
    val s = vm.gameUiState

    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                when (s.gameResult) {
                    GameResult.ONGOING -> if (s.isPlayerTurn) "Ваш ход" else "Ход соперника"
                    GameResult.WHITE_WINS -> "Мат. Победа белых"
                    GameResult.BLACK_WINS -> "Мат. Победа чёрных"
                    GameResult.DRAW_STALEMATE -> "Пат"
                    GameResult.DRAW_AGREEMENT -> "Ничья по соглашению"
                    GameResult.DRAW_INSUFFICIENT_MATERIAL -> "Ничья: недостаточный материал"
                    GameResult.DRAW_THREEFOLD_REPETITION -> "Ничья: троекратное повторение"
                    GameResult.DRAW_FIFTY_MOVE_RULE -> "Ничья: правило 50 ходов"
                    GameResult.ABANDONED -> "Партия прервана"
                }
            )

            Spacer(Modifier.height(8.dp))

            ChessBoard(
                board = s.boardState.squares,
                selected = s.selectedSquare,
                moves = s.possibleMoves,
                onClick = vm::onSquareClick
            )
        }
    }
}

@Composable
private fun ChessBoard(
    board: Array<Array<ChessPiece?>>,
    selected: Position?,
    moves: List<Position>,
    onClick: (Position) -> Unit
) {
    LazyVerticalGrid(columns = GridCells.Fixed(8), modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .padding(4.dp)) {

        items((0..63).toList()) { idx ->
            val row = 7 - idx / 8
            val col = idx % 8
            val pos = Position(row, col)
            val piece = board[row][col]
            val isLight = (row + col) % 2 == 0
            val isSel = selected == pos
            val isMove = moves.contains(pos)

            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .background(
                        when {
                            isSel -> Color(0xFF2196F3)
                            isMove -> Color(0x774CAF50)
                            isLight -> Color(0xFFF0D9B5)
                            else -> Color(0xFFB58863)
                        }
                    )
                    .border(1.dp, Color.Black.copy(0.05f))
                    .clickable { onClick(pos) },
                contentAlignment = Alignment.Center
            ) {
                piece?.let {
                    val res = it.getDrawableResourceId()
                    Image(
                        painter = painterResource(res),
                        contentDescription = "${it.color} ${it.type}",
                        modifier = Modifier.fillMaxSize(0.8f)
                    )
                }
            }
        }
    }
}
