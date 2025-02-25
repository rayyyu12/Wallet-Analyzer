package com.analyzer.models

import java.time.Instant

case class Transaction(
  signature: String,
  timestamp: Instant,
  amount: Double,
  transactionType: String,
  fee: Double,
  success: Boolean
)