package com.analyzer

import com.analyzer.services.{AnalyzerService, SolanaService}
import com.analyzer.models.WalletStats
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global

object Main extends App {
  // We'll use only the imported global ExecutionContext
  
  if (args.length != 1) {
    println("Usage: sbt \"run <wallet-address>\"")
    System.exit(1)
  }

  val walletAddress = args(0)
  val solanaService = new SolanaService()
  val analyzerService = new AnalyzerService()

  println(s"Analyzing wallet: $walletAddress")
  
  solanaService.getTransactions(walletAddress) match {
    case Success(transactions) =>
      val stats = analyzerService.analyzeWallet(transactions)
      printStats(stats)
    case Failure(exception) =>
      println(s"Error: ${exception.getMessage}")
      System.exit(1)
  }

  def printStats(stats: WalletStats): Unit = {
    println("\n=== Wallet Analysis ===")
    println(s"Total Profit (30d): ${stats.profitLast30Days} SOL")
    println(s"Win Rate: ${stats.winRate}%")
    println(s"Average Trade Size: ${stats.averageTradeSize} SOL")
    println(s"Number of Trades: ${stats.totalTrades}")
    println(s"Best Trade: ${stats.bestTrade} SOL")
    println(s"Worst Trade: ${stats.worstTrade} SOL")
    println(s"Average Hold Time: ${stats.averageHoldTime} hours")
    println("==================")
  }
}