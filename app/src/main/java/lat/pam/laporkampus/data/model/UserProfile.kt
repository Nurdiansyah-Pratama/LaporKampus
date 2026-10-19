package lat.pam.laporkampus.data.model

/**
 * Representasi dokumen pengguna pada koleksi `users/{uid}` di Cloud Firestore.
 * Field disimpan dengan nama `id`, `nama`, `email`, dan `peran`.
 */
data class UserProfile(
    val id: String = "",
    val nama: String = "",
    val email: String = "",
    val peran: String = UserRole.MAHASISWA.name
) {
    val roleEnum: UserRole
        get() = UserRole.fromString(peran)

    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "id" to id,
        "nama" to nama,
        "email" to email,
        "peran" to peran
    )

    companion object {
        fun fromFirestoreMap(id: String, data: Map<String, Any?>?): UserProfile {
            if (data == null) return UserProfile(id = id)
            return UserProfile(
                id = (data["id"] as? String)?.ifBlank { id } ?: id,
                nama = data["nama"] as? String ?: "",
                email = data["email"] as? String ?: "",
                peran = data["peran"] as? String ?: UserRole.MAHASISWA.name
            )
        }
    }
}
