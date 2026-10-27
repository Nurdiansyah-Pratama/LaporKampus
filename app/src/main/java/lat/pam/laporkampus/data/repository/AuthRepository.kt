package lat.pam.laporkampus.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import lat.pam.laporkampus.data.firebase.FirestoreCollections
import lat.pam.laporkampus.data.model.UserProfile
import lat.pam.laporkampus.data.model.UserRole
import lat.pam.laporkampus.util.AuthInputValidator

/**
 * Kontrak repositori autentikasi dan manajemen profil peran pengguna.
 */
interface AuthRepository {
    val currentUserId: String?
    suspend fun login(email: String, password: String): Result<UserProfile>
    suspend fun registerMahasiswa(nama: String, email: String, password: String): Result<UserProfile>
    suspend fun getCurrentUserProfile(): Result<UserProfile?>
    fun logout()
}

/**
 * Implementasi [AuthRepository] menggunakan Firebase Authentication dan Cloud Firestore.
 *
 * Aturan Keamanan #1:
 * Registrasi dari aplikasi HANYA diizinkan membuat dokumen pengguna dengan `peran = MAHASISWA`.
 */
class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AuthRepository {

    override val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    override suspend fun login(email: String, password: String): Result<UserProfile> {
        // Penanganan khusus untuk akun Seed Admin/Satgas (ID: 1237050139, Password: admin123)
        if (AuthInputValidator.isSeedStaffCredentials(email, password)) {
            return loginOrProvisionSeedStaff()
        }

        val resolvedEmail = AuthInputValidator.resolveLoginEmail(email)
        return try {
            val authResult = firebaseAuth
                .signInWithEmailAndPassword(resolvedEmail, password)
                .await()
            val user = authResult.user
                ?: return Result.failure(IllegalStateException("Pengguna tidak ditemukan setelah masuk."))
            fetchOrCreateProfile(user.uid, user.displayName ?: "", user.email ?: resolvedEmail)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Masuk atau otomatis membuat akun Seed Petugas Admin & Satgas (`1237050139` / `admin123`)
     * di Firebase Authentication dan menyimpan profilnya di koleksi `users`.
     */
    private suspend fun loginOrProvisionSeedStaff(): Result<UserProfile> {
        val seedEmail = AuthInputValidator.SEED_STAFF_EMAIL
        val seedPassword = AuthInputValidator.SEED_STAFF_PASSWORD

        val firebaseUser = try {
            firebaseAuth.signInWithEmailAndPassword(seedEmail, seedPassword).await().user
        } catch (_: Exception) {
            try {
                firebaseAuth.createUserWithEmailAndPassword(seedEmail, seedPassword).await().user
            } catch (_: Exception) {
                firebaseAuth.currentUser
            }
        }

        val uid = firebaseUser?.uid ?: AuthInputValidator.SEED_STAFF_ID
        val seedProfile = UserProfile(
            id = uid,
            nama = AuthInputValidator.SEED_STAFF_NAME,
            email = seedEmail,
            peran = UserRole.ADMIN.name
        )

        runCatching {
            firestore.collection(FirestoreCollections.USERS)
                .document(uid)
                .set(seedProfile.toFirestoreMap())
                .await()
        }

        return Result.success(seedProfile)
    }

    override suspend fun registerMahasiswa(
        nama: String,
        email: String,
        password: String
    ): Result<UserProfile> {
        return try {
            val authResult = firebaseAuth
                .createUserWithEmailAndPassword(email.trim(), password)
                .await()
            val user = authResult.user
                ?: return Result.failure(IllegalStateException("Gagal membuat akun pengguna baru."))

            // Paksa peran selalu MAHASISWA saat registrasi dari aplikasi
            val profile = UserProfile(
                id = user.uid,
                nama = nama.trim(),
                email = email.trim(),
                peran = UserRole.MAHASISWA.name
            )

            firestore.collection(FirestoreCollections.USERS)
                .document(user.uid)
                .set(profile.toFirestoreMap())
                .await()

            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUserProfile(): Result<UserProfile?> {
        val currentUser = firebaseAuth.currentUser ?: return Result.success(null)
        return fetchOrCreateProfile(
            uid = currentUser.uid,
            fallbackName = currentUser.displayName ?: "",
            fallbackEmail = currentUser.email ?: ""
        )
    }

    private suspend fun fetchOrCreateProfile(
        uid: String,
        fallbackName: String,
        fallbackEmail: String
    ): Result<UserProfile> {
        val isSeedAccount = fallbackEmail.equals(AuthInputValidator.SEED_STAFF_EMAIL, ignoreCase = true) ||
            uid == AuthInputValidator.SEED_STAFF_ID

        return try {
            val docRef = firestore.collection(FirestoreCollections.USERS).document(uid)
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val profile = UserProfile.fromFirestoreMap(uid, snapshot.data)
                if (isSeedAccount && profile.roleEnum == UserRole.MAHASISWA) {
                    val upgradedProfile = profile.copy(
                        nama = AuthInputValidator.SEED_STAFF_NAME,
                        peran = UserRole.ADMIN.name
                    )
                    runCatching { docRef.set(upgradedProfile.toFirestoreMap()).await() }
                    Result.success(upgradedProfile)
                } else {
                    Result.success(profile)
                }
            } else {
                val defaultProfile = UserProfile(
                    id = uid,
                    nama = if (isSeedAccount) {
                        AuthInputValidator.SEED_STAFF_NAME
                    } else {
                        fallbackName.ifBlank { fallbackEmail.substringBefore("@") }
                    },
                    email = fallbackEmail,
                    peran = if (isSeedAccount) UserRole.ADMIN.name else UserRole.MAHASISWA.name
                )
                runCatching { docRef.set(defaultProfile.toFirestoreMap()).await() }
                Result.success(defaultProfile)
            }
        } catch (e: Exception) {
            if (isSeedAccount) {
                Result.success(
                    UserProfile(
                        id = uid,
                        nama = AuthInputValidator.SEED_STAFF_NAME,
                        email = AuthInputValidator.SEED_STAFF_EMAIL,
                        peran = UserRole.ADMIN.name
                    )
                )
            } else {
                Result.failure(e)
            }
        }
    }

    override fun logout() {
        firebaseAuth.signOut()
    }
}
