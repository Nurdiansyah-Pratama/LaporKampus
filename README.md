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
- **Peta & Lokasi**: OpenStreetMap (`osmdroid-android`, open-source tanpa API key) + Fused Location Provider (beserta input lokasi manual sebagai fallback)
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
│   ├── report/         # Form pembuatan laporan, pemilih foto & lokasi peta (OSM)
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

## Prasyarat & Konfigurasi

Demi keamanan, file kredensial **`google-services.json`** dan **`local.properties`** telah dimasukkan ke dalam `.gitignore` agar tidak pernah bocor ke repositori Git.

### 1. Konfigurasi Firebase (`app/google-services.json`)
- File `app/google-services.json` untuk package `lat.pam.laporkampus` sudah terpasang secara lokal.
- Pastikan di [Firebase Console](https://console.firebase.google.com/) layanan berikut telah diaktifkan:
  - **Authentication** -> Sign-in method -> **Email/Password**
  - **Cloud Firestore**
  - **Firebase Storage**

### 2. Peta Open-Source OpenStreetMap (OSMDroid)
- Proyek ini menggunakan **OpenStreetMap (`osmdroid-android`)** serta Nominatim reverse-geocoding yang bersifat open-source dan gratis, sehingga **tidak memerlukan Google Maps API Key**.

---

## Cara Menjalankan Proyek

1. Pastikan Android Studio (Ladybug / versi terbaru) dan JDK 17+ / 21 telah terpasang.
2. Jalankan perintah berikut di terminal root proyek untuk menguji kompilasi dan unit test:
   ```bash
   ./gradlew assembleDebug testDebugUnitTest
   ```
