# Onlasdan Android

<p align="center">
  <img src="icon.png" width="128" height="128" alt="Onlasdan Logo">
</p>

<p align="center">
  <strong>Aplikasi Pembaca Komik Modern, Cepat, dan Elegan Berbasis Jetpack Compose</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android_15%2B_(API_35%2B)-blue?style=flat-square&logo=android" alt="Android Version">
  <img src="https://img.shields.io/badge/Kotlin-2.2.0-purple?style=flat-square&logo=kotlin" alt="Kotlin Version">
  <img src="https://img.shields.io/badge/Jetpack_Compose-2026.03.01-green?style=flat-square&logo=jetpackcompose" alt="Compose Version">
  <img src="https://img.shields.io/badge/Architecture-Clean_%2B_Blueprints-orange?style=flat-square" alt="Architecture">
  <img src="https://img.shields.io/badge/Theme-Noir_%26_Blush-pink?style=flat-square" alt="Theme">
  <img src="https://img.shields.io/badge/Motion-100%25_Spring_Physics-black?style=flat-square" alt="Motion">
  <img src="https://img.shields.io/badge/Signing-V3_Scheme_Only-red?style=flat-square" alt="Signing">
</p>

---

## 📖 Apa Itu Onlasdan?

**Onlasdan** adalah aplikasi pembaca komik (Manga, Manhwa, Manhua, Doujinshi) untuk sistem operasi Android yang mengutamakan kecepatan, estetika visual gelap AMOLED, dan pengalaman pengguna (*UX*) tanpa gangguan iklan.

Dibangun dari dasar menggunakan **100% Jetpack Compose deklaratif** dan **Kotlin 2.2.0**, Onlasdan mengintegrasikan arsitektur modular *Slot-Based Screen Blueprints* yang memungkinkan integrasi multi-sumber (*multi-source comic aggregator*) secara seragam, bersih, dan independen.

---

## ✨ Fitur Utama

- 📚 **Multi-Source Aggregator**: Baca ribuan judul komik dari berbagai sumber terpopuler:
  - **Shinigami**: REST API murni, update super cepat, feed format khusus (Manga, Manhwa, Manhua).
  - **Komikindo**: Pustaka komik berbahasa Indonesia terlengkap dengan filter genre luas.
  - **Komiku**: Platform komik populer dengan fitur peringkat harian dan update berkala.
  - **DoujinDesu**: Pustaka manga dan doujinshi dengan enkripsi payload AES-256-CBC.
- 🖤 **Desain Sistem "Noir & Blush"**:
  - Tampilan AMOLED Pure Black pekat (`#000000`) hemat daya baterai.
  - Aksen Blush Pink (`#FF2E93`) hemat (~10% rasio visual) yang elegan dan tidak melelahkan mata.
- 🧲 **Universal Hold-Tap Morphing Preview**:
  - Tekan dan tahan (long press) pada kartu komik mana pun untuk memunculkan preview kartu melayang (*morphing card*) lengkap dengan sinopsis, cover HD, dan haptic feedback taktil.
- 📐 **Penerapan Prinsip Gestalt & Hukum UX**:
  - *Law of Uniform Connectedness*: Filter chapter dengan segmented pill yang terhubung visual.
  - *Law of Common Region & Proximity*: Restrukturisasi modal detail komik yang terpadu dan kompak.
  - *Law of Figure-Ground*: Framing hairline border halus pada seluruh poster kartu.
  - *Fitts's Law*: Target sentuh interaktif terstandarisasi ≥ 44–48dp.
- 🌀 **Sistem Animasi Fisika Pegas (100% Pure Spring)**:
  - Nol durasi tetap (`zero ms tween()`). Semua transisi dikendalikan fisika pegas `NoirMotion`.
  - Tetap berjalan halus meskipun *Animation Scale* di *Developer Options* di-set ke `0x`.
- 📖 **Webtoon Infinite Canvas & Seamless Reader**:
  - Scroll vertikal tanpa batas dengan *continuous next-chapter prefetcher*.
  - Auto-scroll otomatis dengan pengatur kecepatan instan.
  - Mode layar penuh imersif (*edge-to-edge*) dengan proteksi notch / camera cutout.
  - Filter malam (*Night Dimmer*) in-app untuk kenyamanan membaca di kegelapan.
- 📥 **Manajer Unduhan Offline**:
  - Simpan chapter favorit ke penyimpanan lokal via Scoped Storage (`DocumentFile`).
  - Baca kapan saja tanpa koneksi internet.
- 🔖 **Riwayat Baca & Bookmark Lokal**:
  - Progres baca dan posisi scroll tersimpan otomatis di Room Database lokal tanpa perlu akun/login.

---

## 🛠️ Tech Stack & Arsitektur

| Kategori | Teknologi | Deskripsi |
|---|---|---|
| **Core OS & SDK** | Android 15+ (API 35–36) | Min SDK 35, Target SDK 36, Java 21 Toolchain |
| **Language & Framework** | Kotlin 2.2.0 & Jetpack Compose | Compose BOM 2026.03.01, Compose Compiler 2.2.0 |
| **Arsitektur UI** | Slot-Based Screen Blueprints | Pola modular deklaratif tanpa `if/when(source)` di UI |
| **Dependency Injection** | Dagger Hilt 2.59.2 | Pengelolaan siklus hidup dependensi terpusat |
| **Penyimpanan Lokal** | Room 2.8.0 & DataStore | Database SQLite lokal untuk riwayat, bookmark, dan download |
| **Jaringan & Scraping** | OkHttp 4.12, Gson 2.11, Jsoup 1.17 | HTTP/2, pooling koneksi, serialisasi JSON, dan parsing HTML DOM |
| **Image Pipeline** | Coil Compose 2.6.0 | In-memory + Disk LRU cache dengan custom spoof headers |
| **Format & Analisis** | Spotless 8.10.4 & ktlint 1.8.0 | Standar kode ketat dengan custom Compose linter rules |
| **Keamanan Build** | R8 Full Mode & V3 Signing Only | Minifikasi bytecode dan verifikasi tanda tangan modern |

---

## 🚀 Panduan Memulai & Alur Kerja Pengembang

Proyek ini dibangun menggunakan Android Gradle Plugin standar:

### Prasyarat Pengembangan
- **Android SDK**: API level 35 (Android 15) dengan Build-tools 35.0.0
- **Java Platform**: OpenJDK 21 (LTS)

### Perintah Gradle Utama

| Perintah | Deskripsi |
|---|---|
| `./gradlew assembleDebug` | Membangun APK varian pengujian langsung (Debug) |
| `./gradlew assembleRelease` | Membangun APK varian rilis dengan optimasi R8 dan penandatanganan V3 |
| `./gradlew testDebugUnitTest` | Menjalankan seluruh rangkaian pengujian unit (unit tests) |
| `./gradlew spotlessCheck` | Memeriksa kepatuhan pemformatan gaya kode (ktlint) |
| `./gradlew spotlessApply` | Memformat seluruh kode sumber secara otomatis sesuai aturan proyek |

---

## 📚 Dokumentasi Lengkap (`docs/`)

Untuk dokumentasi teknis yang lebih terperinci, silakan merujuk ke folder [`docs/`](docs/):

- 🏛️ [**Arsitektur Sistem**](docs/ARCHITECTURE.md) — Aliran data UDF, Clean Architecture, dan Slot-Based Screen Blueprints.
- ⚙️ [**Spesifikasi Tech Stack**](docs/TECH_STACK.md) — Daftar lengkap dependensi, compiler, dan plugin Gradle.
- 🌟 [**Katalog Fitur Lengkap**](docs/FEATURES.md) — Penjelasan detail seluruh fitur aplikasi.
- 🎨 [**Design System "Noir & Blush"**](docs/DESIGN_SYSTEM.md) — Filosofi warna, Gestalt & UX Laws, serta fisika gerak `NoirMotion`.
- 🔌 [**Panduan ComicSource**](docs/SOURCES.md) — Struktur adapter sumber dan langkah menambah comic source baru.

---

## 📄 Lisensi

Hak Cipta © 2026 Onlasdan. Dikembangkan untuk pengalaman membaca komik terbaik.
