package com.analyzer.services

import com.analyzer.models.Transaction
import sttp.client3._
import play.api.libs.json._
import scala.util.{Try, Success, Failure}
import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration._
import java.time.Instant
import scala.collection.mutable.ListBuffer

class SolanaService(implicit ec: ExecutionContext) {
  private val endpoint = "https://api.mainnet-beta.solana.com"
  private val backend = HttpURLConnectionBackend()
  private val batchSize = 100
  private val maxRetries = 3
  private val retryDelay = 2.seconds

  // Main method to get all transactions with pagination
  def getTransactions(walletAddress: String): Try[List[Transaction]] = {
    Try {
      var allTransactions = ListBuffer[Transaction]()
      var beforeSignature: Option[String] = None
      var hasMore = true
      var retryCount = 0

      while (hasMore && retryCount < maxRetries) {
        try {
          val result = getTransactionBatch(walletAddress, beforeSignature)
          result match {
            case Success(batch) =>
              if (batch.isEmpty) {
                hasMore = false
              } else {
                allTransactions ++= batch
                beforeSignature = Some(batch.last.signature)
              }
              retryCount = 0
            case Failure(e) =>
              retryCount += 1
              if (retryCount >= maxRetries) {
                throw new Exception(s"Failed to fetch transactions after $maxRetries retries")
              }
              Thread.sleep(retryDelay.toMillis)
          }
        } catch {
          case e: Exception =>
            retryCount += 1
            if (retryCount >= maxRetries) {
              throw new Exception(s"Failed to fetch transactions after $maxRetries retries: ${e.getMessage}")
            }
            Thread.sleep(retryDelay.toMillis)
        }
      }
      allTransactions.toList
    }
  }

  // Get a batch of transaction signatures
  private def getTransactionBatch(walletAddress: String, beforeSignature: Option[String]): Try[List[Transaction]] = {
    val params = Json.obj(
      "limit" -> batchSize,
      "commitment" -> "confirmed"
    ) ++ beforeSignature.map(sig => Json.obj("beforeSignature" -> sig)).getOrElse(Json.obj())

    val body = Json.obj(
      "jsonrpc" -> "2.0",
      "id" -> 1,
      "method" -> "getSignaturesForAddress",
      "params" -> Json.arr(walletAddress, params)
    )

    for {
      signatures <- makeRpcCall(body)
      transactions <- getTransactionDetails(signatures)
    } yield transactions
  }

  // Get detailed transaction information
  private def getTransactionDetails(signatures: List[String]): Try[List[Transaction]] = {
    Try {
      signatures.flatMap { signature =>
        val body = Json.obj(
          "jsonrpc" -> "2.0",
          "id" -> 1,
          "method" -> "getTransaction",
          "params" -> Json.arr(
            signature,
            Json.obj(
              "encoding" -> "json",
              "commitment" -> "confirmed",
              "maxSupportedTransactionVersion" -> 0
            )
          )
        )

        makeTransactionRpcCall(body) match {
          case Success(txnJson) => Some(parseTransactionDetail(txnJson, signature))
          case Failure(e) => 
            println(s"Warning: Failed to fetch details for transaction $signature: ${e.getMessage}")
            None
        }
      }
    }
  }

  // Make RPC call for signatures
  private def makeRpcCall(body: JsValue): Try[List[String]] = {
    Try {
      val request = basicRequest
        .post(uri"$endpoint")
        .body(Json.stringify(body))
        .header("Content-Type", "application/json")

      val response = request.send(backend)
      
      response.body match {
        case Right(jsonStr) =>
          val json = Json.parse(jsonStr)
          (json \ "result").as[JsArray].value.map { txn =>
            (txn \ "signature").as[String]
          }.toList
        case Left(error) =>
          throw new Exception(s"RPC call failed: $error")
      }
    }
  }

  // Make RPC call for transaction details
  private def makeTransactionRpcCall(body: JsValue): Try[JsValue] = {
    Try {
      val request = basicRequest
        .post(uri"$endpoint")
        .body(Json.stringify(body))
        .header("Content-Type", "application/json")

      val response = request.send(backend)
      
      response.body match {
        case Right(jsonStr) =>
          Json.parse(jsonStr)
        case Left(error) =>
          throw new Exception(s"RPC call failed: $error")
      }
    }
  }

  // Parse detailed transaction information
  private def parseTransactionDetail(json: JsValue, signature: String): Transaction = {
    val result = (json \ "result").as[JsValue]
    val meta = (result \ "meta").as[JsValue]
    val transaction = (result \ "transaction").as[JsValue]
    
    val blockTime = (result \ "blockTime").as[Long]
    val timestamp = Instant.ofEpochSecond(blockTime)
    val preBalances = (meta \ "preBalances").as[List[Long]]
    val postBalances = (meta \ "postBalances").as[List[Long]]
    val fee = (meta \ "fee").as[Long].toDouble / 1e9  // Convert lamports to SOL
    
    // Calculate the net amount (simplified - you might want to add more sophisticated logic)
    val amount = (postBalances.head - preBalances.head).toDouble / 1e9
    
    // Determine transaction type based on instruction data
    val txType = determineTransactionType(transaction, meta)
    
    // Check if there's an error in the transaction
    val success = (meta \ "err").toOption.isEmpty
    
    Transaction(
      signature = signature,
      timestamp = timestamp,
      amount = amount,
      transactionType = txType,
      fee = fee,
      success = success
    )
  }

  // Determine the type of transaction based on program and instruction data
  private def determineTransactionType(transaction: JsValue, meta: JsValue): String = {
    val instructions = (transaction \ "message" \ "instructions").as[JsArray]
    val innerInstructions = (meta \ "innerInstructions").asOpt[JsArray].getOrElse(JsArray())
    
    // This is a simplified version - you might want to add more sophisticated logic
    if (instructions.value.isEmpty) {
      "UNKNOWN"
    } else {
      val programId = (instructions.value.head \ "programId").as[String]
      programId match {
        case "11111111111111111111111111111111" => "TRANSFER" // System Program
        case "DeJBGdMFa1uynnnKiwrVioatTuHmcHoQiv6jGrNnHZU" => "BUY" // Serum v3
        case "9xQeWvG816bUx9EPjHmaT23yvVM2ZWbrrpZb9PusVFin" => "SWAP" // Serum v3
        case _ => "OTHER"
      }
    }
  }
}