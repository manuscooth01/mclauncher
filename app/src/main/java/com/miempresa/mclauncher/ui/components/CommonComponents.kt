package com.miempresa.mclauncher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miempresa.mclauncher.VersionsUiState
import com.miempresa.mclauncher.ui.theme.*

@Composable
fun VersionGridCard(
    versionId: String,
    versionType: String,
    isInstalled: Boolean,
    onCardClick: () -> Unit
) {
    val accent = when (versionType) {
        "release" -> NeonGreen
        "snapshot" -> CyberCyan
        "old_beta" -> StatusWarn
        "old_alpha" -> StatusError
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(4.dp)) // Reduced elevation
            .clickable { onCardClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f) // Slightly transparent
        ),
        border = BorderStroke(
            0.5.dp, // Thinner border
            if (isInstalled) NeonGreen.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp) // Reduced padding
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = versionId,
                fontSize = 13.sp, // Reduced font size
                fontWeight = FontWeight.Medium, // Reduced weight
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp)) // Reduced spacing
            // Simplified version type badge - just text with background
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 1.dp)
                    .background(accent.copy(alpha = 0.1f), shape = RoundedCornerShape(2.dp))
            ) {
                Text(
                    text = versionType.uppercase(),
                    fontSize = 8.sp, // Reduced font size
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    letterSpacing = 0.5.sp
                )
            }
            if (isInstalled) {
                Spacer(modifier = Modifier.height(4.dp)) // Reduced spacing
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(12.dp) // Slightly smaller icon
                    )
                    Spacer(modifier = Modifier.width(2.dp)) // Reduced spacing
                    Text(
                        text = "INSTALADO",
                        fontSize = 8.sp, // Reduced font size
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadProgressIndicator(progress: com.miempresa.mclauncher.VersionManager.DownloadProgress?) {
    if (progress == null) return
    Column(modifier = Modifier.padding(vertical = 2.dp)) { // Reduced padding
        LinearProgressIndicator(
            progress = if (progress.total > 0)
                progress.current.toFloat() / progress.total else 0f,
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp) // Thinner progress bar
                .clip(RoundedCornerShape(1.dp)),
            color = NeonGreen,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp)) // Reduced spacing
        Text(
            text = "[${progress.phase}] ${progress.detail}",
            fontSize = 9.sp, // Reduced font size
            color = MaterialTheme.colorScheme.outline
        )
    }
}
