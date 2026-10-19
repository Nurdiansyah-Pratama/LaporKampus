# LaporKampus

**LaporKampus** adalah aplikasi Android berbasis Jetpack Compose (Material 3) untuk pelaporan fasilitas rusak dan keluhan lingkungan kampus secara transparan, terstruktur, dan aman (termasuk dukungan pelaporan anonim serta laporan sensitif khusus Satgas).

---

## Teknologi & Arsitektur (Stack)

- **Bahasa & Build**: Kotlin, Gradle Kotlin DSL (`build.gradle.kts`), Version Catalog (`libs.versions.toml`), `minSdk 27`
- **Antarmuka Pengguna (UI)**: Jetpack Compose + Material 3 (Single-Activity Architecture)
- **Arsitektur**: MVVM (`ViewModel` + `StateFlow` + `Repository`)
- **Navigasi**: Navigation Compose dengan kontrol akses berbasis peran
- **Backend**:
  - Firebase Authentication (Email & Kata Sandi)
  - Cloud Firestore (Data pengguna, laporan, riwayat status, rating)
  - Firebase Storage (Unggah foto bukti laporan & penyelesaian)
  - Firebase Cloud Messaging / FCM (Notifikasi perubahan status)
- **Peta & Lokasi**: Google Maps Compose + Fused Location Provider (beserta input lokasi manual sebagai fallback)
- **Kamera & Gambar**: Photo Picker / CameraX + Coil Compose

---

## Struktur Paket (`lat.pam.laporkampus`)

```text
lat.pam.laporkampus/
├── data/
│   ├── model/          # Model data & Enum (UserRole, ReportStatus, ReportPriority, dll.)
│   ├── repository/     # Abstraksi & implementasi repositori
│   └── firebase/       # Sumber data Firebase & konstanta koleksi
├── ui/
│   ├── auth/           # Layar Login & Registrasi
│   ├── report/         # Form pembuatan laporan, pemilih foto & lokasi peta
│   ├── tracking/       # Daftar "Laporan Saya", detail laporan, timeline status & rating
│   ├── admin/          # Dashboard Admin, verifikasi, penugasan teknisi, & statistik
│   ├── technician/     # Daftar tugas teknisi & unggah bukti selesai
│   ├── components/     # Komponen UI Compose yang dapat digunakan kembali
│   └── theme/          # Tema Material 3 (Color, Type, Theme)
├── navigation/         # NavHost, rute layar, dan penjaga rute per peran
├── viewmodel/          # ViewModel untuk setiap fitur
└── util/               # Utilitas & aturan bisnis terpusat (StatusTransitionRules)
```

---

## Prasyarat & Konfigurasi Rahasia (Wajib Sebelum Sprint 1)

Demi keamanan, file kredensial **`google-services.json`** dan **`local.properties`** tidak pernah disimpan ke dalam repositori Git.

### 1. Menghubungkan Firebase (`google-services.json`)
1. Buka [Firebase Console](https://console.firebase.google.com/) dan buat proyek baru (misalnya `lapor-kampus`).
2. Tambahkan aplikasi Android dengan **Package Name**: `lat.pam.laporkampus`.
3. Unduh file `google-services.json` dan letakkan di dalam folder modul **`app/`** (`app/google-services.json`).
4. Aktifkan layanan berikut di Firebase Console:
   - **Authentication** -> Sign-in method -> Aktifkan **Email/Password**.
   - **Cloud Firestore** -> Buat database.
   - **Storage** -> Aktifkan Firebase Storage.

### 2. Mengatur Google Maps API Key (`local.properties`)
1. Buat atau salin **Maps SDK for Android API Key** dari [Google Cloud Console](https://console.cloud.google.com/).
2. Buka file `local.properties` di root proyek.
3. Tambahkan baris berikut di bagian bawah file:
   ```properties
   MAPS_API_KEY=ISI_DENGAN_API_KEY_GOOGLE_MAPS_ANDA
   ```
4. Script `app/build.gradle.kts` secara otomatis meneruskan nilai `MAPS_API_KEY` ke `AndroidManifest.xml` melalui `manifestPlaceholders`.

---

## Cara Menjalankan Proyek

1. Pastikan Android Studio (Ladybug / versi terbaru) dan JDK 17+ / 21 telah terpasang.
2. Jalankan perintah berikut di terminal root proyek untuk menguji kompilasi dan unit test:
   ```bash
   ./gradlew assembleDebug testDebugUnitTest
   ```
