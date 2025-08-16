package com.marishutka.chess.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marishutka.chess.presentation.viewmodel.GameViewModel

@Composable
fun MainScreen(vm: GameViewModel = viewModel()) {
    val ui = vm.gameUiState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("♕ Marishutka Chess ♕", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Button(onClick = { vm.startLocalGame() }) { Text("Играть локально") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { vm.createOnlineGame() }) { Text("Онлайн (Firebase)") }
        Spacer(Modifier.height(24.dp))

        // Простой холст для доски
        GameScreen(vm)
    }
}
