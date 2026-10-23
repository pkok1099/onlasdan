# Katalog Fitur & Kegunaan — Onlasdan Android

Dokumen ini menjelaskan seluruh fitur, fungsionalitas, dan kegunaan aplikasi **Onlasdan Android**.

---

## 1. Multi-Source Manga & Comic Aggregator

Onlasdan mengintegrasikan berbagai sumber baca komik Indonesia populer ke dalam satu antarmuka yang seragam dan bersih tanpa iklan pop-up:

- **Shinigami (`shinigami`)**: Koleksi Manhwa, Manhua, dan Manga terlengkap melalui integrasi REST API murni.
- **Komikindo (`komikindo`)**: Scraping komik terjemahan bahasa Indonesia dengan filter genre dan status komprehensif.
- **Komiku (`komiku`)**: Platform baca komik populer dengan tata letak peringkat harian, update tercepat, dan pustaka luas.
- **DoujinDesu (`doujindesu`)**: Sumber doujinshi dan manga dengan payload API terenkripsi AES-256-CBC dan proxy gambar terproteksi.

---

## 2. Beranda & Penemuan Komik (Home & Discovery)

- **Hero Cinema Carousel**: Tampilan banner komik unggulan dengan gradient overlay gelap dan metadata status/rating.
- **Pill Section Headers**: Navigasi bagian yang jelas (Series Populer, Update Terbaru, Komik Hot, dsb.) dengan tombol aksi "Lihat Semua" bertarget sentuh luas (≥44dp).
- **Slot-Based Blueprint Layouts**: Tiap source memiliki susunan beranda yang disesuaikan secara deklaratif (misal: Shinigami memiliki format tab manga/manhwa/manhua; Komiku memiliki peringkat harian).
- **Skeleton Shimmer Loading**: Animasi loading berkonten-sadar yang mulus tanpa kedip layout (*content-aware shimmer*).

---

## 3. Gestalt-Driven Comic Detail Sheet

Detail komik ditampilkan melalui modal bottom sheet yang elegan dan intuitif:
- **Integrated Header Info**: Poster cover, judul, author, status, rating bintang, dan tombol aksi tersusun rapat dalam satu region visual (*Law of Common Region & Proximity*).
- **Segmented Filter Pill**: Filter chapter cepat (*Semua*, *Belum Dibaca*, *Terunduh*) dengan indikator aktif yang terhubung (*Uniform Connectedness & Similarity*).
- **Focal Point Chapter Hierarchy**:
  - Chapter yang **belum dibaca** ditonjolkan dengan teks kontras tinggi (`TextPrimary`).
  - Chapter yang **sudah dibaca** ditenangkan ke warna sekunder (`TextSecondary`) dengan ikon checkmark aksen pink hemat.
- **Aksi Cepat**: Tombol baca bab pertama/terbaru, bookmarking, dan unduh per-chapter dengan touch target yang ramah jari (*Fitts's Law* ≥44dp).

---

## 4. Universal Hold-Tap Morphing Preview

Fitur interaksi khas Onlasdan:
- **Tekan & Tahan (Long Press)** pada kartu komik mana pun di seluruh source (Beranda, Jelajah, Peringkat, dsb.) untuk memunculkan **kartu preview melayang (morphing card)**.
- **Taktil Feedback**: Menggetarkan haptik ponsel saat kartu ber-morphing ke tengah layar.
- Menampilkan sinopsis ringkas, cover resolusi tinggi, rating, dan tombol cepat untuk membaca langsung tanpa harus membuka halaman detail.

---

## 5. Webtoon & Manga Reader Modern

Pembaca chapter yang dirancang untuk kenyamanan membaca berjam-jam:
- **Vertical Infinite Webtoon Canvas**: Scroll vertikal tanpa jeda dengan decoding gambar berkinerja tinggi.
- **Continuous Next-Chapter Prefetcher**: Memuat gambar bab berikutnya secara otomatis di latar belakang saat pembaca mendekati akhir chapter, memungkinkan transisi chapter tanpa henti (*seamless scrolling*).
- **Auto-Scroll Pintar**: Gulir otomatis dengan slider pengatur kecepatan (`10–300 px/detik`), jeda cerdas saat disentuh jari, dan timer auto-next chapter.
- **Immersive Fullscreen & Cutout Handling**: Menyembunyikan status bar dan navigasi bar secara cerdas dengan proteksi display cutout / notch agar gambar tidak terpotong.
- **Night Dimmer Filter**: Pengatur redup malam in-app khusus untuk membaca di ruangan gelap tanpa menyilaukan mata.
- **Offline Fallback**: Otomatis mendeteksi jika chapter sudah diunduh untuk dibaca offline tanpa koneksi internet.

---

## 6. Sistem Download & Baca Offline

- **Manajer Unduhan**: Mengunduh bab komik secara asinkronus ke penyimpanan internal / Scoped Storage via `DocumentFile`.
- **Indikator Progres**: Menampilkan status antrean, jumlah halaman terunduh, dan tombol pembatalan.
- **Integrasi Database**: Mencatat bab yang tersimpan di disk lokal agar dapat diakses kapan saja meski perangkat dalam Mode Pesawat.

---

## 7. Riwayat Baca & Bookmark

- **Penyimpanan Lokal Room DB**: Semua progres membaca (chapter terakhir, halaman terakhir, tanggal/waktu baca, dan offset scroll) tersimpan aman di database lokal tanpa perlu login.
- **Sinkronisasi Riwayat Otomatis**: Setiap chapter yang dibuka otomatis diperbarui di tab Riwayat.
- **Koleksi Favorit (Bookmark)**: Simpan komik favorit dengan tanda notifikasi chapter baru.

---

## 8. Jelajah & Filter Lanjutan (Browse & Search)

- **Pencarian Cepat dengan Debounce (300ms)**: Mengetik kata kunci pencarian tanpa spamming server.
- **Filter Multi-Kategori**:
  - Filter Tipe (Manga, Manhwa, Manhua, Doujinshi)
  - Filter Genre lengkap (hingga 112+ genre resmi)
  - Filter Status (Ongoing, Tamat)
  - Filter Urutan (Populer, Update Terbaru, Rating, Acak)
  - Filter Periode Waktu (Harian, Mingguan, Semua Waktu)
- **Pagination Cepat**: Navigasi halaman sebelumnya dan selanjutnya dengan caching memori instan.

---

## 9. Pengaturan & Aksesibilitas

- **Manajemen Cache**: Tombol bersih memori dan disk cache gambar dalam sekali ketuk.
- **In-App Animation Mode**: Memilih animasi dinamis penuh atau reduced motion yang tetap berbasis fisika pegas lembut.
- **Keep Screen On**: Opsi agar layar tidak mati otomatis saat membaca komik.
