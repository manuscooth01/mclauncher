package com.miempresa.mclauncher.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miempresa.mclauncher.SettingsManager
import com.miempresa.mclauncher.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    var ram by remember { mutableStateOf(viewModel.settingsManager.ramMb) }
    var username by remember { mutableStateOf(viewModel.settingsManager.username) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("AJUSTES", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF4444))

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("RAM (MB): $ram", fontSize = 16.sp, color = Color.White)
            Slider(
                value = ram.toFloat(),
                onValueChange = { ram = it.roundToInt() },
                valueRange = 512f..4096f,
                steps = 12,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.SliderDefaults.colors(activeTrackColor = Color(0xFF00FF00), inactiveTrackColor = Color(0xFF333333), thumbColor = Color(0xFF00FF00))
            )
        }

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Usuario", fontSize = 16.sp, color = Color.White)
            androidx.compose.material3.OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFF333333),
                    focusedBorderColor = Color(0xFF00FF00),
                    unfocusedContainerColor = Color(0xFF1A1A1A),
                    focusedContainerColor = Color(0xFF1A1A1A),
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White
                )
            )
        }

        Button(
            onClick = {
                viewModel.settingsManager.ramMb = ram.coerceIn(512, 4096)
                viewModel.settingsManager.username = username
            },
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF00), contentColor = Color.Black)
        ) {
            Text("GUARDAR", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}