package com.ooruva.app.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ooruva.app.data.remote.CatalogueItemDto
import com.ooruva.app.ui.components.PremiumButton
import com.ooruva.app.ui.theme.EyebrowStyle
import com.ooruva.app.ui.theme.Gold
import com.ooruva.app.ui.theme.OoruvaToolTheme
import com.ooruva.app.ui.theme.Spacing

/**
 * Vendor onboarding.
 *
 * One step per screenful, with the draft saved after each. The category and
 * type lists come from the database, so adding a business type is an admin
 * insert rather than an app release.
 */
@Composable
fun VendorOnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinished: () -> Unit,
    onExit: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.submitted) {
        SubmittedScreen(onFinished)
        return
    }

    OoruvaToolTheme {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            OnboardingHeader(
                step = state.step,
                onBack = { if (state.step == OnboardingStep.Basics) onExit() else viewModel.back() }
            )

            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md)
            ) {
                item {
                    Text(
                        state.step.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        state.step.blurb,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(Spacing.xl))
                }

                item { StepBody(state, viewModel) }

                state.error?.let { message ->
                    item {
                        Spacer(Modifier.height(Spacing.lg))
                        ErrorNote(message)
                    }
                }

                if (state.offlineDraft) {
                    item {
                        Spacer(Modifier.height(Spacing.lg))
                        // Said out loud rather than shown as a silent icon: the
                        // vendor needs to know their work is not yet safe.
                        InfoNote(
                            "Saved on this device only — we could not reach OORUVA. " +
                                "It will sync when you are back online."
                        )
                    }
                }

                item { Spacer(Modifier.height(Spacing.xl)) }
            }

            OnboardingFooter(state, viewModel)
        }
    }
}

@Composable
private fun StepBody(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    when (state.step) {
        OnboardingStep.Basics -> BasicsStep(state, viewModel)
        OnboardingStep.Category -> CategoryStep(state, viewModel)
        OnboardingStep.Type -> TypeStep(state, viewModel)
        OnboardingStep.Details -> DetailsStep(state, viewModel)
        OnboardingStep.Location -> LocationStep(state, viewModel)
        OnboardingStep.Hours -> HoursStep(state, viewModel)
        OnboardingStep.Catalogue -> CatalogueStep(state, viewModel)
        OnboardingStep.Photos -> PhotosStep(state, viewModel)
        OnboardingStep.Requirements -> RequirementsStep(state, viewModel)
        OnboardingStep.Terms -> TermsStep(state, viewModel)
        OnboardingStep.Review -> ReviewStep(state)
    }
}

// == Steps ===================================================================

@Composable
private fun BasicsStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    Column {
        ChoiceRow(
            label = "I already run this business",
            selected = state.draft.isExistingBusiness == true,
            onClick = { viewModel.update { it.copy(isExistingBusiness = true) } }
        )
        Spacer(Modifier.height(Spacing.sm))
        ChoiceRow(
            label = "I am starting something new",
            selected = state.draft.isExistingBusiness == false,
            onClick = { viewModel.update { it.copy(isExistingBusiness = false) } }
        )

        Spacer(Modifier.height(Spacing.xl))
        Field("Business name", state.draft.name) { v -> viewModel.update { it.copy(name = v) } }
        Spacer(Modifier.height(Spacing.lg))
        Field("Owner's name (optional)", state.draft.ownerName) { v ->
            viewModel.update { it.copy(ownerName = v) }
        }
    }
}

@Composable
private fun CategoryStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    if (state.loading) {
        LoadingRow("Loading categories")
        return
    }
    if (state.categories.isEmpty()) {
        InfoNote("No categories available. Check your connection and try again.")
        return
    }
    Column {
        state.categories.forEach { category ->
            ChoiceRow(
                label = category.name,
                detail = category.description,
                selected = state.draft.categoryId == category.id,
                onClick = { viewModel.chooseCategory(category.id) }
            )
            Spacer(Modifier.height(Spacing.sm))
        }
    }
}

@Composable
private fun TypeStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    if (state.types.isEmpty()) {
        LoadingRow("Loading business types")
        return
    }
    Column {
        state.types.forEach { type ->
            ChoiceRow(
                label = type.name,
                selected = state.draft.businessTypeId == type.id,
                onClick = { viewModel.chooseType(type.id) }
            )
            Spacer(Modifier.height(Spacing.sm))
        }
    }
}

@Composable
private fun DetailsStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    Column {
        Field(
            "Phone customers call",
            state.draft.phone,
            keyboard = KeyboardType.Phone,
            prefix = "+91",
        ) { v -> viewModel.update { it.copy(phone = v.filter { c -> c.isDigit() }.take(10)) } }
        Spacer(Modifier.height(Spacing.lg))
        Field("Address", state.draft.address) { v -> viewModel.update { it.copy(address = v) } }
        Spacer(Modifier.height(Spacing.lg))
        Field("District", state.draft.district) { v -> viewModel.update { it.copy(district = v) } }
        Spacer(Modifier.height(Spacing.lg))
        Field("What are you known for? (optional)", state.draft.description) { v ->
            viewModel.update { it.copy(description = v) }
        }
    }
}

@Composable
private fun LocationStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    Column {
        // Deliberately typed rather than map-picked for now. Wiring the map
        // picker needs a Maps key this build may not have, and a step that
        // silently fails to load is worse than a plain pair of fields.
        InfoNote(
            "If you do not know your coordinates, leave this blank. " +
                "Our team will place you on the map from your address."
        )
        Spacer(Modifier.height(Spacing.lg))
        Field(
            "Latitude (optional)",
            state.draft.latitude?.toString().orEmpty(),
            keyboard = KeyboardType.Decimal,
        ) { v -> viewModel.update { it.copy(latitude = v.toDoubleOrNull()) } }
        Spacer(Modifier.height(Spacing.lg))
        Field(
            "Longitude (optional)",
            state.draft.longitude?.toString().orEmpty(),
            keyboard = KeyboardType.Decimal,
        ) { v -> viewModel.update { it.copy(longitude = v.toDoubleOrNull()) } }
    }
}

@Composable
private fun HoursStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    Column {
        Field("Opening hours", state.draft.openingHours) { v ->
            viewModel.update { it.copy(openingHours = v) }
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(
            "For example: 06:00-22:00, closed Sundays",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CatalogueStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("item") }

    Column {
        state.draft.catalogue.forEachIndexed { index, item ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "₹${item.price} · ${item.kind}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "Remove",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.clickable { viewModel.removeCatalogueItem(index) }
                )
            }
        }

        if (state.draft.catalogue.isNotEmpty()) Spacer(Modifier.height(Spacing.lg))

        Field("Item name", name) { name = it }
        Spacer(Modifier.height(Spacing.md))
        Field("Price (₹)", price, keyboard = KeyboardType.Decimal) { price = it }
        Spacer(Modifier.height(Spacing.md))

        // Not a food-only app: a gift shop sells boxes and an electrician sells
        // a callout, so the kind is picked rather than assumed.
        Text("Kind", style = EyebrowStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            listOf("item", "box", "bundle", "package", "service").forEach { option ->
                KindChip(option, kind == option) { kind = option }
            }
        }

        Spacer(Modifier.height(Spacing.lg))
        PremiumButton(
            label = "Add",
            enabled = name.isNotBlank() && price.toDoubleOrNull() != null,
            onClick = {
                viewModel.addCatalogueItem(
                    CatalogueItemDto(
                        vendorId = "",          // filled in at submit
                        name = name.trim(),
                        price = price.toDouble(),
                        kind = kind,
                    )
                )
                name = ""
                price = ""
                kind = "item"
            }
        )
    }
}

@Composable
private fun PhotosStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.uploadPhoto(context, it) } }

    Column {
        state.draft.photoPaths.forEachIndexed { index, path ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        // The first upload becomes the listing's main image, so
                        // say so rather than letting the vendor discover it.
                        if (index == 0) "Main photo" else "Photo ${index + 1}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        path.substringAfterLast('/'),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "Remove",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.clickable { viewModel.removePhoto(path) }
                )
            }
        }

        if (state.draft.photoPaths.isNotEmpty()) Spacer(Modifier.height(Spacing.lg))

        PremiumButton(
            label = if (state.draft.photoPaths.isEmpty()) "Add a photo" else "Add another",
            loading = state.saving,
            enabled = !state.saving,
            onClick = { picker.launch("image/*") }
        )

        Spacer(Modifier.height(Spacing.md))
        Text(
            "JPG, PNG or WebP, up to 8 MB. You can add more from your dashboard later.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RequirementsStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    if (state.prompts.isEmpty()) {
        InfoNote("Nothing extra is needed for this kind of business.")
        return
    }

    Column {
        state.prompts.forEach { prompt ->
            val entry = state.draft.documents[prompt.key] ?: DocumentEntry(prompt.key)

            Text(prompt.label, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(Spacing.xs))

            Text(
                when (prompt) {
                    is RequirementPrompt.Mandatory -> "Required for this business type."
                    is RequirementPrompt.Optional -> "Optional."
                    // The wording matters. The database says requires_review,
                    // which means nobody has decided this applies -- so OORUVA
                    // asks rather than telling the vendor they need it.
                    is RequirementPrompt.AskVendor ->
                        "Do you have one? If you are not sure whether it applies to you, leave it blank and our team will advise."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(Spacing.sm))
            Field("${prompt.label} number", entry.number) { v ->
                viewModel.setDocument(entry.copy(number = v, declaredNotApplicable = false))
            }

            Spacer(Modifier.height(Spacing.md))
            DocumentPicker(prompt, entry, viewModel)

            if (prompt !is RequirementPrompt.Mandatory) {
                Spacer(Modifier.height(Spacing.xs))
                ChoiceRow(
                    label = "This does not apply to my business",
                    selected = entry.declaredNotApplicable,
                    onClick = {
                        viewModel.setDocument(
                            entry.copy(
                                declaredNotApplicable = !entry.declaredNotApplicable,
                                number = "",
                            )
                        )
                    }
                )
            }

            Spacer(Modifier.height(Spacing.xl))
        }
    }
}

@Composable
private fun DocumentPicker(
    prompt: RequirementPrompt,
    entry: DocumentEntry,
    viewModel: OnboardingViewModel,
) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.uploadDocument(context, prompt.key, it) } }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PremiumButton(
                label = when {
                    entry.uploading -> "Uploading"
                    entry.hasFile -> "Replace file"
                    else -> "Attach certificate"
                },
                loading = entry.uploading,
                enabled = !entry.uploading,
                outlined = true,
                // Images as well as PDFs: most vendors will photograph the
                // certificate with the same phone they are signing up on.
                onClick = { picker.launch("*/*") }
            )

            if (entry.hasFile) {
                Spacer(Modifier.width(Spacing.md))
                Icon(Icons.Default.Check, contentDescription = "Attached", tint = Gold)
            }
        }

        entry.uploadError?.let { message ->
            Spacer(Modifier.height(Spacing.sm))
            ErrorNote(message)
        }
    }
}

@Composable
private fun TermsStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    Column {
        Text(
            "You confirm the details you have given are accurate, that you are " +
                "authorised to list this business, and that OORUVA may show them " +
                "to customers once verified.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(Spacing.lg))
        ChoiceRow(
            label = "I agree to the vendor terms",
            selected = state.draft.termsAccepted,
            onClick = { viewModel.update { it.copy(termsAccepted = !it.termsAccepted) } }
        )
    }
}

@Composable
private fun ReviewStep(state: OnboardingUiState) {
    val d = state.draft
    Column {
        SummaryRow("Name", d.name)
        SummaryRow("Owner", d.ownerName.ifBlank { "Not given" })
        SummaryRow("Type", state.types.firstOrNull { it.id == d.businessTypeId }?.name ?: "Not set")
        SummaryRow("Phone", if (d.phone.isBlank()) "Not given" else "+91 ${d.phone}")
        SummaryRow("Address", d.address.ifBlank { "Not given" })
        SummaryRow("Hours", d.openingHours.ifBlank { "Not given" })
        SummaryRow("Items", "${d.catalogue.size}")
        SummaryRow("Photos", "${d.photoPaths.size}")
        SummaryRow(
            "Documents",
            // Counts files that actually reached the bucket, not intentions.
            "${d.documents.values.count { it.hasFile }} attached"
        )
        SummaryRow("Profile complete", "${d.completeness()}%")

        Spacer(Modifier.height(Spacing.xl))
        InfoNote(
            "Our team reviews new listings within 48 hours. " +
                "Nothing is shown to customers until then."
        )
    }
}

@Composable
private fun SubmittedScreen(onFinished: () -> Unit) {
    OoruvaToolTheme {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(Spacing.xl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Gold.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Gold)
            }
            Spacer(Modifier.height(Spacing.lg))
            Text("Sent for verification", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(Spacing.sm))
            Text(
                "We will review your listing within 48 hours and let you know. " +
                    "You can keep editing it from your dashboard in the meantime.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.xl))
            PremiumButton(label = "Go to dashboard", onClick = onFinished)
        }
    }
}

// == Chrome ==================================================================

@Composable
private fun OnboardingHeader(step: OnboardingStep, onBack: () -> Unit) {
    val steps = OnboardingStep.entries
    val index = steps.indexOf(step)

    Column(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.07f))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(Modifier.width(Spacing.md))
            Text(
                "Step ${index + 1} of ${steps.size}",
                style = EyebrowStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(Spacing.md))

        // A plain progress bar rather than eleven dots: at this many steps,
        // dots stop being countable and start being decoration.
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(
                Modifier
                    .fillMaxWidth((index + 1).toFloat() / steps.size)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Gold)
            )
        }
    }
}

@Composable
private fun OnboardingFooter(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    val isLast = state.step == OnboardingStep.Review
    Column(Modifier.padding(Spacing.lg)) {
        PremiumButton(
            label = if (isLast) "Submit for verification" else "Continue",
            loading = state.saving,
            enabled = state.draft.canAdvance(state.step) && !state.saving,
            onClick = { if (isLast) viewModel.submit() else viewModel.next() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// == Small pieces ============================================================

@Composable
private fun ChoiceRow(
    label: String,
    selected: Boolean,
    detail: String? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(
                1.dp,
                if (selected) Gold else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            detail?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = Gold)
    }
}

@Composable
private fun KindChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Gold.copy(alpha = 0.18f) else Color.Transparent)
            .border(
                1.dp,
                if (selected) Gold else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    keyboard: KeyboardType = KeyboardType.Text,
    prefix: String? = null,
    onValueChange: (String) -> Unit,
) {
    Column {
        Text(label, style = EyebrowStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            prefix?.let {
                Text(it, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.width(Spacing.xs))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(Gold),
                keyboardOptions = KeyboardOptions(keyboardType = keyboard),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(Spacing.xs))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = Spacing.xs)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(140.dp)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun InfoNote(message: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Gold.copy(alpha = 0.10f))
            .padding(Spacing.md)
    ) {
        Text(message, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ErrorNote(message: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(Spacing.md)
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun LoadingRow(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Gold)
        Spacer(Modifier.width(Spacing.md))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
