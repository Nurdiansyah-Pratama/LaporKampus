package lat.pam.laporkampus.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import lat.pam.laporkampus.data.firebase.FirestoreCollections
import lat.pam.laporkampus.data.model.UserProfile
import lat.pam.laporkampus.data.model.UserRole

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
        return try {
            val authResult = firebaseAuth
                .signInWithEmailAndPassword(email.trim(), password)
                .await()
            val user = authResult.user
                ?: return Result.failure(IllegalStateException("Pengguna tidak ditemukan setelah masuk."))
            fetchOrCreateProfile(user.uid, user.displayName ?: "", user.email ?: email.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
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
        return try {
            val docRef = firestore.collection(FirestoreCollections.USERS).document(uid)
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val profile = UserProfile.fromFirestoreMap(uid, snapshot.data)
                Result.success(profile)
            } else {
                // Jika akun dibuat langsung di Firebase Console tanpa dokumen di `users`,
                // buat dokumen default dengan peran MAHASISWA.
                val defaultProfile = UserProfile(
                    id = uid,
                    nama = fallbackName.ifBlank { fallbackEmail.substringBefore("@") },
                    email = fallbackEmail,
                    peran = UserRole.MAHASISWA.name
                )
                docRef.set(defaultProfile.toFirestoreMap()).await()
                Result.success(defaultProfile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun logout() {
        firebaseAuth.signOut()
    }
}
