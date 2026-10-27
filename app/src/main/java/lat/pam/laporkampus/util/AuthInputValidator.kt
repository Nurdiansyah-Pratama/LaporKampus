package lat.pam.laporkampus.util

/**
 * Validator murni untuk input autentikasi (Login & Registrasi) agar dapat diuji melalui Unit Test.
 */
object AuthInputValidator {

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val USERNAME_ID_REGEX = Regex("^[A-Za-z0-9._-]{4,50}$")
    const val MIN_PASSWORD_LENGTH = 6

    // Kredensial Seed Petugas Admin / Satgas
    const val SEED_STAFF_ID = "1237050139"
    const val SEED_STAFF_EMAIL = "1237050139@laporkampus.ac.id"
    const val SEED_STAFF_PASSWORD = "admin123"
    const val SEED_STAFF_NAME = "Petugas Admin & Satgas (1237050139)"

    fun isValidName(name: String): Boolean {
        return name.trim().length >= 2
    }

    fun isValidEmail(email: String): Boolean {
        return EMAIL_REGEX.matches(email.trim())
    }

    /**
     * Memvalidasi input login yang dapat berupa alamat email kampus ATAU
     * ID/Username pengguna (seperti NIM/NIP `1237050139`).
     */
    fun isValidLoginIdentifier(identifier: String): Boolean {
        val trimmed = identifier.trim()
        return isValidEmail(trimmed) || USERNAME_ID_REGEX.matches(trimmed)
    }

    /**
     * Memeriksa apakah kredensial yang dimasukkan adalah akun seed Admin/Satgas (`1237050139` / `admin123`).
     */
    fun isSeedStaffCredentials(identifier: String, password: String): Boolean {
        val trimmed = identifier.trim()
        val matchesIdOrEmail = trimmed.equals(SEED_STAFF_ID, ignoreCase = true) ||
            trimmed.equals(SEED_STAFF_EMAIL, ignoreCase = true)
        return matchesIdOrEmail && password == SEED_STAFF_PASSWORD
    }

    /**
     * Mengonversi ID/Username tanpa domain menjadi format email standar Firebase Auth.
     */
    fun resolveLoginEmail(identifier: String): String {
        val trimmed = identifier.trim()
        return if (trimmed.contains("@")) {
            trimmed
        } else {
            "$trimmed@laporkampus.ac.id"
        }
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= MIN_PASSWORD_LENGTH
    }

    fun doPasswordsMatch(password: String, confirmPassword: String): Boolean {
        return password.isNotEmpty() && password == confirmPassword
    }
}
