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