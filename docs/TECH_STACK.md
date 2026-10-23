# Spesifikasi Lengkap Tech Stack — Onlasdan Android

Dokumen ini memuat rincian lengkap arsitektur teknologi, library, framework, compiler, dan tooling yang digunakan dalam pengembangan **Onlasdan Android**.

---

## 1. Lingkungan & Toolchain Inti

| Komponen | Versi | Catatan |
|---|---|---|
| **Sistem Operasi Target** | Android 15+ (API 35–36) | `minSdk = 35`, `targetSdk = 36` |
| **Bahasa Pemrograman** | Kotlin 2.2.0 | Fitur modern coroutine, context parameters, contracts |
| **Java Platform** | OpenJDK 21 (LTS) | `jvmToolchain(21)` |
| **Android Gradle Plugin** | AGP 9.1.0 | Generasi terbaru build system Android |
| **Kotlin Symbol Processing** | KSP 2.2.0-2.0.2 | Code generation untuk Room & Hilt |
| **Signature Scheme** | **APK Signature Scheme v3 ONLY** | V1, V2, dan V4 dimatikan demi integritas modern |

---

## 2. Antarmuka Pengguna & Rendering (UI & Graphics)

- **Jetpack Compose BOM**: `2026.03.01`
- **Compose Compiler Plugin**: `2.2.0` (terintegrasi langsung dalam Kotlin compiler)
- **Desain Sistem Kustom**: *"Noir & Blush"*
  - AMOLED Pure Black (`0xFF000000`)
  - Permukaan bertingkat: DarkSurface (`0xFF0D0D0D`), DarkElevated (`0xFF151515`), DarkCard (`0xFF1F1F1F`)
  - Aksen Blush Pink (`0xFFFF2E93`) yang terkontrol ketat (~10% rasio visual)
  - Hairline Border (`0xFF2A2A2A`) untuk pemisahan *Figure-Ground* kontras
- **Sistem Animasi**: **100% Pure Spring Physics (`NoirMotion`)**
  - Bebas durasi milidetik (`zero ms tween()`)
  - Dukungan bypass *Developer Options 0x Animation Scale* via `NoirForceMotionScale`
  - Feedback sentuhan taktil (scale down 0.95f–0.97f dengan spring response)
- **Komponen Navigasi**:
  - `androidx.navigation:navigation-compose:2.9.7`
  - Floating pill bottom navigation dock dengan animasi elastis

---

## 3. Arsitektur Data & Injeksi Dependensi

- **Dependency Injection**:
  - `com.google.dagger:hilt-android:2.59.2`
  - `androidx.hilt:hilt-navigation-compose:1.3.0`
- **Database Lokal**:
  - `androidx.room:room-runtime:2.8.0` + `androidx.room:ktx:2.8.0`
  - Room KSP Compiler untuk pembuatan DAO asinkronus berbasis Coroutine
- **Penyimpanan Preferensi**:
  - `androidx.datastore:datastore-preferences:1.1.1`
  - `SharedPreferences` terenkapsulasi untuk konfigurasi pembaca instan
- **Manajemen Storage & Dokumen**:
  - `androidx.documentfile:documentfile:1.1.0` (Scoped Storage untuk download bab komik)

---

## 4. Jaringan, Scraping & Pengolahan Gambar

- **HTTP Client & Serialization**:
  - `com.squareup.okhttp3:okhttp:4.12.0` dengan HTTP/2, pooling koneksi, dan timeout adaptif
  - `com.google.code.gson:gson:2.11.0` untuk deserialisasi JSON model dan disk cache
- **HTML Parser & Web Scraper**:
  - `org.jsoup:jsoup:1.17.2` untuk scraping DOM HTML yang cepat dan andal
- **Image Loading & Cache**:
  - `io.coil-kt:coil-compose:2.6.0`
  - Dual-layer cache: in-memory bitmap cache + disk LRU cache
  - Dukungan decoding WebP, JPG, PNG, dan GIF
  - Header kustom (Referer / User-Agent spoofing) per comic source

---

## 5. Kualitas Kode, Linter & Format Otomatis

- **Spotless Plugin**: `com.diffplug.spotless:8.10.4`
- **ktlint**: `1.8.0` dengan custom ruleset Compose (`io.nlopez.compose.rules:ktlint:0.6.8`)
- **Android Lint**:
  - Mode tegas: `abortOnError = true`, `warningsAsErrors = true`, `checkDependencies = true`
  - Deteksi ketat resource tidak terpakai (`UnusedResources`, `UnusedIds`)

---

## 6. Testing & Verifikasi

- **Unit Testing**:
  - `junit:junit:4.13.2`
  - `org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0`
  - `org.mockito.kotlin:mockito-kotlin:5.4.0` & `org.mockito:mockito-core:5.12.0`
- **Instrumentation & UI Testing**:
  - `androidx.test.ext:junit:1.3.0`
  - `androidx.test.espresso:espresso-core:3.7.0`
  - `androidx.compose.ui:ui-test-junit4`

---

## 7. Optimasi Rilis & Build Engine

- **Minifikasi & Bytecode Optimization**: R8 Full Mode dengan keep-rules ketat
- **Resource Shrinking**: Menghapus asset dan resource XML yang tidak terpakai
- **Gradle Daemons & Caches**: Parallel build execution, configuration caching, incremental KSP
