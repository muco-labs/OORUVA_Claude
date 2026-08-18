package com.ooruva.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ooruva.app.data.models.Vendor
import com.ooruva.app.ui.components.GoldRating
import com.ooruva.app.ui.components.HeroTile
import com.ooruva.app.ui.components.PremiumCard
import com.ooruva.app.ui.components.SectionHeader
import com.ooruva.app.ui.theme.EyebrowStyle
import com.ooruva.app.ui.theme.ForestLight
import com.ooruva.app.ui.theme.Gold

@Composable
fun BusinessDetailScreen(
    vendor: Vendor,
    onBackClick: () -> Unit = {},
    onCheckIn: () -> Unit = {}
) {
    var favourite by remember { mutableStateOf(false) }
    var checkedIn by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
            item {
                Box {
                    HeroTile(
                        name = vendor.name,
                        category = vendor.category,
                        height = 320,
                        corner = 0
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        GlassIcon(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBackClick)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GlassIcon(
                                icon = if (favourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                label = "Save",
                                onClick = { favourite = !favourite },
                                tint = if (favourite) Gold else Color.White
                            )
                            GlassIcon(Icons.Default.Share, "Share", onClick = {})
                        }
                    }
                }
            }

            // Signature move: the identity card floats over the hero's bottom edge.
            item {
                PremiumCard(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .offset(y = (-40).dp),
                    corner = 24
                ) {
                    Column(Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = vendor.category,
                                style = EyebrowStyle,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(10.dp))
                            Box(
                                Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(if (vendor.isClosed) MaterialTheme.colorScheme.error else ForestLight)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (vendor.isClosed) "CLOSED" else "OPEN NOW",
                                style = EyebrowStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = vendor.name,
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = vendor.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        GoldRating(rating = vendor.rating, reviews = vendor.reviewCount)
                    }
                }
            }

            item {
                Column(
                    Modifier
                        .padding(horizontal = 24.dp)
                        .offset(y = (-20).dp)
                ) {
                    InfoRow(Icons.Default.LocationOn, "Address", vendor.address)
                    InfoRow(Icons.Default.Schedule, "Hours", vendor.hours)
                    InfoRow(Icons.Default.Phone, "Phone", vendor.phone)

                    Spacer(Modifier.height(28.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(26.dp))

                    SectionHeader(eyebrow = "From the street", title = "What people say")
                    Spacer(Modifier.height(18.dp))

                    ReviewRow("Sarah", "Amazing chai and snacks. Best in the area.", 23, "2 days ago")
                    Spacer(Modifier.height(20.dp))
                    ReviewRow("Raj", "Fresh ingredients, quick service. Highly recommend.", 18, "5 days ago")
                }
            }
        }

        // Check-in rests on the ivory, not in a heavy bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp)
                .padding(top = 12.dp, bottom = 20.dp)
        ) {
            Button(
                onClick = {
                    checkedIn = true
                    onCheckIn()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (checkedIn) MaterialTheme.colorScheme.secondary else Gold,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (checkedIn) "Checked in · 50 points earned" else "Check in here",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun GlassIcon(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = Color.White,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(0x471C1917))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun InfoRow(icon: ImageVector, title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(17.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = title.uppercase(),
            style = EyebrowStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(84.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun ReviewRow(name: String, caption: String, likes: Int, ago: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(1),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = ago.uppercase(),
                    style = EyebrowStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = caption,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "" + likes + " found this helpful",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
