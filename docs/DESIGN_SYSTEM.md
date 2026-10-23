# Design System & Motion Philosophy — "Noir & Blush"

Dokumen ini menjelaskan fondasi estetika, tokens desain, implementasi prinsip Gestalt & hukum UX, serta sistem animasi berbasis fisika pegas murni pada **Onlasdan Android**.

---

## 1. Filosofi Estetika: Noir & Blush

Tema visual Onlasdan dirancang eksklusif untuk layar OLED/AMOLED:
- **AMOLED Pure Black (`#000000`)**: Menghemat daya baterai perangkat dan memberikan kontras mutlak.
- **Dark Surface Hierarchy**:
  - `DarkSurface` (`#0D0D0D`): Kanvas sekunder
  - `DarkElevated` (`#151515`): Kontainer kartu dan baris
  - `DarkCard` (`#1F1F1F`): Elemen melayang dan bottom sheet
  - `DarkOutline` (`#2A2A2A`): Garis batas hairline halus 1dp
- **The 10% Blush Accent Rule**: Warna pink cerah (`#FF2E93`) hanya digunakan sebagai aksen penanda (indikator aktif, status unread, floating action button). Maksimal ~10% dari total luas visual layar untuk menjaga kesan mewah dan fokus.

---

## 2. Implementasi Prinsip Gestalt & Hukum UX

| Prinsip | Implementasi di Onlasdan |
|---|---|
| **Law of Uniform Connectedness & Similarity** | `SegmentedFilterPill`: Tombol filter chapter menyatu dalam satu kontainer kapsul dengan background penanda yang berpindah mulus. |
| **Law of Common Region & Proximity** | `ComicDetailSheet`: Cover poster, judul, author, status badge, rating bintang, dan tombol aksi utama dikelompokkan rapat dalam satu area berbingkai jelas di bagian atas lembar detail. |
| **Law of Figure-Ground & Framing** | Penambahan hairline border `DarkOutline` (1dp) pada seluruh poster kartu komik dan carousel agar gambar poster terpisah tegas dari kanvas hitam pekat. |
| **Law of Focal Point** | Chapter yang belum dibaca menggunakan warna teks primer (`TextPrimary`) kontras tinggi sebagai titik fokus mata, sedangkan chapter yang sudah dibaca diredupkan ke `TextSecondary`. |
| **Fitts's Law** | Semua tombol aksi interaktif (unduh chapter, clear search bar, tombol aksi cepat) memiliki area target sentuh minimal **44–48dp** agar mudah ditekan jari tanpa salah sentuh. |

---

## 3. Sistem Animasi: 100% Pure Physics, Zero Milliseconds

Onlasdan menerapkan aturan gerak tanpa kompromi:
1. **Nol Durasi Tetap (No fixed `tween()` / zero ms)**:
   - Gerakan tidak dipaksa oleh angka waktu milidetik buatan, melainkan mengikuti **fisika pegas (spring physics)** alami.
2. **Spesifikasi Spring Tokens (`NoirMotion`)**:
   - **Spatial Spring (Posisi, Ukuran, Scale)**:
     - `dampingRatio = 0.80f`, `stiffness = 800f`
     - Menghasilkan gerakan responsif dengan satu rem magnetik halus di akhir tanpa guncangan berlebih.
   - **Effects Spring (Alpha / Opacity & Warna)**:
     - `dampingRatio = Spring.DampingRatioNoBouncy (1.0f)`, `stiffness = 1100f`
     - Menjamin pergantian transparansi dan warna halus tanpa flickering.
   - **Bouncy Launch (Pill Bottom Dock)**:
     - `dampingRatio = 0.65f`, `stiffness = 500f` (efek lempar pegas elastis saat dock muncul).
3. **Tactile Scale Feedback**:
   - Setiap elemen yang dapat diklik (`noirClickable`) mengecil ke scale `0.96f` saat ditekan, lalu membal kembali ke `1.0f`.
4. **Bypass Developer Options 0x**:
   - Pengguna dengan *Animation Scale 0x* di Android Developer Options tetap dapat menikmati animasi halus melalui injeksi `NoirForceMotionScale`.
