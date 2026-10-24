package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.NarayanViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OfflineSyncBanner(viewModel: NarayanViewModel) {
    val syncState by viewModel.syncUiState.collectAsState()

    val formattedSyncTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(syncState.lastSyncTime))

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("offline_sync_banner"),
        shape = RoundedCornerShape(10.dp),
        color = when {
            !syncState.isOnline -> Color(0xFFFFF3E0) // Warm offline orange
            syncState.isSyncing -> Color(0xFFE8F0FE) // Light syncing blue
            syncState.pendingCount > 0 -> Color(0xFFFFF8E1) // Pending amber
            else -> Color(0xFFE8F5E9) // Clean sync green
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clickable { viewModel.triggerSync() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                !syncState.isOnline -> Color(0xFFE65100)
                                syncState.isSyncing -> Color(0xFF1976D2)
                                syncState.pendingCount > 0 -> Color(0xFFF57F17)
                                else -> Color(0xFF2E7D32)
                            }
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        !syncState.isOnline -> "Offline Mode: Changes saved safely to device (${syncState.pendingCount} pending sync)"
                        syncState.isSyncing -> "Syncing ${syncState.pendingCount} changes to server..."
                        syncState.pendingCount > 0 -> "${syncState.pendingCount} local changes pending sync"
                        else -> "All changes synced • Last sync $formattedSyncTime"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        !syncState.isOnline -> Color(0xFFBF360C)
                        syncState.isSyncing -> Color(0xFF0D47A1)
                        syncState.pendingCount > 0 -> Color(0xFFE65100)
                        else -> Color(0xFF1B5E20)
                    }
                )
            }

            if (syncState.isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF1976D2)
                )
            } else {
                Icon(
                    imageVector = if (!syncState.isOnline) Icons.Default.CloudOff else if (syncState.pendingCount > 0) Icons.Default.Sync else Icons.Default.CloudDone,
                    contentDescription = "Sync Status",
                    modifier = Modifier.size(16.dp),
                    tint = if (!syncState.isOnline) Color(0xFFE65100) else Color(0xFF2E7D32)
                )
            }
        }
    }
}
