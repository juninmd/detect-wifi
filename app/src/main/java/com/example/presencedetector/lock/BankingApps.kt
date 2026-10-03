package com.example.presencedetector.lock

/** Well-known banking and payment apps (Brazil), pre-selected the first time the lock is set up. */
object BankingApps {
  val DEFAULTS: Set<String> =
    setOf(
      "com.nu.production", // Nubank
      "com.itau", // Itaú
      "br.com.bb.android", // Banco do Brasil
      "com.bradesco", // Bradesco
      "com.santander.app", // Santander
      "br.com.gabba.Caixa", // Caixa
      "br.com.intermedium", // Inter
      "com.c6bank.app", // C6 Bank
      "com.picpay", // PicPay
      "com.mercadopago.wallet", // Mercado Pago
      "br.com.uol.ps.myaccount", // PagBank
      "br.com.neon", // Neon
      "br.com.sicredi.app", // Sicredi
    )
}
