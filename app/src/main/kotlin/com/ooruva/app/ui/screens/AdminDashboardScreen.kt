package com.ooruva.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ooruva.app.data.models.Vendor

@Composable
fun AdminDashboardScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFD32F2F))
                .padding(16.dp)
        ) {
            Text(
                text = "Admin Dashboard",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "System Overview",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Key Metrics", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminStatCard("2.5K", "Users", "👥")
                    AdminStatCard("450", "Vendors", "🏪")
                    AdminStatCard("12.5K", "Check-ins", "✓")
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            }

            item {
                Text("Pending Actions", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            }

            item {
                AdminActionCard("Vendor Verification", "23 pending", Icons.Default.VerifiedUser, Color(0xFFFFC107))
            }

            item {
                AdminActionCard("Review Moderation", "8 flagged", Icons.Default.Flag, Color(0xFFE91E63))
            }

            item {
                AdminActionCard("Fraud Detection", "2 suspicious", Icons.Default.Security, Color(0xFFD32F2F))
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            }

            item {
                Text("Recent Activity", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            }

            item {
                AdminActivityLog("New vendor signup", "5 min ago")
                AdminActivityLog("Review reported", "12 min ago")
                AdminActivityLog("User check-in", "23 min ago")
            }
        }
    }
}

@Composable
fun AdminStatCard(value: String, label: String, emoji: String) {
    Card(
        modifier = Modifier
            .weight(1f)
            .height(100.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = emoji, fontSize = 24.sp, modifier = Modifier.padding(bottom = 4.dp))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AdminActionCard(title: String, count: String, icon: ImageVector, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(32.dp),
                    tint = color
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = count, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Navigate")
        }
    }
}

@Composable
fun AdminActivityLog(action: String, time: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = action, fontSize = 13.sp)
        Text(text = time, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
