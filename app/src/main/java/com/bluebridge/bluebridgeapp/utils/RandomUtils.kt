package com.bluebridge.bluebridgeapp.utils

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import java.security.MessageDigest
import java.util.Base64





@RequiresApi(Build.VERSION_CODES.O)
fun encryptPassword(password: String): String {
    val bytes = password.toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return Base64.getEncoder().encodeToString(digest)
}





// Password strength logic reused for signup
data class PasswordStrength(val strength: String, val color: Color, val message: String)
fun getPasswordStrength(password: String): PasswordStrength {
    val hasMinLength = password.length >= 8
    val hasUpperCase = password.any { it.isUpperCase() }
    val hasLowerCase = password.any { it.isLowerCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSpecialChar = password.any { !it.isLetterOrDigit() }
    val met = listOf(hasMinLength, hasUpperCase, hasLowerCase, hasDigit, hasSpecialChar).count { it }
    return when {
        password.isEmpty() -> PasswordStrength("Empty", Color.Gray, "Enter a password")
        !hasMinLength && met <= 1 -> PasswordStrength("Very Weak", Color.Red.copy(alpha = 0.7f), "Too short and simple")
        met == 1 -> PasswordStrength("Weak", Color(0xFFFF6B6B), "Add uppercase/numbers/special chars")
        met == 2 -> PasswordStrength("Medium", Color(0xFFFFB84D), "Good start, add variety")
        met <= 4 -> PasswordStrength("Strong", Color(0xFF59C135), "Strong password")
        else -> PasswordStrength("Very Strong", Color(0xFF2E7D32), "Excellent password!")
    }
}
