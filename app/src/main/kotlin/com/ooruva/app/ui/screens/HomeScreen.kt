package com.ooruva.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.ooruva.app.data.models.Vendor
import com.ooruva.app.ui.components.EditorialHeader
import com.ooruva.app.ui.components.GoldRating
import com.ooruva.app.ui.components.HeroTile
import com.ooruva.app.ui.components.MetaPill
import com.ooruva.app.ui.components.SectionHeader
import com.ooruva.app.ui.theme.EyebrowStyle
import com.ooruva.app.ui.theme.Gold
import com.ooruva.app.ui.theme.ForestLight
import com.ooruva.app.ui.theme.pressScale

// Mock data
fun getMockVendors(): List<Vendor> = listOf(
    Vendor(
        id = "1", name = "Chai Wali", category = "CHAI",
        description = "Authentic Indian tea & snacks",
        latitude = 13.0827, longitude = 80.2707,
        address = "Main Street", phone = "9876543210",
        hours = "06:00-22:00", rating = 4.5f, reviewCount = 48, photoUrl = ""
    ),
    Vendor(
        id = "2", name = "Street Samosa", category = "FOOD",
        description = "Crispy samosas & pakora",
        latitude = 13.0835, longitude = 80.2715,
        address = "Market Road", phone = "9876543211",
        hours = "11:00-20:00", rating = 4.8f, reviewCount = 62, photoUrl = ""
    ),
    Vendor(
        id = "3", name = "Mobile Repair Pro", category = "SERVICE",
        description = "Phone repair & accessories",
        latitude = 13.0820, longitude = 80.2690,
        address = "Tech Lane", phone = "9876543212",
        hours = "09:00-19:00", rating = 4.2f, reviewCount = 35, photoUrl = ""
    ),
    Vendor(
        id = "4", name = "Beauty Studio", category = "SALON",
        description = "Hair & makeup services",
        latitude = 13.0840, longitude = 80.2720,
        address = "Fashion Street", phone = "9876543213",
        hours = "10:00-20:00", rating = 4.6f, reviewCount = 78, photoUrl = ""
    ),
    Vendor(
        id = "5", name = "Fresh Juice Corner", category = "JUICE",
        description = "Natural fruit juices",
        latitude = 13.0815, longitude = 80.2700,
        address = "Health Avenue", phone = "9876543214",
        hours = "07:00-19:00", rating = 4.7f, reviewCount = 92, photoUrl = ""
    )
)

private val categories = listOf("ALL", "CHAI", "FOOD", "JUICE", "SERVICE", "SALON", "SHOP")

@Composable
fun HomeScreen(
    onVendorClick: (Vendor) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    val favourites = remember { mutableStateMapOf<String, Boolean>() }
    val vendors = remember { getMockVendors() }

    val filtered = vendors.filter { vendor ->
        val matchesQuery = searchQuery.isBlank() ||
            vendor.name.contains(searchQuery, ignoreCase = true) ||
            vendor.category.contains(searchQuery, ignoreCase = true) ||
            vendor.description.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "ALL" || vendor.category == selectedCategory
        matchesQuery && matchesCategory
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            EditorialHeader(
                eyebrow = "T. Nagar, Chennai",
                title = "What are we\neating today?"
            )
        }

        item { SearchField(searchQuery) { searchQuery = it } }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    CategoryChip(
                        label = if (category == "ALL") "Everything"
                        else category.lowercase().replaceFirstChar { it.uppercase() },
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category }
                    )
                }
            }
        }

        item {
            SectionHeader(
                eyebrow = "Around you",
                title = if (filtered.size == 1) "One place nearby"
                else "" + filtered.size + " places nearby",
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(18.dp))
        }

        if (filtered.isEmpty()) item { EmptyState(searchQuery) }

        items(filtered, key = { it.id }) { vendor ->
            VendorCard(
                vendor = vendor,
                favourite = favourites[vendor.id] == true,
                onFavourite = { favourites[vendor.id] = favourites[vendor.id] != true },
                onClick = { onVendorClick(vendor) },
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = "Search",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(17.dp)
        )
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    "Search chai, samosa, repairs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                cursorBrush = SolidColor(Gold),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            )
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun VendorCard(
    vendor: Vendor,
    favourite: Boolean,
    onFavourite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Box {
            HeroTile(name = vendor.name, category = vendor.category, height = 200, corner = 32)

            // Open state, quiet
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (vendor.isClosed) MaterialTheme.colorScheme.error else ForestLight)
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = if (vendor.isClosed) "CLOSED" else "OPEN NOW",
                    style = EyebrowStyle,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x3DFAF7F1))
                    .clickable(onClick = onFavourite),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (favourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Save",
                    tint = if (favourite) Gold else Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }

            Text(
                text = vendor.category,
                style = EyebrowStyle,
                color = Color.White.copy(alpha = 0.78f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = vendor.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            GoldRating(rating = vendor.rating, compact = true)
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = vendor.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetaPill(text = vendor.hours)
            MetaPill(text = "2.3 km away", tint = MaterialTheme.colorScheme.primary)
            MetaPill(text = "" + vendor.reviewCount + " reviews")
        }
    }
}

@Composable
private fun EmptyState(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Nothing on this street yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (query.isBlank()) "Try another category."
            else "No vendor matches \"" + query + "\".",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
