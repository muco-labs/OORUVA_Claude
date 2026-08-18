package com.ooruva.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.ooruva.app.data.models.UserRole
import com.ooruva.app.ui.components.GoldUnderline
import com.ooruva.app.ui.components.MucoLabsCredit
import com.ooruva.app.ui.components.OoruvaMark
import com.ooruva.app.ui.components.PremiumButton
import com.ooruva.app.ui.theme.Brand
import com.ooruva.app.ui.theme.EyebrowStyle
import com.ooruva.app.ui.theme.Gold
import com.ooruva.app.ui.theme.GoldBright
import com.ooruva.app.ui.theme.NightOnBg
import com.ooruva.app.ui.theme.Spacing
import kotlinx.coroutines.delay


@Composable
fun PhoneAuthScreen(
    role: UserRole,
    headline: String,
    blurb: String,
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit,
) {
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showOtp by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var secondsLeft by remember { mutableIntStateOf(0) }

    // 30-second resend countdown, restarted every time a code is "sent".
    LaunchedEffect(showOtp, secondsLeft) {
        if (showOtp && secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brand.EspressoWash)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = Spacing.xl)
        ) {
            Spacer(Modifier.height(Spacing.md))

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.07f))
                    .clickable {
                        if (showOtp) {
                            showOtp = false
                            otp = ""
                            errorMessage = ""
                        } else onBack()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = NightOnBg,
                    modifier = Modifier.size(17.dp)
                )
            }

            Spacer(Modifier.height(Spacing.xl))

            OoruvaMark(size = 60)

            Spacer(Modifier.height(Spacing.lg))

            Text(
                text = if (showOtp) "Check your\nmessages." else headline,
                style = MaterialTheme.typography.displayLarge,
                color = NightOnBg
            )

            Spacer(Modifier.height(Spacing.md))

            Text(
                text = if (showOtp) "We sent a six-digit code to +91 " + phone else blurb,
                style = MaterialTheme.typography.bodyLarge,
                color = NightOnBg.copy(alpha = 0.55f)
            )

            Spacer(Modifier.height(48.dp))

            if (!showOtp) {
                UnderlineField(
                    value = phone,
                    onValueChange = { if (it.length <= 10) phone = it.filter { c -> c.isDigit() } },
                    label = "Phone number",
                    placeholder = "98765 43210",
                    prefix = "+91"
                )
            } else {
                UnderlineField(
                    value = otp,
                    onValueChange = { if (it.length <= 6) otp = it.filter { c -> c.isDigit() } },
                    label = "Verification code",
                    placeholder = "······"
                )
            }

            ErrorLine(errorMessage)

            Spacer(Modifier.height(Spacing.xl))

            PremiumButton(
                label = if (showOtp) "Verify and continue" else "Send OTP",
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                loading = isLoading,
                enabled = if (showOtp) otp.isNotEmpty() else phone.isNotEmpty(),
                onClick = {
                    if (!showOtp) {
                        if (phone.length == 10) {
                            errorMessage = ""
                            showOtp = true
                            secondsLeft = 30
                        } else {
                            errorMessage = "That needs to be ten digits"
                        }
                    } else {
                        if (otp.length == 6) {
                            errorMessage = ""
                            isLoading = true
                            android.util.Log.d(
                                "OORUVA",
                                "Signed in as " + role.name + " with phone " + phone
                            )
                            onLoginSuccess()
                        } else {
                            errorMessage = "The code is six digits"
                        }
                    }
                }
            )

            if (showOtp) {
                Spacer(Modifier.height(Spacing.md))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (secondsLeft > 0) {
                        Text(
                            text = "Resend in " + secondsLeft + "s",
                            style = MaterialTheme.typography.bodySmall,
                            color = NightOnBg.copy(alpha = 0.4f)
                        )
                    } else {
                        Text(
                            text = "Resend code",
                            style = MaterialTheme.typography.labelMedium,
                            color = GoldBright,
                            modifier = Modifier.clickable {
                                secondsLeft = 30
                                otp = ""
                            }
                        )
                    }
                    Spacer(Modifier.width(Spacing.md))
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.bodySmall,
                        color = NightOnBg.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.width(Spacing.md))
                    Text(
                        text = "Change number",
                        style = MaterialTheme.typography.labelMedium,
                        color = NightOnBg.copy(alpha = 0.55f),
                        modifier = Modifier.clickable {
                            showOtp = false
                            phone = ""
                            otp = ""
                            errorMessage = ""
                        }
                    )
                }
            }

            Spacer(Modifier.height(64.dp))

            Text(
                text = "By continuing you agree to our Terms of Service and Privacy Policy.",
                style = MaterialTheme.typography.bodySmall,
                color = NightOnBg.copy(alpha = 0.32f)
            )

            Spacer(Modifier.height(Spacing.lg))
            MucoLabsCredit(onDark = true)
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun UnderlineField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    prefix: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    Column(Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = EyebrowStyle,
            color = if (focused) Gold else NightOnBg.copy(alpha = 0.45f)
        )
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (prefix != null) {
                Text(
                    text = prefix,
                    style = MaterialTheme.typography.headlineMedium,
                    color = NightOnBg.copy(alpha = 0.5f)
                )
                Spacer(Modifier.width(14.dp))
            }
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.headlineMedium,
                        color = NightOnBg.copy(alpha = 0.22f)
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    interactionSource = interaction,
                    cursorBrush = SolidColor(Gold),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.headlineMedium.copy(color = NightOnBg),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        GoldUnderline(active = focused)
    }
}

@Composable
private fun ErrorLine(message: String) {
    AnimatedVisibility(visible = message.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
        Row(
            modifier = Modifier.padding(top = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFD9897C)
            )
        }
    }
}
