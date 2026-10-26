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
   - **Spatial Tokens (Posisi, Ukuran, Scale; Float/Dp/IntOffset)**:
     - `Spatial`: `dampingRatio = 0.80f`, `stiffness = 800f` (rem magnetik halus responsif).
     - `SpatialFast`: `dampingRatio = 0.72f`, `stiffness = 900f` (elemen kecil, icon scale, tactile recovery).
     - `SpatialMedium`: `dampingRatio = 0.75f`, `stiffness = 550f` (pill indicator, entrance kartu).
     - `SpatialHeavy`: `dampingRatio = 0.82f`, `stiffness = 380f` (transisi layar, sheet morphing, IME dock).
   - **Effects Tokens (Alpha / Opacity & Warna; NoBouncy)**:
     - `Effects`: `dampingRatio = Spring.DampingRatioNoBouncy (1.0f)`, `stiffness = 1000f` (Float & Color tanpa flickering).
     - `Effects.Fast`: `stiffness = 1200f`.
     - `Effects.Dismiss`: `stiffness = 1500f`, `visibilityThreshold = 4f`.
   - **Press Token**:
     - `Press`: `dampingRatio = 0.80f`, `stiffness = 1200f`.
   - **Ambient Tokens**:
     - `Ambient.Shimmer`: `dampingRatio = 0.80f`, `stiffness = 60f`.
     - `Ambient.Breathe`: `dampingRatio = 0.80f`, `stiffness = 50f`.
   - **Pill Dock Transitions**:
     - `Pill.Throw` (muncul): `dampingRatio = 0.72f`, `stiffness = 500f` (lemparan elastis).
     - `Pill.Drop` (sembunyi): `dampingRatio = 1.0f`, `stiffness = 700f` (jatuh gravitasi).
3. **Tactile Scale Feedback**:
   - Setiap elemen yang dapat diklik mengecil saat ditekan (scale `0.93f`–`0.96f`), lalu membal kembali ke `1.0f` via pegas fisika `NoirMotion.SpatialFast` / `Press`.
4. **Bypass Developer Options 0x**:
   - Pengguna dengan *Animation Scale 0x* di Android Developer Options tetap dapat menikmati animasi halus melalui injeksi `NoirForceMotionScale`.
   - Pengguna dengan preferensi *Reduced Motion* in-app tetap menikmati transisi pegas super kaku tanpa snap instan via override `stiffSpring` (`NoBouncy`, `stiffness = 4000f`).
