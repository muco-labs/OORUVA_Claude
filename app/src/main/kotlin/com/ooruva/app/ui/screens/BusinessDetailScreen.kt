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
fun BusinessDetailScreen(
    vendor: Vendor,
    onBackClick: () -> Unit = {},
    onCheckIn: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Text(
                        text = "Business Details",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share"
                        )
                    }
                }

                // Photo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = vendor.name.take(1),
                        fontSize = 64.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                }

                // Details
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = vendor.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Row(
                        modifier = Modifier.padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(5) { index ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Star",
                                tint = if (index < vendor.rating.toInt())
                                    Color(0xFFFFC107) else Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = " ${vendor.rating} (${vendor.reviewCount} reviews)",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    // Info Cards
                    InfoCard(
                        icon = Icons.Default.LocationOn,
                        title = "Address",
                        value = vendor.address
                    )

                    InfoCard(
                        icon = Icons.Default.Schedule,
                        title = "Hours",
                        value = vendor.hours
                    )

                    InfoCard(
                        icon = Icons.Default.Phone,
                        title = "Phone",
                        value = vendor.phone
                    )

                    // Check-in Button
                    Button(
                        onClick = onCheckIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(vertical = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Check-in",
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 8.dp)
                        )
                        Text("Check-in Here")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Community Posts
                    Text(
                        text = "Community Posts",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    CommunityPostCard(
                        userName = "Sarah",
                        caption = "Amazing chai and snacks! Best in the area ☕",
                        likes = 23
                    )

                    CommunityPostCard(
                        userName = "Raj",
                        caption = "Fresh ingredients, quick service. Highly recommend!",
                        likes = 18
                    )
                }
            }
        }
    }
}

@Composable
fun InfoCard(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun CommunityPostCard(
    userName: String,
    caption: String,
    likes: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = userName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = caption,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Red
                    )
                    Text(
                        text = "$likes likes",
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                Text(
                    text = "2 days ago",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

