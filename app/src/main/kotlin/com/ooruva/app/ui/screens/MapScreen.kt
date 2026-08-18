package com.ooruva.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ooruva.app.ui.components.OoruvaMark
import com.ooruva.app.ui.components.SectionHeader
import com.ooruva.app.ui.theme.EyebrowStyle
import com.ooruva.app.ui.theme.Espresso
import com.ooruva.app.ui.theme.Gold
import com.ooruva.app.ui.theme.OutlineWarm

@Composable
fun MapScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Muted map canvas — warm paper, espresso roads, gold pins. Swap for a
        // GoogleMap with this same style JSON once the API key is wired.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(Color(0xFFEFE9DE))
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val roads = listOf(
                    Pair(Offset(0f, size.height * 0.34f), Offset(size.width, size.height * 0.28f)),
                    Pair(Offset(0f, size.height * 0.68f), Offset(size.width, size.height * 0.74f)),
                    Pair(Offset(size.width * 0.28f, 0f), Offset(size.width * 0.34f, size.height)),
                    Pair(Offset(size.width * 0.74f, 0f), Offset(size.width * 0.66f, size.height)),
                )
                roads.forEach { (start, end) ->
                    drawLine(
                        color = Color(0xFFE0D8C9),
                        start = start,
                        end = end,
                        strokeWidth = 18f
                    )
                }
                // Vendor pins in gold, the user in espresso
                listOf(
                    Offset(size.width * 0.32f, size.height * 0.30f),
                    Offset(size.width * 0.70f, size.height * 0.42f),
                    Offset(size.width * 0.45f, size.height * 0.72f),
                ).forEach { point ->
                    drawCircle(color = Gold, radius = 13f, center = point)
                    drawCircle(color = Color.White, radius = 5f, center = point)
                }
                drawCircle(
                    color = Espresso.copy(alpha = 0.12f),
                    radius = 46f,
                    center = Offset(size.width * 0.5f, size.height * 0.52f)
                )
                drawCircle(
                    color = Espresso,
                    radius = 11f,
                    center = Offset(size.width * 0.5f, size.height * 0.52f)
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OoruvaMark(size = 30)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "T. NAGAR",
                        style = EyebrowStyle,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Three within a kilometre",
                        style = MaterialTheme.typography.titleMedium,
                        color = Espresso
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.85f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "MAPS KEY NOT SET · PREVIEW",
                    style = EyebrowStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        SectionHeader(
            eyebrow = "Nearest first",
            title = "Vendors near you",
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(Modifier.height(8.dp))

        val nearby = listOf(
            Triple("Chai Wali", "0.5 km", "Open until 22:00"),
            Triple("Street Samosa", "1.2 km", "Open until 20:00"),
            Triple("Fresh Juice Corner", "1.8 km", "Open until 19:00"),
        )

        nearby.forEachIndexed { index, item ->
            val (name, distance, hours) = item
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = distance,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(62.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = hours,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.NavigateNext,
                    contentDescription = "Open",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (index != nearby.lastIndex) {
                HorizontalDivider(
                    color = OutlineWarm,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}
