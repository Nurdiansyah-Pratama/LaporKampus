# LaporKampus — Aplikasi Pelaporan Fasilitas dan Keluhan Kampus (MVP)

**LaporKampus** adalah aplikasi Android berbasis **Jetpack Compose (Material 3)**, arsitektur **MVVM (ViewModel + StateFlow + Repository)**, dan backend **Firebase (Authentication, Cloud Firestore, Firebase Storage, Cloud Messaging / FCM)** serta peta open-source **OpenStreetMap (OSMDroid)** untuk pelaporan kerusakan fasilitas maupun keluhan sensitif di lingkungan kampus.

---

## 1. Spesifikasi Teknis & Stack

| Komponen | Teknologi yang Digunakan |
|---|---|
| **Package / Application ID** | `lat.pam.laporkampus` |
| **Bahasa & Build System** | Kotlin (`2.2.10`), Gradle Kotlin DSL (`build.gradle.kts`), Version Catalog (`gradle/libs.versions.toml`) |
| **SDK Android** | `minSdk = 27`, `targetSdk = 37`, `compileSdk = 37` |
| **Antarmuka Pengguna (UI)** | Jetpack Compose + Material 3 (Single-Activity Architecture di `MainActivity`) |
| **Arsitektur** | MVVM (`ViewModel` + `StateFlow` + `Repository`), aturan domain terpusat (`StatusTransitionRules`) |
| **Navigasi** | Navigation Compose (`LaporKampusNavHost`) + Role-Based Route Guard (`RoleGuard`) |
| **Backend** | Firebase BoM (`33.5.1`): Firebase Auth, Cloud Firestore, Firebase Storage, Firebase Cloud Messaging (FCM) |
| **Peta & Lokasi** | OpenStreetMap (`org.osmdroid:osmdroid-android:6.1.20`) + Google Play Services Fused Location Provider + Fallback Input Lokasi Manual |
| **Kamera & Gambar** | Photo Picker / CameraX (`FileProvider`) + Coil Compose (`io.coil-kt:coil-compose:2.7.0`) |

---

## 2. Struktur Paket (`lat.pam.laporkampus`)

```text
app/src/main/java/lat/pam/laporkampus/
├── MainActivity.kt                          # Single Activity entrypoint
├── data/
│   ├── firebase/
│   │   ├── FirestoreCollections.kt          # Konstanta nama koleksi Firestore & path Storage
│   │   └── LaporKampusMessagingService.kt   # Penerima FCM & helper notifikasi sistem perubahan status
│   ├── model/
│   │   ├── AppEnums.kt                      # Enum UserRole, ReportStatus, ReportPriority
│   │   ├── UserProfile.kt                   # Model koleksi users/{id}
│   │   └── ReportModels.kt                  # Model koleksi reports/{id}, history, dan ratings
│   └── repository/
│       ├── AuthRepository.kt                # Autentikasi & pembuatan profil wajib MAHASISWA
│       └── ReportRepository.kt              # CRUD laporan, upload foto Storage, timeline history, Satgas, rating
├── navigation/
│   ├── ScreenRoute.kt                       # Definisi rute & RoleGuard (pembatasan akses rute per peran)
│   └── LaporKampusNavHost.kt                # NavHost utama + BottomNavigation dinamis per peran
├── ui/
│   ├── auth/
│   │   ├── LoginScreen.kt                   # Layar masuk
│   │   └── RegisterScreen.kt                # Layar daftar akun Mahasiswa baru
│   ├── report/
│   │   ├── CreateReportScreen.kt            # Form buat laporan + kamera/galeri + opsi anonim/sensitif
│   │   └── OsmLocationPicker.kt             # Pemilih titik koordinat OpenStreetMap + GPS + fallback manual
│   ├── tracking/
│   │   ├── MyReportsScreen.kt               # Daftar "Laporan Saya" (real-time Flow)
│   │   └── ReportDetailScreen.kt            # Detail laporan, timeline history real-time, & form rating 1-5
│   ├── admin/
│   │   ├── AdminDashboardScreen.kt          # Ringkasan jumlah per status, verifikasi, prioritas, & tugaskan teknisi
│   │   └── SatgasReportsScreen.kt           # Layar khusus peran SATGAS untuk menangani laporan sensitif
│   ├── technician/
│   │   └── TechnicianTasksScreen.kt         # Daftar tugas teknisi & penyelesaian wajib foto bukti perbaikan
│   ├── components/
│   │   └── RoleShellComponents.kt           # Layar Profil Pengguna & layar Akses Ditolak (RoleGuard)
│   └── theme/
│       ├── Color.kt, Type.kt, Theme.kt      # Tema Material 3 LaporKampus
├── util/
│   ├── StatusTransitionRules.kt             # Aturan terpusat transisi status laporan & agregasi jumlah per status
│   ├── AuthInputValidator.kt                # Validator input login & registrasi
│   ├── ReportFormValidator.kt               # Validator form laporan & validasi rating 1-5
│   └── DateAndPrivacyUtils.kt               # Format waktu Indonesia & penyembunyian reporterId untuk laporan anonim
└── viewmodel/
    ├── AuthViewModel.kt                     # State manajemen sesi & autentikasi
    ├── CreateReportViewModel.kt             # State pembuatan laporan baru
    ├── TrackingViewModel.kt                 # State pelacakan laporan real-time, notifikasi status, & rating
    ├── AdminViewModel.kt                    # State pengelolaan laporan oleh Admin
    ├── TechnicianViewModel.kt               # State penyelesaian tugas oleh Teknisi
    └── SatgasViewModel.kt                   # State penanganan laporan sensitif oleh Satgas
```

File aturan keamanan di root proyek:
- `firestore.rules` — Aturan keamanan Cloud Firestore.
- `storage.rules` — Aturan keamanan Firebase Storage.
- `firebase.json` & `rules-tests/` — Konfigurasi Firebase Emulator Suite & unit test otomatis untuk `firestore.rules`.

---

## 3. Cara Setup Proyek

1. **Prasyarat**:
   - Android Studio Ladybug / Meerkat atau lebih baru.
   - JDK 11+ (disarankan JDK 17/21 bawaan Android Studio).
   - Proyek Firebase dengan layanan **Authentication (Email/Password)**, **Cloud Firestore**, **Firebase Storage**, dan **Cloud Messaging** yang telah diaktifkan.
2. **Konfigurasi Firebase (`google-services.json`)**:
   - Unduh file `google-services.json` untuk package `lat.pam.laporkampus` dari Firebase Console.
   - Letakkan di `app/google-services.json`.
   - File `app/google-services.json` dan `local.properties` sudah masuk ke dalam `.gitignore` sehingga tidak akan pernah ter-commit ke repositori Git.
3. **Konfigurasi Peta OpenStreetMap (OSMDroid)**:
   - Aplikasi menggunakan pustaka open-source `osmdroid-android` yang **tidak memerlukan API Key berbayar**.
   - Jika perangkat tidak memberikan izin lokasi atau tidak memiliki sinyal GPS/internet peta, form laporan menyediakan **Input Nama Lokasi Manual** sebagai fallback wajib.
4. **Penerapan Aturan Keamanan (`firestore.rules` & `storage.rules`)**:
   - Salin isi `firestore.rules` ke **Firebase Console -> Firestore Database -> Rules -> Publish** (atau jalankan `firebase deploy --only firestore:rules`).
   - Salin isi `storage.rules` ke **Firebase Console -> Storage -> Rules -> Publish** (atau jalankan `firebase deploy --only storage`).
5. **Pengaturan Peran Staf (`ADMIN`, `TEKNISI`, `SATGAS`)**:
   - Demi keamanan, registrasi dari aplikasi **hanya** membuat akun dengan `peran: "MAHASISWA"`.
   - Untuk menguji peran `ADMIN`, `TEKNISI`, atau `SATGAS`, daftarkan akun terlebih dahulu dari aplikasi, lalu buka **Firebase Console -> Firestore Database -> koleksi `users/{uid}`**, dan ubah nilai field `peran` menjadi `"ADMIN"`, `"TEKNISI"`, atau `"SATGAS"`.

---

## 4. Cara Menjalankan Build & Pengujian

### A. Build APK Debug & Menjalankan Unit Test Kotlin (ViewModel & Domain)
Jalankan perintah berikut dari terminal di root proyek:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest --no-configuration-cache
```

Cakupan unit test di `app/src/test/java/lat/pam/laporkampus/`:
- `StatusTransitionRulesTest.kt`: Menguji seluruh transisi status yang diizinkan (`DITERIMA -> DIVERIFIKASI -> DIKERJAKAN -> SELESAI`, plus `DITOLAK`), pemblokiran transisi tidak sah, pemeriksaan status terminal, dan agregasi jumlah per status (`countByStatus`).
- `ViewModelUnitTests.kt`: Menguji `AuthViewModel`, `CreateReportViewModel`, `AdminViewModel`, `TechnicianViewModel`, `SatgasViewModel`, dan `TrackingViewModel` menggunakan `FakeAuthRepository` dan `FakeReportRepository`.
- `ExampleUnitTest.kt`: Menguji validator input autentikasi, validator form laporan & fallback lokasi manual, `RoleGuard`, perlindungan privasi `isAnonymous`, dan validator rating 1–5.

### B. Menguji Firestore Security Rules dengan Firebase Emulator
Jika Anda memiliki Node.js dan `firebase-tools` terinstal:

```powershell
cd rules-tests
npm install
npx firebase emulators:exec --only firestore "npm test"
```

---

## 5. Checklist Skenario Uji Manual (End-to-End)

Gunakan daftar periksa berikut untuk memverifikasi seluruh alur utama aplikasi pada perangkat/emulator Android:

### [ ] Skenario 1 — Registrasi & Pembuatan Laporan (Mahasiswa)
1. Buka aplikasi, pilih **Daftar di sini**, masukkan Nama, Email, dan Kata Sandi (minimal 6 karakter), lalu tekan **Daftar Sekarang**.
2. Pastikan Anda masuk ke beranda **MAHASISWA** dan dokumen `users/{uid}` di Firestore memiliki `peran = "MAHASISWA"`.
3. Buka tab **Buat Laporan**:
   - Pilih kategori (misal: *Kelistrikan & Pencahayaan*).
   - Isi deskripsi minimal 10 karakter (misal: *"Lampu koridor gedung B lantai 2 padam total"*).
   - Tambahkan 1–3 foto dari Galeri atau Kamera.
   - Pilih titik lokasi pada peta OpenStreetMap (atau tolak izin lokasi untuk menguji **fallback input nama lokasi manual**, misal: *"Gedung B Lt. 2"*).
   - *(Opsional)* Centang **Lapor sebagai Anonim** untuk menguji penyembunyian identitas pelapor dari Admin/Teknisi.
4. Tekan **Kirim Laporan**.
5. Pastikan laporan muncul di tab **Laporan Saya** dengan status **`DITERIMA`**, dan saat dibuka menampilkan **Linimasa Riwayat Status** awal.

### [ ] Skenario 2 — Verifikasi, Prioritas, & Penugasan Teknisi (Admin)
1. Masuk menggunakan akun yang di dokumen `users/{uid}`-nya telah diatur `peran = "ADMIN"`.
2. Di layar **Dasbor Admin**, periksa kartu ringkasan jumlah laporan per status (`Diterima`, `Diverifikasi`, `Dikerjakan`, `Selesai`, `Ditolak`).
3. Pastikan laporan yang memiliki `isSensitive = true` **tidak muncul** di daftar Admin. Jika laporan bertanda `isAnonymous = true`, pastikan UID/nama pelapor disembunyikan menjadi *"Pelapor Anonim (Identitas Disembunyikan)"*.
4. Tekan **Kelola / Ubah Status** pada laporan berstatus `DITERIMA`:
   - Ubah status ke **`DIVERIFIKASI`** (perhatikan bahwa dropdown hanya menampilkan status transisi yang sah).
   - Pilih prioritas (misal: **`TINGGI`**).
   - Pilih atau ketik nama Teknisi pada kolom **Tugaskan ke Teknisi**.
   - Isi catatan perubahan status, lalu tekan **Simpan Perubahan**.
5. Tekan kembali **Kelola / Ubah Status** untuk mengubah status dari `DIVERIFIKASI` menjadi **`DIKERJAKAN`** ketika teknisi mulai mengerjakan, atau biarkan ditindaklanjuti sesuai alur.

### [ ] Skenario 3 — Penyelesaian Tugas dengan Foto Bukti (Teknisi)
1. Masuk menggunakan akun dengan `peran = "TEKNISI"` (yang namanya/ID-nya ditugaskan pada langkah sebelumnya).
2. Di layar **Tugas Teknisi**, pastikan laporan yang ditugaskan muncul.
3. Tekan **Tandai Selesai + Foto Bukti** (pastikan laporan sudah berstatus `DIKERJAKAN`):
   - Coba tekan simpan tanpa foto — pastikan validasi menolak dan meminta minimal 1 foto bukti.
   - Pilih/ambil minimal 1 foto bukti hasil perbaikan dan isi catatan penyelesaian pekerjaan.
   - Tekan **Kirim & Tandai Selesai**.
4. Pastikan status laporan berubah menjadi **`SELESAI`** dan foto bukti ditambahkan ke laporan beserta catatan baru di subkoleksi `history`.

### [ ] Skenario 4 — Notifikasi Status & Pemberian Rating 1–5 (Mahasiswa)
1. Masuk kembali menggunakan akun **MAHASISWA** pembuat laporan.
2. Pastikan notifikasi perubahan status muncul ketika status laporan diperbarui, dan kartu laporan di **Laporan Saya** kini berstatus **`SELESAI`**.
3. Ketuk laporan tersebut untuk membuka **Detail Laporan**:
   - Periksa seluruh **Linimasa Riwayat Status** (`DITERIMA` -> `DIVERIFIKASI` -> `DIKERJAKAN` -> `SELESAI`) beserta foto bukti perbaikan.
   - Di bagian **Penilaian Penanganan Laporan**, pilih bintang **1 sampai 5** dan tulis komentar ulasan, lalu tekan **Kirim Penilaian**.
   - Pastikan penilaian tersimpan ke koleksi `ratings/{reportId}` dan ditampilkan sebagai kartu ulasan tersimpan.

### [ ] Skenario 5 — Jalur Laporan Sensitif & Security Rules (Satgas)
1. Masuk sebagai **MAHASISWA**, buat laporan baru dengan mencentang **Laporan Sensitif (Khusus Satgas)** atau memilih kategori *Kekerasan / Pelecehan / Perundungan (Sensitif)*.
2. Pastikan laporan tersebut **tidak terlihat** saat login sebagai `ADMIN` maupun `TEKNISI`.
3. Masuk menggunakan akun dengan `peran = "SATGAS"`.
4. Di layar **Laporan Sensitif (Satgas)**, pastikan laporan sensitif tersebut muncul dan dapat ditindaklanjuti (`DITERIMA` -> `DIVERIFIKASI` -> `DIKERJAKAN` -> `SELESAI`) secara rahasia oleh Satgas.
