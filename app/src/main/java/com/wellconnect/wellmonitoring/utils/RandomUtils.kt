package com.wellconnect.wellmonitoring.utils

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.Color
import com.wellconnect.wellmonitoring.data.WellDataStore
import com.wellconnect.wellmonitoring.network.LocationData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64

fun exchangeWells(fromIndex: Int, toIndex: Int, wellDataStore: WellDataStore, scope: CoroutineScope) {
    scope.launch {
        val currentList = wellDataStore.wellListFlow.first().toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val temp = currentList[fromIndex]
            currentList[fromIndex] = currentList[toIndex]
            currentList[toIndex] = temp

            wellDataStore.saveWellList(currentList)
        }
    }
}


fun resetErrorMessage(errorMessage: MutableState<String?>) {
    errorMessage.value = null
}

@RequiresApi(Build.VERSION_CODES.O)
fun encryptPassword(password: String): String {
    val bytes = password.toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return Base64.getEncoder().encodeToString(digest)
}


// Add this function to parse the location input
@RequiresApi(Build.VERSION_CODES.O)
fun parseLocationInput(input: String): LocationData? {
    val latRegex = Regex("""lat:\s*(-?\d+\.\d+)""")
    val lonRegex = Regex("""lon:\s*(-?\d+\.\d+)""")

    val latMatch = latRegex.find(input)
    val lonMatch = lonRegex.find(input)

    return if (latMatch != null && lonMatch != null) {
        val latitude = latMatch.groups[1]?.value?.toDoubleOrNull()
        val longitude = lonMatch.groups[1]?.value?.toDoubleOrNull()
        if (latitude != null && longitude != null) {
            LocationData(latitude, longitude, LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
        } else {
            null // Return null if parsing fails
        }
    } else {
        null // Return null if regex doesn't match
    }
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
