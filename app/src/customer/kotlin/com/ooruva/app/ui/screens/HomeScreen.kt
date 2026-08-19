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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.runtime.LaunchedEffect
import com.ooruva.app.data.mock.getMockVendors
import com.ooruva.app.data.remote.DataResult
import com.ooruva.app.data.remote.Supabase
import com.ooruva.app.data.repository.BusinessRepository
import com.ooruva.app.data.repository.TaxonomyRepository
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

/**
 * Categories are no longer declared here. They come from business_categories,
 * so adding one is an admin insert rather than an app release. The only literal
 * left is the "everything" pseudo-category, which is a UI affordance rather
 * than a row in the taxonomy.
 */
private const val ALL_SLUG = "__all__"

@Composable
fun HomeScreen(
    onOpenGroupFinder: () -> Unit = {},
    onVendorClick: (Vendor) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ALL_SLUG) }
    val favourites = remember { mutableStateMapOf<String, Boolean>() }
    var state by remember { mutableStateOf(DiscoveryState()) }

    LaunchedEffect(Unit) {
        // Not configured: fall back to the seed set so the screen is usable for
        // design work, but flag it so the UI can say these are samples. Showing
        // invented shops as real places is the one thing discovery must not do.
        if (!Supabase.isConfigured) {
            state = state.copy(
                businesses = getMockVendors(),
                loading = false,
                fromSampleData = true,
            )
            return@LaunchedEffect
        }

        val categories = when (val result = TaxonomyRepository.categories()) {
            is DataResult.Success -> result.data
            else -> emptyList()
        }

        state = when (val nearby = BusinessRepository.nearby(
            lat = DEFAULT_ORIGIN_LAT,
            lng = DEFAULT_ORIGIN_LNG,
            radiusKm = 25.0,
        )) {
            is DataResult.Success -> state.copy(
                categories = categories,
                businesses = nearby.data.map { it.toVendor() },
                loading = false,
            )
            is DataResult.Failure -> state.copy(
                categories = categories,
                loading = false,
                error = "Could not load businesses near you. Check your connection.",
            )
            DataResult.Loading -> state.copy(categories = categories)
        }
    }

    val filtered = state.businesses.filter { vendor ->
        val matchesQuery = searchQuery.isBlank() ||
            vendor.name.contains(searchQuery, ignoreCase = true) ||
            vendor.category.contains(searchQuery, ignoreCase = true) ||
            vendor.description.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == ALL_SLUG ||
            vendor.category.equals(selectedCategory, ignoreCase = true)
        matchesQuery && matchesCategory
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 108.dp)
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
                CategoryChip(
                    label = "Everything",
                    selected = selectedCategory == ALL_SLUG,
                    onClick = { selectedCategory = ALL_SLUG }
                )
                state.categories.forEach { category ->
                    CategoryChip(
                        label = category.name,
                        selected = selectedCategory == category.slug,
                        onClick = { selectedCategory = category.slug }
                    )
                }
            }
        }

        if (state.fromSampleData) {
            item { SampleDataBanner() }
        }

        state.error?.let { message ->
            item { DiscoveryError(message) }
        }

        item { GroupFinderCard(onOpenGroupFinder) }

        item {
            SectionHeader(
                eyebrow = "Around you",
                title = when {
                    state.loading -> "Looking nearby"
                    filtered.size == 1 -> "One place nearby"
                    else -> "" + filtered.size + " places nearby"
                },
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(18.dp))
        }

        if (filtered.isEmpty() && !state.loading) item { EmptyState(searchQuery) }

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

/**
 * Group Finder used to sit on the navigation bar. It is a task, not a place,
 * so it belongs in the flow of Home where the question actually occurs.
 */
@Composable
private fun GroupFinderCard(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .pressScale(interaction)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "EATING TOGETHER?",
                style = EyebrowStyle,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Split a budget across the group",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.width(12.dp))
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(18.dp)
        )
    }
    Spacer(Modifier.height(24.dp))
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

/**
 * Shown when the app is running without a backend. Deliberately unmissable:
 * a customer must never mistake seed data for shops they can actually visit.
 */
@Composable
private fun SampleDataBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Gold.copy(alpha = 0.12f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Sample listings. These are not real businesses \u2014 " +
                "OORUVA is not connected to its backend on this build.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DiscoveryError(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}
