package com.marishutka.chess.data.network

class FirebaseGameService {
    suspend fun createGameSkeleton(): Boolean = true
    // сюда позже добавишь реальную работу с Realtime Database:
    // auth, createGame, joinGame, makeMoveUci, listen, etc.
}
