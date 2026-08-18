// HomeScreen.kt - vendor discovery feed + search
package com.ooruva.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ooruva.app.data.models.Vendor

// Mock data
fun getMockVendors(): List<Vendor> = listOf(
    Vendor(
        id = "1",
        name = "Chai Wali",
        category = "FOOD",
        description = "Authentic Indian tea & snacks",
        latitude = 13.0827,
        longitude = 80.2707,
        address = "Main Street",
        phone = "9876543210",
        hours = "06:00-22:00",
        rating = 4.5f,
        reviewCount = 48,
        photoUrl = ""
    ),
    Vendor(
        id = "2",
        name = "Street Samosa",
        category = "FOOD",
        description = "Crispy samosas & pakora",
        latitude = 13.0835,
        longitude = 80.2715,
        address = "Market Road",
        phone = "9876543211",
        hours = "11:00-20:00",
        rating = 4.8f,
        reviewCount = 62,
        photoUrl = ""
    ),
    Vendor(
        id = "3",
        name = "Mobile Repair Pro",
        category = "SERVICE",
        description = "Phone repair & accessories",
        latitude = 13.0820,
        longitude = 80.2690,
        address = "Tech Lane",
        phone = "9876543212",
        hours = "09:00-19:00",
        rating = 4.2f,
        reviewCount = 35,
        photoUrl = ""
    ),
    Vendor(
        id = "4",
        name = "Beauty Studio",
        category = "SALON",
        description = "Hair & makeup services",
        latitude = 13.0840,
        longitude = 80.2720,
        address = "Fashion Street",
        phone = "9876543213",
        hours = "10:00-20:00",
        rating = 4.6f,
        reviewCount = 78,
        photoUrl = ""
    ),
    Vendor(
        id = "5",
        name = "Fresh Juice Corner",
        category = "FOOD",
        description = "Natural fruit juices",
        latitude = 13.0815,
        longitude = 80.2700,
        address = "Health Avenue",
        phone = "9876543214",
        hours = "07:00-19:00",
        rating = 4.7f,
        reviewCount = 92,
        photoUrl = ""
    )
)

@Composable
fun HomeScreen(
    onVendorClick: (Vendor) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    val vendors = getMockVendors()
    val filteredVendors = vendors.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.category.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(16.dp)
        ) {
            Text(
                text = "OORUVA",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Discover Street Vendors Around You",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }

        // Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp),
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (searchQuery.isEmpty()) {
                        Text(
                            "Search vendors...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            )
        }

        // Vendor List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(filteredVendors) { vendor ->
                VendorCard(
                    vendor = vendor,
                    onClick = { onVendorClick(vendor) }
                )
            }
        }
    }
}

@Composable
fun VendorCard(
    vendor: Vendor,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Photo Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = vendor.name.take(1),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            }

            // Content
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = vendor.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = vendor.category,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Rating
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                // Hours & Distance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Hours: ${vendor.hours}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "2.3 km away",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

