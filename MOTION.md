# Panduan Arsitektur Animasi & Interaksi — "Noir & Blush"

Acuan resmi saat memoles UI onlasdan. Berlaku SETELAH urusan kloning/audit
source web selesai. Jangan eksekusi sebelum ada perintah.

**Prinsip keras: 100% Pure Physics, Zero ms.** Sistem animasi kita murni
berbasis fisika pegas (spring) dan bebas dari durasi waktu tetap — tidak ada
tween() dan tidak ada angka milidetik di dalam MotionTokens.

## 1. Filosofi Desain: Hidup tapi Tetap Elegan (Bukan Kartun)

Kita tidak menggunakan komponen visual M3E. Kesan "hidup" dan mewah dibangun
di atas tema custom Noir & Blush melalui:

- **Tactile Response:** Setiap elemen interaktif (tombol, card, chip) wajib
  punya feedback fisik saat disentuh — scale down ke 0.95f–0.97f saat ditekan,
  lalu membal kembali ke 1.0f dengan pegas.
- **Depth & Connected Transitions:** Saat sheet/dialog muncul, beri efek
  kedalaman — konten latar sedikit meredup dan mengecil ke scale 0.98f.
  Hindari kemunculan halaman/elemen secara instan.
- **Hukum Massa (Mass & Weight):** Elemen kecil (ikon, pill indicator, switch)
  bergerak dengan stiffness tinggi (1200f–1500f). Elemen besar (Bottom Sheet,
  expandable card) bergerak lebih tenang dan berbobot (stiffness 400f–600f).

## 2. Spesifikasi Fisika Pegas (Noir Physics Tokens)

Jangan gunakan pantulan berlebihan (DampingRatioHighBouncy / 0.5f).
Pisahkan mutlak antara ruang dan efek visual:

- **Spatial Tokens** (posisi, ukuran, scale, bentuk; varian Float, Dp, IntOffset):
  - `Spatial` (default): `dampingRatio = 0.80f`, `stiffness = 800f`. Ayunan rem magnetik sangat halus di akhir gerakan.
  - `SpatialFast`: `dampingRatio = 0.72f`, `stiffness = 900f`. Elemen kecil, tactile recovery, pop icon.
  - `SpatialMedium`: `dampingRatio = 0.75f`, `stiffness = 550f`. Indikator pill dock, card entrance berbobot.
  - `SpatialHeavy`: `dampingRatio = 0.82f`, `stiffness = 380f`. Transisi layar, sheet morphing, keyboard IME dock offset.
  - `SpatialSlow`: `dampingRatio = 0.72f`, `stiffness = 60f`. Entrance kartu sinematik (~1 detik) agar halftone burst + SFX stamp terbaca, bukan sia-sia.
- **Effects Tokens** (alpha/opacity & warna; NoBouncy):
  - `Effects` (default): `dampingRatio = Spring.DampingRatioNoBouncy (1.0f)`, `stiffness = 1000f` (Float & Color) agar transisi warna dan transparansi mulus tanpa flickering.
  - `Effects.Fast`: `stiffness = 1200f` untuk perubahan efek yang lebih gesit.
  - `Effects.Dismiss`: `stiffness = 1500f`, `visibilityThreshold = 4f` untuk lembar dismiss yang responsif.
- **Press Tokens**:
  - `Press`: `dampingRatio = 0.80f`, `stiffness = 1200f`. Basis fisik respons sentuhan cepat (`springPress()`).
- **Ambient Tokens**:
  - `Ambient.Shimmer`: `dampingRatio = 0.80f`, `stiffness = 60f`.
  - `Ambient.Wave`: `dampingRatio = 0.86f`, `stiffness = 14f` untuk wave skeleton — sweep pita cahaya ~2x lebih lambat agar gelombang di atas screentone tenang.
  - `Ambient.Breathe`: `dampingRatio = 0.80f`, `stiffness = 50f` untuk skeleton slot reader.
- **Pill Tokens**:
  - `Pill.Throw` (muncul): dilempar dari bawah dengan momentum (`dampingRatio = 0.72f`, `stiffness = 500f`).
  - `Pill.Drop` (sembunyi): jatuh ditarik gravitasi (`dampingRatio = 1.0f`, `stiffness = 700f`).

## 3. Penanganan Developer Options (Animation 0x) & Aksesibilitas

Banyak power user mematikan animasi di Developer Options (0x) agar OS terasa
cepat, tapi tetap ingin menikmati animasi di dalam aplikasi kita.

- **Bypass System 0x Scale:** Jangan biarkan TRANSITION_ANIMATION_SCALE = 0f
  dari sistem mematikan animasi Compose kita menjadi snap instan. Paksa
  MotionDurationScale di konteks Compose kita tetap 1f — atau sediakan
  in-app toggle di Settings: "Animasi UI: Aktif / Minim" (default **Aktif**).
- **Definisi "Reduced Motion" yang Sopan:** Bagi pengguna yang benar-benar
  memilih mode Minim Animasi (aksesibilitas), JANGAN gunakan if/else instan
  (snap) yang membuat UI patah-patah — dan JANGAN pakai crossfade berdurasi
  ms. Tetap gunakan spring sepenuhnya, cukup kunci ke pegas super kaku tanpa
  pantulan: `spring(dampingRatio = Spring.DampingRatioNoBouncy,
  stiffness = 4000f)`. Tidak ada efek membal yang memicu motion sickness,
  tetapi transisi UI tetap halus dan tetap murni fisika.

Catatan: ini mengubah perilaku `rememberReducedMotion()` yang sekarang —
snap instan diganti pegas kaku / crossfade singkat.

## 4. Target Eksekusi Pertama (nanti, setelah kloning source selesai)

- **Lingkaran Selected di Pill Dock:** Ganti snap if/else dengan animasi
  terpisah — scale (0.8f → 1.0f) pakai Spatial Spring (dampingRatio ~0.8f),
  alpha (0f → 1f) pakai Effects Spring (NoBouncy).
- **Show/Hide Pill Dock:** Show = "dilempar dari bawah" — `Pill.Throw`
  (`dampingRatio = 0.72f`, `stiffness = 500f`) dengan sedikit momentum/overshoot.
  Hide = "jatuh" — `Pill.Drop` drop tanpa pantulan
  (`dampingRatio = 1.0f`, `stiffness = 700f`).
  Tanpa tween dan tanpa angka milidetik.
