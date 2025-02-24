package com.analyzer.models

case class WalletStats(
  profitLast30Days: Double,
  winRate: Double,
  averageTradeSize: Double,
  totalTrades: Int,
  bestTrade: Double,
  worstTrade: Double,
  averageHoldTime: Double
)