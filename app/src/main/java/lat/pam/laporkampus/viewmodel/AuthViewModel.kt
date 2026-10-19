package lat.pam.laporkampus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lat.pam.laporkampus.data.model.UserProfile
import lat.pam.laporkampus.data.repository.AuthRepository
import lat.pam.laporkampus.data.repository.FirebaseAuthRepository
import lat.pam.laporkampus.util.AuthInputValidator

/**
 * State UI untuk layar Login, Registrasi, dan sesi pengguna aktif.
 */
data class AuthUiState(
    val isCheckingSession: Boolean = true,
    val isLoading: Boolean = false,
    val currentUser: UserProfile? = null,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val authErrorMessage: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        restoreSession()
    }

    /**
     * Memeriksa apakah pengguna sudah login sebelumnya dan memuat profil perannya dari Firestore.
     */
    fun restoreSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingSession = true, authErrorMessage = null) }
            val result = authRepository.getCurrentUserProfile()
            result.fold(
                onSuccess = { profile ->
                    _uiState.update {
                        it.copy(
                            isCheckingSession = false,
                            currentUser = profile
                        )
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isCheckingSession = false,
                            currentUser = null
                        )
                    }
                }
            )
        }
    }

    fun clearErrors() {
        _uiState.update {
            it.copy(
                nameError = null,
                emailError = null,
                passwordError = null,
                confirmPasswordError = null,
                authErrorMessage = null
            )
        }
    }

    fun login(email: String, password: String) {
        val emailValid = AuthInputValidator.isValidEmail(email)
        val passwordValid = AuthInputValidator.isValidPassword(password)

        if (!emailValid || !passwordValid) {
            _uiState.update {
                it.copy(
                    emailError = if (!emailValid) "Format alamat email tidak valid" else null,
                    passwordError = if (!passwordValid) "Kata sandi minimal 6 karakter" else null,
                    authErrorMessage = null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    emailError = null,
                    passwordError = null,
                    authErrorMessage = null
                )
            }
            val result = authRepository.login(email, password)
            result.fold(
                onSuccess = { profile ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentUser = profile,
                            authErrorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            authErrorMessage = error.localizedMessage
                                ?: "Gagal masuk. Periksa kembali email dan kata sandi Anda."
                        )
                    }
                }
            )
        }
    }

    fun register(
        nama: String,
        email: String,
        password: String,
        confirmPassword: String
    ) {
        val nameValid = AuthInputValidator.isValidName(nama)
        val emailValid = AuthInputValidator.isValidEmail(email)
        val passwordValid = AuthInputValidator.isValidPassword(password)
        val confirmValid = AuthInputValidator.doPasswordsMatch(password, confirmPassword)

        if (!nameValid || !emailValid || !passwordValid || !confirmValid) {
            _uiState.update {
                it.copy(
                    nameError = if (!nameValid) "Nama lengkap tidak boleh kosong" else null,
                    emailError = if (!emailValid) "Format alamat email tidak valid" else null,
                    passwordError = if (!passwordValid) "Kata sandi minimal 6 karakter" else null,
                    confirmPasswordError = if (!confirmValid) "Konfirmasi kata sandi tidak cocok" else null,
                    authErrorMessage = null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    nameError = null,
                    emailError = null,
                    passwordError = null,
                    confirmPasswordError = null,
                    authErrorMessage = null
                )
            }
            val result = authRepository.registerMahasiswa(nama, email, password)
            result.fold(
                onSuccess = { profile ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentUser = profile,
                            authErrorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            authErrorMessage = error.localizedMessage
                                ?: "Gagal mendaftarkan akun. Silakan coba lagi."
                        )
                    }
                }
            )
        }
    }

    fun logout() {
        authRepository.logout()
        _uiState.update {
            AuthUiState(isCheckingSession = false, currentUser = null)
        }
    }
}
