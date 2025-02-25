package com.analyzer.services

import com.analyzer.models.{Transaction, Trade, WalletStats}
import com.analyzer.utils.DateUtils
import java.time.Instant
import java.time.format.DateTimeFormatter

class AnalyzerService {
  def analyzeWallet(transactions: List[Transaction]): WalletStats = {
    // Print all transactions for debugging
    println("\nReceived transactions:")
    transactions.foreach(t => 
      println(s"Transaction ${t.signature.take(8)}...: type=${t.transactionType}, amount=${t.amount}, success=${t.success}, time=${DateTimeFormatter.ISO_INSTANT.format(t.timestamp)}")
    )
    
    // Process both successful and unsuccessful transactions for testing
    // This is a temporary change to see what data we have
    val allTransactions = transactions
    
    println(s"\nAnalyzing all ${allTransactions.size} transactions (ignoring success flag for testing)")
    
    val trades = identifyTrades(allTransactions)
    println(s"Identified ${trades.size} trades")
    
    // Print all trades for debugging
    if (trades.nonEmpty) {
      println("\nTrades identified:")
      trades.foreach(t => 
        println(s"Trade: entry=${DateTimeFormatter.ISO_INSTANT.format(t.entryTime)}, " +
                s"exit=${t.exitTime.map(DateTimeFormatter.ISO_INSTANT.format)}, " +
                s"entryPrice=${t.entryPrice}, exitPrice=${t.exitPrice}, size=${t.size}")
      )
    }
    
    val thirtyDaysAgo = Instant.now().minusSeconds(30 * 24 * 60 * 60)
    val recentTrades = trades.filter(_.entryTime.isAfter(thirtyDaysAgo))
    
    println(s"Found ${recentTrades.size} trades in the last 30 days")
    
    // Calculate completed trades (trades with both entry and exit)
    val completedTrades = recentTrades.filter(_.exitPrice.isDefined)
    println(s"Found ${completedTrades.size} completed trades in the last 30 days")
    
    // Print profit calculations for debugging
    val profit = calculateProfit(recentTrades)
    println(s"Calculated profit: $profit SOL")
    
    WalletStats(
      profitLast30Days = profit,
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
    
    println("\nTransaction sequence for trade identification:")
    sortedTxns.foreach { txn =>
      println(s"Processing txn ${txn.signature.take(8)}...: type=${txn.transactionType}, amount=${txn.amount}")
      
      txn.transactionType match {
        case "BUY" | "SWAP" if openTrade.isEmpty =>
          println(s"  Identified as BUY/SWAP, opening new trade")
          openTrade = Some(Trade(
            entryTime = txn.timestamp,
            exitTime = None,
            entryPrice = txn.amount.abs, // Use absolute value to handle negative amounts
            exitPrice = None,
            size = txn.amount.abs
          ))
          
        case "SELL" | "TRANSFER" if openTrade.isDefined =>
          println(s"  Identified as SELL/TRANSFER, closing trade")
          openTrade.foreach { trade =>
            trades = trades :+ trade.copy(
              exitTime = Some(txn.timestamp),
              exitPrice = Some(txn.amount.abs)
            )
          }
          openTrade = None
          
        case "TRADE" =>
          // For generic trades, check if it's positive or negative amount
          if (txn.amount > 0 && openTrade.isEmpty) {
            println(s"  Identified as TRADE with positive amount, treating as buy")
            // Positive amount might indicate a buy
            openTrade = Some(Trade(
              entryTime = txn.timestamp,
              exitTime = None,
              entryPrice = txn.amount,
              exitPrice = None,
              size = txn.amount
            ))
          } else if (txn.amount < 0 && openTrade.isDefined) {
            println(s"  Identified as TRADE with negative amount, treating as sell")
            // Negative amount might indicate a sell
            openTrade.foreach { trade =>
              trades = trades :+ trade.copy(
                exitTime = Some(txn.timestamp),
                exitPrice = Some(txn.amount.abs)
              )
            }
            openTrade = None
          } else {
            println(s"  TRADE transaction doesn't match opening/closing criteria")
          }
          
        case other => 
          println(s"  Ignoring transaction of type $other")
      }
    }
    
    // If we have any remaining open trades, we can include them in the analysis
    if (openTrade.isDefined) {
      println("Note: There is an open trade that hasn't been closed")
    }
    
    trades
  }

  private def isTradeProfit(trade: Trade): Boolean = {
    trade.exitPrice.exists(_ > trade.entryPrice)
  }

  private def calculateProfit(trades: List[Trade]): Double = {
    val profit = trades.flatMap { trade =>
      trade.exitPrice.map(_ - trade.entryPrice)
    }.sum
    
    // Debug profit calculation
    trades.foreach { trade =>
      val profitForTrade = trade.exitPrice.map(_ - trade.entryPrice).getOrElse(0.0)
      println(s"Trade profit: entry=${trade.entryPrice}, exit=${trade.exitPrice.getOrElse(0.0)}, profit=$profitForTrade")
    }
    
    profit
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