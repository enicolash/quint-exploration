# Panduan Pemula: Formal Verification dengan Quint pada Studi Kasus Saldo Akun

**Dokumen:** Panduan langkah demi langkah
**Audiens:** Developer Java yang belum pernah memakai formal verification atau Quint
**Waktu:** sekitar 60 sampai 90 menit
**Prasyarat:** Node.js, Quint terpasang dan sudah berjalan (`quint run`), Java 21 untuk `quint verify`

---

## 1. Ringkasan

Kode Java pada studi kasus ini mencatat saldo akun lewat daftar transaksi debit (D) dan kredit (K). Kode ini sudah berjalan, tetapi belum punya aturan yang menjaga saldo. Akibatnya, pengeluaran bisa melebihi saldo, dan kode tetap mencetak hasil tanpa error.

Dokumen ini mengajarkan cara:

1. Menuliskan aturan bisnis dalam bahasa biasa.
2. Memodelkan aturan tersebut di Quint.
3. Menjalankan Quint dan membaca hasilnya.
4. Membandingkan model versi "asal-asalan" dengan model versi yang sudah diperbaiki.
5. Menerjemahkan hasilnya kembali ke kode Java.

## 2. Mengapa perlu ini?

Unit test biasa hanya memeriksa skenario yang kita bayangkan. Formal verification memeriksa banyak kemungkinan urutan aksi secara otomatis, sehingga kasus aneh seperti "belanja berkali-kali sampai saldo minus" ikut diperiksa.

Pada studi kasus ini, `main` hanya mencoba satu skenario: nabung 100.000, lalu belanja 20.000. Hasilnya benar. Namun kode tidak menolak jika belanja 500.000. Quint akan menemukan masalah seperti ini.

## 3. Konsep dasar (baca dulu)

| Istilah | Arti sederhana | Contoh pada studi kasus |
|---|---|---|
| **Formal verification** | Memeriksa aturan sistem secara matematis atau dengan simulasi terstruktur | Memastikan saldo tidak minus |
| **Spesifikasi** | Deskripsi sistem dalam bahasa formal, bukan kode | File `saldo_legacy.qnt` |
| **State** | Keadaan sistem pada satu waktu | `saldo = 80000` |
| **Aksi (action)** | Perubahan yang mengubah state | `deposit`, `expense` |
| **Invariant** | Aturan yang harus selalu benar di setiap state | `saldo >= 0` |
| **Counterexample** | Urutan langkah yang melanggar invariant | Belanja 50.000, lalu belanja 20.000, saldo jadi -20.000 |
| **Simulasi (`quint run`)** | Menjalankan banyak urutan aksi acak | Cepat, cocok untuk mencari bug |
| **Model checking (`quint verify`)** | Mencari pelanggaran lebih menyeluruh | Lebih lambat, butuh Java 21 |

Ingat satu hal: **Quint memeriksa model, bukan kode Java.** Model harus mencerminkan aturan bisnis yang benar. Jika model salah, hasilnya juga salah.

## 4. Analisis kode Java

### 4.1 Aturan bisnis yang diasumsikan

Sebelum menulis model, tuliskan aturannya. Ini langkah yang paling penting.

| ID | Aturan | Sumber |
|---|---|---|
| R1 | Saldo tidak boleh negatif | Asumsi bisnis. **Konfirmasi dengan pemilik produk.** |
| R2 | Saldo harus sama dengan total debit dikurangi total kredit | Cara kode menghitung saldo |
| R3 | Nominal transaksi harus lebih dari nol | Asumsi umum |

Jika pemilik produk mengizinkan saldo minus (misalnya untuk kredit), R1 harus diubah. Quint hanya bisa memeriksa aturan yang Anda tuliskan.

### 4.2 Masalah yang terlihat di kode

| No | Lokasi | Masalah | Dampak |
|---|---|---|---|
| 1 | `saveTransaction` dan `main` | Transaksi kredit disimpan tanpa mengecek saldo | Melanggar R1 |
| 2 | `createNewTransaction` | Nominal tidak divalidasi | Nominal nol atau negatif bisa masuk (melanggar R3) |
| 3 | `createNewTransaction` | Tipe transaksi bebas berupa string | Tipe `"X"` tidak dihitung, saldo diam-diam salah |
| 4 | `createNewTransaction` | `accountId` tidak dicek keberadaannya | Transaksi bisa menempel ke akun yang tidak ada |
| 5 | `saveAccount` | Akun duplikat tidak dicegah | Satu orang bisa punya dua akun |
| 6 | `findAllAccountByAccountId` | Method tidak pernah dipakai | Membingungkan, bisa dihapus atau dipakai untuk validasi |
| 7 | Seluruh kode | Tidak ada `synchronized` atau lock | Aman untuk program satu thread, berbahaya jika dipakai banyak thread |

Fokus kita adalah masalah 1 (melanggar R1), karena itu yang paling terlihat dampaknya.

### 4.3 Coba dulu di Java

Tambahkan satu baris ke `main` sebelum perhitungan saldo:

```java
Transaction besarBelanja = createNewTransaction(EdwardAccount.getAccountId(), KREDIT, 500000L);
saveTransaction(besarBelanja);
```

Saat dijalankan, saldo akan tercetak **negatif (-420.000)** dan tidak ada error. Inilah yang akan kita tunjukkan dengan Quint.

## 5. Langkah 1: Siapkan folder

Di PowerShell:

```powershell
cd D:\Java\quint-exploration
mkdir saldo
cd saldo
```

Anda akan membuat dua file:

- `saldo_legacy.qnt` (model versi asal-asalan, untuk menemukan bug)
- `saldo_fixed.qnt` (model versi yang sudah diperbaiki)

## 6. Langkah 2: Tulis model versi asal-asalan

Buat file `saldo_legacy.qnt`:

```quint
// saldo_legacy.qnt
// Model dari kode Java yang belum punya pengecekan saldo.
module saldo_legacy {
  // Nominal yang mungkin dipakai dalam satu transaksi (dalam rupiah)
  val NOMINALS = Set(20000, 50000, 100000)

  // Tiga state yang kita pantau
  var saldo: int          // saldo akun saat ini
  var totalDebit: int     // jumlah semua transaksi D (nabung)
  var totalKredit: int    // jumlah semua transaksi K (belanja)

  // Keadaan awal: akun baru, saldo 0
  action init = all {
    saldo' = 0,
    totalDebit' = 0,
    totalKredit' = 0,
  }

  // Aksi 1: nabung (setara transaksi D di Java)
  action deposit = nondet n = NOMINALS.oneOf()
    all {
      saldo' = saldo + n,
      totalDebit' = totalDebit + n,
      totalKredit' = totalKredit,
    }

  // Aksi 2: belanja (setara transaksi K di Java)
  // BUG: tidak ada pengecekan saldo, sama seperti kode Java sekarang
  action expense = nondet n = NOMINALS.oneOf()
    all {
      saldo' = saldo - n,
      totalDebit' = totalDebit,
      totalKredit' = totalKredit + n,
    }

  // Di setiap langkah, sistem boleh melakukan salah satu aksi
  action step = any {
    deposit,
    expense,
  }

  // ===== INVARIANT =====

  // R1: saldo tidak boleh negatif
  val tidakNegatif = saldo >= 0

  // R2: saldo harus sama dengan total debit dikurangi total kredit
  val konsisten = saldo == totalDebit - totalKredit
}
```

Penjelasan singkat untuk pemula:

- `var` adalah data yang berubah saat simulasi berjalan.
- `x'` berarti "nilai x di langkah berikutnya".
- `all { }` berarti semua baris di dalamnya harus terjadi bersamaan.
- `nondet n = NOMINALS.oneOf()` berarti Quint memilih salah satu nilai dari himpunan secara acak.
- `any { }` berarti Quint memilih salah satu aksi secara acak di setiap langkah.

## 7. Langkah 3: Cek tipe dan jalankan simulasi

```powershell
quint typecheck saldo_legacy.qnt
quint run saldo_legacy.qnt --invariant tidakNegatif --max-steps 10
```

Output yang mungkin muncul:

```
[State 0] { saldo: 0, totalDebit: 0, totalKredit: 0 }
[State 1] { saldo: -50000, ... }
[violation] Found an issue
```

**Cara membaca output:**

- `[State 0]` adalah keadaan awal. Akun baru punya saldo nol.
- Setiap `[State n]` adalah keadaan setelah satu aksi.
- `[violation]` berarti Quint menemukan langkah yang melanggar invariant.

Jalankan juga invariant kedua:

```powershell
quint run saldo_legacy.qnt --invariant konsisten --max-steps 10
```

Invariant `konsisten` tidak akan dilanggar, karena kedua aksi selalu memperbarui `saldo`, `totalDebit`, dan `totalKredit` bersamaan. Ini hasil yang diharapkan. Artinya **kode Java tidak salah hitung**, masalahnya ada di **aturan yang tidak dijaga**.

Catat `--seed` dari output jika Anda ingin mengulang hasil yang sama.

## 8. Langkah 4: Lihat urutan langkah yang melanggar

Untuk melihat detail urutan langkah:

```powershell
quint run saldo_legacy.qnt --invariant tidakNegatif --max-steps 10 --verbosity=3
```

Anda akan melihat setiap state. Cari baris yang saldonya menjadi negatif, lalu tulis ulang urutannya dalam bahasa biasa, misalnya:

> Nabung 20.000, lalu belanja 50.000. Saldo menjadi -30.000.

Urutan seperti ini persis yang bisa Anda uji di Java.

## 9. Langkah 5: Tulis model versi yang sudah diperbaiki

Perbaikan paling sederhana: aksi belanja hanya boleh terjadi jika saldo cukup.

Buat file `saldo_fixed.qnt`:

```quint
// saldo_fixed.qnt
// Model desain baru: belanja hanya boleh jika saldo mencukupi.
module saldo_fixed {
  val NOMINALS = Set(20000, 50000, 100000)

  var saldo: int
  var totalDebit: int
  var totalKredit: int

  action init = all {
    saldo' = 0,
    totalDebit' = 0,
    totalKredit' = 0,
  }

  action deposit = nondet n = NOMINALS.oneOf()
    all {
      saldo' = saldo + n,
      totalDebit' = totalDebit + n,
      totalKredit' = totalKredit,
    }

  // PERBAIKAN: guard "saldo >= n" menolak belanja yang melebihi saldo
  action expense = nondet n = NOMINALS.oneOf()
    all {
      saldo >= n,
      saldo' = saldo - n,
      totalDebit' = totalDebit,
      totalKredit' = totalKredit + n,
    }

  action step = any {
    deposit,
    expense,
  }

  val tidakNegatif = saldo >= 0
  val konsisten = saldo == totalDebit - totalKredit
}
```

Perhatikan baris `saldo >= n`. Ini adalah **guard**: aksi hanya boleh berjalan jika kondisinya benar. Inilah yang belum ada di kode Java.

Jalankan:

```powershell
quint typecheck saldo_fixed.qnt
quint run saldo_fixed.qnt --invariant tidakNegatif --max-steps 50
quint run saldo_fixed.qnt --invariant konsisten --max-steps 50
```

Kali ini tidak ada pelanggaran. Jalankan beberapa kali atau naikkan `--max-steps` untuk keyakinan lebih.

## 10. Langkah 6: Terjemahkan ke Java

Model yang benar belum berarti kode benar. Kode Java perlu diubah agar mengikuti guard tadi.

Ubah `main` (atau buat method baru) agar mengecek saldo sebelum menyimpan transaksi K:

```java
private static boolean belanja(String accountId, Long nominal) {
    if (nominal <= 0) {
        throw new IllegalArgumentException("Nominal harus lebih dari nol");
    }
    Long saldoSekarang = sumTransactionByAccountId(accountId);
    if (saldoSekarang < nominal) {
        return false; // tolak: saldo tidak cukup
    }
    return saveTransaction(createNewTransaction(accountId, KREDIT, nominal));
}
```

Lalu di `main`, ganti pemanggilan belanja dengan:

```java
boolean berhasil = belanja(EdwardAccount.getAccountId(), 500000L);
System.out.println("Belanja berhasil? " + berhasil);
```

Sekarang saldo tidak akan negatif. Output akan menunjukkan `Belanja berhasil? false`.

Untuk memastikan perbaikannya bekerja, tambahkan test:

```java
// src/test/java/QuintTestingTest.java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QuintTestingTest {
    @Test
    void belanjaDitolakJikaSaldoKurang() {
        // Catatan: pastikan method belanja dan databaseTransaction bisa diakses dari test,
        // misalnya dengan membuatnya package-private dan mengosongkan list sebelum tiap test.
    }
}
```

Catatan: kode Java saat ini memakai `static` dan `private` untuk semua method, sehingga sulit diuji. Refaktor kecil ke class service terpisah akan sangat membantu. Ini pekerjaan lanjutan, bukan syarat untuk memahami Quint.

## 11. Langkah 7: Urutan kerja yang disarankan (SDLC)

```
Aturan bisnis (R1, R2, R3)
        ↓
Model Quint versi asal-asalan → temukan bug
        ↓
Model Quint versi baru → buktikan aturan terjaga
        ↓
Ubah kode Java → tambah guard
        ↓
Test Java → pastikan kode sama dengan model
        ↓
Review PR (kode dan .qnt)
        ↓
Gerbang CI: quint typecheck dan quint run
        ↓
Production
```

Aturan praktis: **tulis model sebelum mengubah kode.** Dengan begitu, bug desain ketahuan sebelum ada kode yang harus diperbaiki.

## 12. Langkah 8 (opsional): Verifikasi lebih menyeluruh

Setelah Java 21 aktif (sesuai langkah sebelumnya), jalankan:

```powershell
quint verify saldo_legacy.qnt --invariant tidakNegatif
quint verify saldo_fixed.qnt --invariant tidakNegatif
```

Hasil yang diharapkan:

- `saldo_legacy.qnt` melaporkan pelanggaran dengan counterexample.
- `saldo_fixed.qnt` melaporkan tidak ada pelanggaran.

Perbedaan utama dengan `quint run`: `verify` memeriksa semua kemungkinan dalam batas tertentu, sedangkan `run` hanya mengambil sampel acak.

## 13. Troubleshooting

| Gejala | Penyebab kemungkinan | Solusi |
|---|---|---|
| `quint typecheck` error pada `nondet` | Sintaks `nondet` memerlukan `all` atau `any` setelahnya | Pastikan ada blok `all { }` atau `any { }` di bawahnya |
| Tidak ada pelanggaran di `saldo_legacy` | Simulasi terlalu pendek atau sampel acak tidak kena | Naikkan `--max-steps` atau jalankan beberapa kali |
| `Unknown identifier` | Nama variabel salah ketik | Cek huruf besar dan kecil, Quint sensitif terhadap ini |
| `quint verify` masih error Java | Java 17 masih aktif | Jalankan `java --version` di sesi yang sama |
| Hasil `run` berbeda tiap kali | Simulasi memang acak | Pakai `--seed` untuk mengulang hasil yang sama |

## 14. FAQ

**Q: Apakah saya harus menguasai formal verification dulu?**
Tidak. Cukup pahami empat hal: state, aksi, guard, dan invariant. Sisanya bisa dipelajari sambil berjalan.

**Q: Apakah Quint menggantikan unit test?**
Tidak. Quint memeriksa desain dan aturan. Unit test memeriksa kode. Keduanya dipakai bersama.

**Q: Bagaimana jika aturan bisnis saya berbeda, misalnya boleh saldo minus?**
Ubah invariant `tidakNegatif` sesuai aturan yang disepakati. Quint hanya memeriksa aturan yang Anda tulis, jadi aturan harus jelas dulu.

**Q: Kenapa saldo di Quint hanya bilangan bulat?**
Karena studi kasus ini memakai rupiah utuh, tanpa desimal. Untuk uang dengan sen, gunakan satuan terkecil (misalnya sen sebagai `int`) dan pastikan konsisten di Java.

**Q: Kapan saya perlu `quint verify`?**
Saat aturannya penting (misalnya uang atau keamanan) dan Anda ingin hasil yang lebih kuat dari simulasi.

## 15. Glosarium singkat

- **D (Debit):** dalam studi kasus ini berarti nabung, menambah saldo.
- **K (Kredit):** dalam studi kasus ini berarti belanja, mengurangi saldo.
- **Guard:** syarat yang harus benar agar aksi boleh berjalan.
- **Invariant:** aturan yang harus selalu benar di setiap state.
- **State:** keadaan sistem pada satu waktu.
- **Trace:** urutan state dari awal sampai akhir simulasi.

## 16. Langkah berikutnya

1. Jalankan `saldo_legacy.qnt` dan catat urutan yang melanggar.
2. Jalankan `saldo_fixed.qnt` dan pastikan tidak ada pelanggaran.
3. Ubah kode Java dengan guard seperti di bagian 10.
4. Tambahkan aturan R3 (nominal harus lebih dari nol) ke model Quint dan ulangi langkah 1 sampai 3.
5. Tulis test Java untuk skenario yang Quint temukan.