package com.ooruva.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ooruva.app.ui.components.EditorialHeader
import com.ooruva.app.ui.components.GoldRating
import com.ooruva.app.ui.components.MetaPill
import com.ooruva.app.ui.components.PremiumCard
import com.ooruva.app.ui.components.SectionHeader
import com.ooruva.app.ui.theme.EyebrowStyle
import com.ooruva.app.ui.theme.Gold

private val groupCategories = listOf("Everything", "Chai", "Food", "Juice", "Shop", "Service")

@Composable
fun GroupFinderScreen() {
    var peopleCount by remember { mutableStateOf(4) }
    var budget by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Everything") }
    var showResults by remember { mutableStateOf(false) }

    val budgetValue = budget.toIntOrNull() ?: 0
    val perPerson = if (peopleCount > 0 && budgetValue > 0) budgetValue / peopleCount else 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 108.dp)
    ) {
        item {
            EditorialHeader(
                eyebrow = "Group finder",
                title = "Who is\neating?",
                subtitle = "Tell us the headcount and the budget. We will find the places that fit."
            )
        }

        item {
            Column(Modifier.padding(horizontal = 24.dp)) {
                // Headcount
                Text("HEADCOUNT", style = EyebrowStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepperButton(Icons.Default.Remove, "Fewer") {
                        if (peopleCount > 1) peopleCount--
                    }
                    Text(
                        text = peopleCount.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    StepperButton(Icons.Default.Add, "More") {
                        if (peopleCount < 50) peopleCount++
                    }
                }

                Spacer(Modifier.height(32.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(28.dp))

                // Budget
                Text("TOTAL BUDGET", style = EyebrowStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "₹",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (budget.isEmpty()) {
                            Text(
                                text = "1500",
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        }
                        BasicTextField(
                            value = budget,
                            onValueChange = { if (it.length <= 7) budget = it.filter { c -> c.isDigit() } },
                            singleLine = true,
                            cursorBrush = SolidColor(Gold),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = MaterialTheme.typography.displaySmall.copy(
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                // The hero number
                AnimatedVisibility(visible = perPerson > 0, enter = fadeIn() + expandVertically()) {
                    Column {
                        Spacer(Modifier.height(28.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "₹" + perPerson,
                                style = MaterialTheme.typography.displayLarge,
                                color = Gold
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "per person",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(30.dp))

                // Category
                Text("NARROW IT DOWN", style = EyebrowStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupCategories.forEach { category ->
                        val selected = selectedCategory == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                )
                                .border(
                                    1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 14.dp, vertical = 9.dp)
                        ) {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = { showResults = true },
                    enabled = perPerson > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = Color.White
                    )
                ) {
                    Text("Find places for my group", style = MaterialTheme.typography.labelLarge)
                }

                Spacer(Modifier.height(36.dp))
            }
        }

        if (showResults && perPerson > 0) {
            item {
                SectionHeader(
                    eyebrow = "" + peopleCount + " people · ₹" + budgetValue,
                    title = "Three that fit",
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(Modifier.height(18.dp))
            }

            val picks = listOf(
                Triple("Street Samosa", 4.8f, "Comfortably within budget"),
                Triple("Chai Wali", 4.5f, "Room for a second round"),
                Triple("Fresh Juice Corner", 4.7f, "Just about right")
            )

            items(picks.size) { index ->
                val (name, rating, note) = picks[index]
                PremiumCard(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    corner = 16
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "₹" + perPerson,
                                style = MaterialTheme.typography.headlineSmall,
                                color = Gold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GoldRating(rating = rating, compact = true)
                            Spacer(Modifier.width(12.dp))
                            MetaPill(text = "per person")
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(19.dp)
        )
    }
}
