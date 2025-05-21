package com.bluebridge.bluebridgeapp.utils

object InputSanitizer {
    private val SAFE_USERNAME_REGEX = Regex("^[a-zA-Z0-9_.-]{3,30}$")
    private val SAFE_NAME_REGEX = Regex("^[a-zA-Z\\s-']{2,50}$")
    private val EMAIL_REGEX = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
    private val PASSWORD_REGEX = Regex("^[\\x20-\\x7E]{8,72}$") // Printable ASCII chars only

    fun sanitizeUsername(username: String): String? {
        val trimmed = username.trim()
        return if (SAFE_USERNAME_REGEX.matches(trimmed)) {
            trimmed
        } else null
    }

    fun sanitizeName(name: String): String? {
        val trimmed = name.trim()
        return if (SAFE_NAME_REGEX.matches(trimmed)) {
            trimmed
        } else null
    }

    fun sanitizeEmail(email: String): String? {
        val trimmed = email.trim().lowercase()
        return if (EMAIL_REGEX.matches(trimmed)) {
            trimmed
        } else null
    }

    fun validatePassword(password: String): Boolean {
        return PASSWORD_REGEX.matches(password) &&
               password.length >= 8 &&
               password.any { it.isUpperCase() } &&
               password.any { it.isLowerCase() } &&
               password.any { it.isDigit() } &&
               password.any { !it.isLetterOrDigit() }
    }

    fun sanitizeInput(input: String): String {
        return input.replace(Regex("[\\x00-\\x1F\\x7F<>\"'&;]"), "")
    }

    data class ValidationResult(
        val isValid: Boolean,
        val sanitizedValue: String? = null,
        val errorMessage: String? = null
    )

    fun validateUserInput(
        email: String,
        password: String,
        username: String? = null,
        firstName: String? = null,
        lastName: String? = null
    ): Map<String, ValidationResult> {
        val results = mutableMapOf<String, ValidationResult>()

        // Validate email
        val sanitizedEmail = sanitizeEmail(email)
        results["email"] = ValidationResult(
            isValid = sanitizedEmail != null,
            sanitizedValue = sanitizedEmail,
            errorMessage = if (sanitizedEmail == null) "Invalid email format" else null
        )

        // Validate password
        results["password"] = ValidationResult(
            isValid = validatePassword(password),
            sanitizedValue = if (validatePassword(password)) password else null,
            errorMessage = if (!validatePassword(password)) 
                "Password must be 8-72 characters long and include uppercase, lowercase, number, and special character" 
            else null
        )

        // Optional fields for registration
        if (username != null) {
            val sanitizedUsername = sanitizeUsername(username)
            results["username"] = ValidationResult(
                isValid = sanitizedUsername != null,
                sanitizedValue = sanitizedUsername,
                errorMessage = if (sanitizedUsername == null) 
                    "Username must be 3-30 characters long and contain only letters, numbers, dots, underscores, or hyphens" 
                else null
            )
        }

        if (firstName != null) {
            val sanitizedFirstName = sanitizeName(firstName)
            results["firstName"] = ValidationResult(
                isValid = sanitizedFirstName != null,
                sanitizedValue = sanitizedFirstName,
                errorMessage = if (sanitizedFirstName == null) 
                    "First name must contain only letters, spaces, hyphens, or apostrophes" 
                else null
            )
        }

        if (lastName != null) {
            val sanitizedLastName = sanitizeName(lastName)
            results["lastName"] = ValidationResult(
                isValid = sanitizedLastName != null,
                sanitizedValue = sanitizedLastName,
                errorMessage = if (sanitizedLastName == null) 
                    "Last name must contain only letters, spaces, hyphens, or apostrophes" 
                else null
            )
        }

        return results
    }
} 