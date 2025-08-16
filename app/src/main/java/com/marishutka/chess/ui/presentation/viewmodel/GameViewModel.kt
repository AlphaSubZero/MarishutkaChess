package com.marishutka.chess.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marishutka.chess.domain.model.*
import com.marishutka.chess.data.network.FirebaseGameService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GameUiState(
    val boardState: BoardState = BoardState(),
    val selectedSquare: Position? = null,
    val possibleMoves: List<Position> = emptyList(),
    val isPlayerTurn: Boolean = true,
    val gameResult: GameResult = GameResult.ONGOING
)

class GameViewModel : ViewModel() {

    private val engine = ChessEngine()
    private val firebase = FirebaseGameService() // вызовы заглушены до добавления google-services.json

    private val _ui = MutableStateFlow(GameUiState(boardState = engine.getBoardState()))
    val gameUiState: GameUiState get() = _ui.value
    val stateFlow: StateFlow<GameUiState> = _ui.asStateFlow()

    fun startLocalGame() {
        // уже инициализировано двигателем
        _ui.value = GameUiState(boardState = engine.getBoardState())
    }

    fun createOnlineGame() {
        viewModelScope.launch {
            firebase.createGameSkeleton() // каркасный вызов без ошибок сборки
        }
    }

    fun onSquareClick(pos: Position) {
        val sel = _ui.value.selectedSquare
        if (sel == null) {
            // выбрать свою фигуру
            engine.getPiece(pos)?.let {
                if (it.color == _ui.value.boardState.activeColor) {
                    val moves = engine.getLegalMoves().filter { m -> m.from == pos }.map { it.to }
                    _ui.value = _ui.value.copy(selectedSquare = pos, possibleMoves = moves)
                }
            }
        } else {
            val legal = engine.getLegalMoves().firstOrNull { it.from == sel && it.to == pos }
            if (legal != null) {
                val result = engine.makeMove(legal)
                val newBoard = engine.getBoardState()
                _ui.value = _ui.value.copy(
                    boardState = newBoard,
                    selectedSquare = null,
                    possibleMoves = emptyList(),
                    gameResult = (result as? MoveResult.Valid)?.gameResult ?: _ui.value.gameResult
                )
                // TODO: здесь можно дернуть firebase.makeMoveUci(legal.toUci())
            } else {
                _ui.value = _ui.value.copy(selectedSquare = null, possibleMoves = emptyList())
            }
        }
    }
}
