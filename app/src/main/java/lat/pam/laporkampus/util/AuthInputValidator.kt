package lat.pam.laporkampus.util

/**
 * Validator murni untuk input autentikasi (Login & Registrasi) agar dapat diuji melalui Unit Test.
 */
object AuthInputValidator {

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    const val MIN_PASSWORD_LENGTH = 6

    fun isValidName(name: String): Boolean {
        return name.trim().length >= 2
    }

    fun isValidEmail(email: String): Boolean {
        return EMAIL_REGEX.matches(email.trim())
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= MIN_PASSWORD_LENGTH
    }

    fun doPasswordsMatch(password: String, confirmPassword: String): Boolean {
        return password.isNotEmpty() && password == confirmPassword
    }
}
