module counter {
  var count: int

  action init = {
    count' = 0
  }

  action inc = {
    count' = count + 1
  }

  action dec = {
    count' = count - 1
  }

  action step = inc

  // Properti: count tidak boleh negatif. Ini akan dilanggar oleh dec.
  val nonNegative = count >= 0
}