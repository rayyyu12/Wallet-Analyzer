package com.analyzer.services

import com.analyzer.models.{Transaction, Trade, WalletStats}
import com.analyzer.utils.DateUtils
import java.time.Instant

class AnalyzerService {
  def analyzeWallet(transactions: List[Transaction]): WalletStats = {
    val trades = identifyTrades(transactions)
    val thirtyDaysAgo = Instant.now().minusSeconds(30 * 24 * 60 * 60)
    
    val recentTrades = trades.filter(_.entryTime.isAfter(thirtyDaysAgo))
    
    WalletStats(
      profitLast30Days = calculateProfit(recentTrades),
      winRate = calculateWinRate(recentTrades),
      averageTradeSize = calculateAverageTradeSize(recentTrades),
      totalTrades = recentTrades.size,
      bestTrade = findBestTrade(recentTrades),
      worstTrade = findWorstTrade(recentTrades),
      averageHoldTime = calculateAverageHoldTime(recentTrades)
    )
  }

  private def identifyTrades(transactions: List[Transaction]): List[Trade] = {
    val sortedTxns = transactions.sortBy(_.timestamp)
    var trades = List[Trade]()
    var openTrade: Option[Trade] = None
    
    sortedTxns.foreach { txn =>
      txn.transactionType match {
        case "BUY" if openTrade.isEmpty =>
          openTrade = Some(Trade(
            entryTime = txn.timestamp,
            exitTime = None,
            entryPrice = txn.amount,
            exitPrice = None,
            size = txn.amount
          ))
          
        case "SELL" if openTrade.isDefined =>
          openTrade.foreach { trade =>
            trades = trades :+ trade.copy(
              exitTime = Some(txn.timestamp),
              exitPrice = Some(txn.amount)
            )
          }
          openTrade = None
          
        case _ => // Ignore other transaction types
      }
    }
    
    trades
  }

  private def isTradeProfit(trade: Trade): Boolean = {
    trade.exitPrice.exists(_ > trade.entryPrice)
  }

  private def calculateProfit(trades: List[Trade]): Double = {
    trades.flatMap { trade =>
      trade.exitPrice.map(_ - trade.entryPrice)
    }.sum
  }

  private def calculateWinRate(trades: List[Trade]): Double = {
    val completedTrades = trades.count(_.exitPrice.isDefined)
    if (completedTrades == 0) 0.0
    else (trades.count(isTradeProfit) * 100.0) / completedTrades
  }

  private def calculateAverageTradeSize(trades: List[Trade]): Double = {
    if (trades.isEmpty) 0.0
    else trades.map(_.size).sum / trades.size
  }

  private def findBestTrade(trades: List[Trade]): Double = {
    trades.flatMap { trade =>
      trade.exitPrice.map(_ - trade.entryPrice)
    }.maxOption.getOrElse(0.0)
  }

  private def findWorstTrade(trades: List[Trade]): Double = {
    trades.flatMap { trade =>
      trade.exitPrice.map(_ - trade.entryPrice)
    }.minOption.getOrElse(0.0)
  }

  private def calculateAverageHoldTime(trades: List[Trade]): Double = {
    val holdTimes = trades.flatMap { trade =>
      trade.exitTime.map(exit => 
        DateUtils.hoursBetween(trade.entryTime, exit)
      )
    }
    
    if (holdTimes.isEmpty) 0.0
    else holdTimes.sum / holdTimes.size
  }
}