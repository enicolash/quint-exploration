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