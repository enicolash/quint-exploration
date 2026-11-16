# Panduan Umum Memperbaiki Kode Legacy Java dengan Pendekatan Formal (Tanpa Instalasi Tambahan)

**Dokumen:** Panduan langkah demi langkah, berlaku untuk kode legacy apa pun
**Syarat:** JDK 17 atau lebih baru, JUnit 5 (sudah dipakai di proyek). Tidak memerlukan Quint, TLA+, Lean, atau library baru.
**Estimasi waktu:** 1 sampai 2 hari untuk satu modul, tergantung ukuran kode.
**Pemilik dokumen:** Tim pengembang

---

## 1. Tujuan

Kode legacy sering bekerja untuk skenario yang biasa, tetapi gagal untuk urutan kejadian yang tidak terduga. Dokumen ini memberi cara sistematis untuk:

1. Memahami apa yang sebenarnya dilakukan kode.
2. Menuliskan aturan yang harus selalu benar.
3. Menemukan bug tanpa menebak.
4. Memperbaiki kode dengan aman.
5. Membuktikan perbaikan dengan memeriksa banyak urutan kejadian, bukan hanya beberapa contoh.

Pendekatan ini tidak mengharuskan Anda memakai alat formal. Cara berpikirnya yang formal, alatnya cukup Java dan JUnit.

## 2. Istilah dasar

| Istilah | Arti sederhana |
|---|---|
| **State** | Data yang berubah dari waktu ke waktu (field, daftar, map). |
| **Aksi** | Method yang mengubah state. |
| **Guard** | Syarat yang harus benar agar aksi boleh berjalan. |
| **Invariant** | Aturan yang harus selalu benar, di setiap state. |
| **Counterexample** | Urutan aksi yang melanggar invariant. |
| **Bounded checking** | Mencoba semua urutan aksi sampai panjang tertentu. |
| **Mutasi uji** | Sengaja merusak kode untuk memastikan test bisa mendeteksinya. |

## 3. Langkah 1: Pahami kode sebelum mengubahnya

Jangan mengubah apa pun dulu. Lakukan pembacaan terstruktur.

**Tugas:**
1. Cari semua field, variabel static, dan koleksi yang disimpan di luar method. Itu kandidat **state**.
2. Cari semua method yang menulis ke state tersebut. Itu kandidat **aksi**.
3. Cari method yang hanya membaca. Itu **bukan** aksi, cukup dicatat sebagai pembaca.
4. Gambar alur sederhana: aksi apa bisa terjadi, dan dalam urutan apa.

**Template pencatatan:**

| Nama | Tipe | Ditulis oleh | Dibaca oleh |
|---|---|---|---|
| `contohField` | `List<X>` | `tambah()`, `hapus()` | `cari()`, `hitung()` |

**Hasil langkah ini:** daftar state dan daftar aksi. Tidak ada kode yang berubah.

## 4. Langkah 2: Tulis aturan bisnis dalam kalimat

Ini langkah terpenting, dan tidak bisa dijawab oleh kode saja. Kode menunjukkan apa yang **dilakukan**, bukan apa yang **seharusnya** dilakukan.

**Cara:**
1. Untuk setiap state, tulis: "Nilai ini seharusnya selalu..." (misalnya: "jumlah stok tidak boleh negatif").
2. Untuk setiap aksi, tulis: "Aksi ini hanya boleh terjadi jika..." (misalnya: "pengiriman hanya boleh jika stok cukup").
3. Tanyakan ke pemilik produk atau analis bisnis. Tandai setiap aturan sebagai **terkonfirmasi** atau **perlu konfirmasi**.

**Template:**

| ID | Aturan | Sumber | Status |
|---|---|---|---|
| R1 | Stok tidak boleh negatif. | Diskusi dengan pemilik produk | Terkonfirmasi |
| R2 | Pengiriman hanya boleh jika stok cukup. | Kode lama | Perlu konfirmasi |

**Peringatan:** jika kode lama melanggar aturan yang masuk akal, itu bug. Jangan menyesuaikan aturan dengan kode yang salah.

## 5. Langkah 3: Tulis spesifikasi tertulis

Susun satu halaman spesifikasi dengan struktur yang sama setiap kali. Ini nanti bisa dibaca orang lain, atau diterjemahkan ke alat formal jika tim mengizinkan.

**Template:**

```
Modul: <nama>

STATE
  - <nama state>: <tipe>, <arti>

KONDISI AWAL (init)
  - <nilai awal setiap state>

AKSI
  - <nama aksi>(<parameter>)
      guard: <syarat>
      efek:  <perubahan state>

INVARIANT
  - INV-1: <aturan>  (dari R1)
  - INV-2: <aturan>  (dari R2)
```

Contoh singkat:

```
Modul: Keranjang Belanja

STATE
  - items: daftar (produk, jumlah)

KONDISI AWAL
  - items kosong

AKSI
  - tambah(produk, jumlah)
      guard: jumlah > 0
      efek:  items bertambah
  - checkout()
      guard: items tidak kosong
      efek:  items dikosongkan

INVARIANT
  - INV-1: setiap jumlah di items > 0
```

## 6. Langkah 4: Tulis test yang membuktikan bug ada

Sebelum mengubah kode, buktikan bahwa bug-nya nyata.

**Cara:**
1. Ambil satu invariant dari langkah 3.
2. Cari skenario tercepat yang bisa melanggarnya. Biasanya dua atau tiga aksi.
3. Tulis test JUnit yang mengharapkan invariant tetap terjaga.
4. Jalankan test pada kode lama. Test **harus gagal**.

**Template test:**

```java
@Test
void <nama_invariant>_harusTerjaga() {
    // Arrange: buat kondisi awal
    // Act: jalankan urutan aksi yang berisiko
    // Assert: invariant harus benar
}
```

Jika test tidak gagal pada kode lama, berarti skenario Anda belum tepat, atau bug-nya tidak ada. Periksa lagi.

## 7. Langkah 5: Pisahkan logika ke class yang bisa diuji

Kode legacy sering memakai `static`, `private`, dan logika tersebar. Itu sulit diuji.

**Cara:**
1. Buat class baru yang memegang state dan semua aksi (misalnya `KeranjangService`, `AccountLedger`).
2. Pindahkan field dan method yang mengubah state ke class tersebut.
3. Buat method publik untuk setiap aksi dari langkah 3.
4. Pindahkan logika validasi ke aksi itu, sehingga guard berada di satu tempat.

**Aturan:**
- Satu state, satu class yang memilikinya.
- Tidak ada jalur lain yang mengubah state tanpa lewat class ini.
- Jangan ubah perilaku saat refaktor. Pindahkan dulu, lalu ubah perilaku di langkah berikutnya.

## 8. Langkah 6: Terapkan guard dan perbaiki bug

Sekarang perbaiki bug dengan syarat yang sudah dicatat di spesifikasi.

**Prinsip:**
- Guard dicek **di dalam** method yang sama dengan penulisan state.
- Jika guard gagal, aksi ditolak. Tidak ada perubahan state.
- Pilih cara penolakan yang konsisten: `false`, exception, atau `Optional`. Tulis pilihannya di spesifikasi.
- Untuk konkurensi, tandai method yang mengubah state dengan `synchronized`, atau gunakan lock yang sesuai.

**Contoh pola:**

```java
public synchronized boolean aksi(long nilai) {
    validasi(nilai);                // input harus valid
    if (!guard()) {                 // syarat aksi
        return false;               // tolak, state tidak berubah
    }
    ubahState(nilai);               // efek aksi
    return true;
}
```

Jalankan test dari langkah 4. Test harus **lolos** sekarang.

## 9. Langkah 7: Periksa semua urutan aksi (bounded checking)

Test contoh hanya mencoba skenario yang dibayangkan. Langkah ini mencoba **semua urutan** aksi sampai panjang tertentu.

**Cara:**
1. Tentukan alfabet aksi: daftar aksi dengan parameter kecil yang mewakili kasus penting (misalnya nilai di bawah, sama dengan, dan di atas batas).
2. Tulis test yang menghasilkan semua urutan sepanjang `KEDALAMAN`.
3. Jalankan setiap urutan dari state awal yang baru.
4. Cek invariant setelah setiap langkah.

**Template:**

```java
class <Nama>ModelTest {

    private static final int KEDALAMAN = 6;
    // Alfabet: indeks -> aksi
    private static final int JUMLAH_AKSI = 4;

    @Test
    void semuaUrutanMenjagaInvariant() {
        List<String> pelanggaran = new ArrayList<>();
        cobaSemua(new int[KEDALAMAN], 0, pelanggaran);
        assertTrue(pelanggaran.isEmpty(),
            "ada " + pelanggaran.size() + " urutan melanggar, contoh: " +
            (pelanggaran.isEmpty() ? "-" : pelanggaran.get(0)));
    }

    private static void cobaSemua(int[] urutan, int posisi, List<String> pelanggaran) {
        if (posisi == urutan.length) {
            cekUrutan(urutan, pelanggaran);
            return;
        }
        for (int a = 0; a < JUMLAH_AKSI; a++) {
            urutan[posisi] = a;
            cobaSemua(urutan, posisi + 1, pelanggaran);
        }
    }

    private static void cekUrutan(int[] urutan, List<String> pelanggaran) {
        // 1. buat objek baru
        // 2. jalankan setiap aksi sesuai indeks
        // 3. setelah setiap langkah, cek invariant. Jika gagal, catat jejak dan keluar.
    }
}
```

**Perkiraan jumlah urutan:** jumlah aksi pangkat kedalaman. Dengan 4 aksi dan kedalaman 6, ada 4.096 urutan. Dengan kedalaman 8, ada 65.536.

**Pilih kedalaman dengan bijak:**
- Mulai dari 4 atau 5 agar cepat.
- Naikkan sampai waktu eksekusi masih nyaman, biasanya di bawah beberapa detik.
- Catat kedalaman yang dipakai di dokumen.

**Tentang keterbatasan:** teknik ini hanya memeriksa kombinasi sampai kedalaman dan nilai yang dipilih. Ia tidak membuktikan kebenaran untuk semua kemungkinan. Itu sebabnya nilai di alfabet harus mewakili kasus penting.

## 10. Langkah 8: Mutasi uji

Pastikan test Anda benar-benar bisa mendeteksi bug.

**Cara:**
1. Sengaja hapus atau balik guard utama di kode.
2. Jalankan test model dan test contoh.
3. Test harus **gagal**.
4. Kembalikan kode, lalu jalankan ulang. Test harus **lolos**.

Jika test tetap lolos saat guard dihapus, test Anda belum cukup kuat. Tambahkan skenario atau nilai di alfabet.

## 11. Langkah 9: Test invariant satu per satu

Setiap invariant sebaiknya punya test sendiri, dengan nama yang jelas.

```java
@Test void inv1_stokTidakNegatif() { ... }
@Test void inv2_totalSesuaiRiwayat() { ... }
```

Nama test menjadi dokumentasi. Saat invariant gagal, orang langsung tahu aturan mana yang dilanggar.

## 12. Langkah 10: Integrasi dan pembersihan

- Ganti pemanggilan di seluruh kode dengan class baru.
- Hapus method `static` lama yang sudah tidak dipakai.
- Jangan hapus kode lama sampai semua test baru lolos dan pemanggilnya sudah diganti.
- Jalankan seluruh test proyek, bukan hanya test modul ini.

## 13. Checklist sebelum merge

- [ ] Setiap state dan aksi sudah tercatat di spesifikasi.
- [ ] Setiap aturan punya status (terkonfirmasi atau perlu konfirmasi).
- [ ] Test yang membuktikan bug gagal pada kode lama dan lolos pada kode baru.
- [ ] Setiap invariant punya test sendiri.
- [ ] Test model lolos pada kedalaman yang sudah dicatat.
- [ ] Mutasi uji dilakukan dan test berhasil mendeteksinya.
- [ ] Semua method yang mengubah state dipusatkan di satu class.
- [ ] Assertion dijalankan dengan `-ea` saat test.
- [ ] Tidak ada dependency baru.
- [ ] Spesifikasi di dokumen sesuai kode final.

## 14. Langkah 11: Review

Saat review, periksa pertanyaan berikut:
- Apakah ada jalur kode yang mengubah state tanpa lewat class baru?
- Apakah guard berada di method yang sama dengan penulisan state?
- Apakah aturan bisnis sudah dikonfirmasi, atau masih asumsi?
- Apakah alfabet aksi sudah mewakili nilai batas?
- Apakah kedalaman cukup untuk urutan yang paling berisiko?

## 15. Troubleshooting

| Gejala | Kemungkinan penyebab | Solusi |
|---|---|---|
| Test contoh gagal padahal sudah diperbaiki | Guard dan efek ditulis di tempat berbeda | Pindahkan keduanya ke satu method |
| Test model lambat | Kedalaman terlalu besar | Turunkan kedalaman atau kurangi alfabet |
| Test model tidak menemukan bug | Alfabet tidak mencakup nilai batas | Tambahkan nilai di bawah, sama dengan, dan di atas batas |
| Hasil berbeda setiap kali | Ada `Random` atau `UUID` di logika | Pisahkan sumber acak, atau gunakan nilai tetap di test |
| Invariant lolos tapi bug masih ada | Invariant kurang lengkap | Kembali ke langkah 2, tambahkan aturan yang belum tercatat |

## 16. Pemetaan jika nanti memakai alat formal

Spesifikasi dari langkah 3 bisa diterjemahkan langsung:

| Dokumen ini | Quint | TLA+ |
|---|---|---|
| State | `var` | `VARIABLE` |
| Aksi dengan guard | `action` dengan `all { guard, efek }` | `Action` dengan `/\` |
| Invariant | `val` | `Inv ==` |
| Bounded checking | `quint run` atau `quint verify` | TLC atau Apalache |

Karena spesifikasi sudah ditulis dengan struktur sama, tidak perlu mulai dari nol jika alat formal diizinkan nanti.

## 17. Glosarium

- **Legacy:** kode yang sudah berjalan tetapi sulit diubah dengan aman.
- **Refaktor:** mengubah struktur kode tanpa mengubah perilaku.
- **Synchronized:** membuat hanya satu thread yang boleh masuk ke method dalam satu waktu.
- **Test-first:** menulis test yang gagal sebelum memperbaiki kode.