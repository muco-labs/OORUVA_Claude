package com.ooruva.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ooruva.app.ui.components.GoldUnderline
import com.ooruva.app.ui.components.MucoLabsCredit
import com.ooruva.app.ui.components.OoruvaMark
import com.ooruva.app.ui.components.OoruvaWordmark
import com.ooruva.app.ui.theme.Brand
import com.ooruva.app.ui.theme.Gold
import com.ooruva.app.ui.theme.GoldBright
import com.ooruva.app.ui.theme.NightBg
import com.ooruva.app.ui.theme.NightOnBg

@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit = {}
) {
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showOTPField by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

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
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Generous top whitespace is the point
            Spacer(Modifier.height(96.dp))

            OoruvaMark(size = 76)

            Spacer(Modifier.height(36.dp))

            Text(
                text = if (showOTPField) "Check your\nmessages." else "Welcome to\nOoruva.",
                style = MaterialTheme.typography.displayLarge,
                color = NightOnBg
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = if (showOTPField) "We sent a six-digit code to +91 " + phone
                else "The chai stalls, samosa carts and juice corners of your street — kept.",
                style = MaterialTheme.typography.bodyLarge,
                color = NightOnBg.copy(alpha = 0.55f)
            )

            Spacer(Modifier.height(56.dp))

            if (!showOTPField) {
                UnderlineField(
                    value = phone,
                    onValueChange = { if (it.length <= 10) phone = it },
                    label = "Phone number",
                    placeholder = "98765 43210",
                    prefix = "+91",
                )
            } else {
                UnderlineField(
                    value = otp,
                    onValueChange = { if (it.length <= 6) otp = it },
                    label = "Verification code",
                    placeholder = "······",
                )
            }

            ErrorLine(errorMessage)

            Spacer(Modifier.height(40.dp))

            val enabled = if (showOTPField) otp.isNotEmpty() else phone.isNotEmpty()
            Button(
                onClick = {
                    if (!showOTPField) {
                        if (phone.length == 10) {
                            errorMessage = ""
                            showOTPField = true
                        } else {
                            errorMessage = "That needs to be ten digits"
                        }
                    } else {
                        if (otp.length == 6) {
                            errorMessage = ""
                            isLoading = true
                            android.util.Log.d("OORUVA", "Login successful with phone: " + phone)
                            onLoginSuccess()
                        } else {
                            errorMessage = "The code is six digits"
                        }
                    }
                },
                enabled = enabled && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = NightBg,
                    disabledContainerColor = Color.White.copy(alpha = 0.07f),
                    disabledContentColor = NightOnBg.copy(alpha = 0.30f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = NightBg
                    )
                } else {
                    Text(
                        text = if (showOTPField) "Verify and continue" else "Continue",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            if (showOTPField) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        showOTPField = false
                        phone = ""
                        otp = ""
                        errorMessage = ""
                    }
                ) {
                    Text(
                        "Use a different number",
                        style = MaterialTheme.typography.labelMedium,
                        color = GoldBright.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(Modifier.height(72.dp))

            Text(
                text = "By continuing you agree to our Terms of Service and Privacy Policy.",
                style = MaterialTheme.typography.bodySmall,
                color = NightOnBg.copy(alpha = 0.32f),
                textAlign = TextAlign.Start
            )

            Spacer(Modifier.height(26.dp))
            MucoLabsCredit(onDark = true)
            Spacer(Modifier.height(28.dp))
        }
    }
}

/**
 * Gold underline rather than a boxed outline — the field recedes and the
 * typography leads.
 */
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
            style = com.ooruva.app.ui.theme.EyebrowStyle,
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
