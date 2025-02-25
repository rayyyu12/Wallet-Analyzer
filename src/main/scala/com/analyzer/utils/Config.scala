package com.analyzer.utils

import pureconfig._
import pureconfig.generic.auto._

object Config {
  case class SolanaRpcConfig(
    endpoint: String,
    apiKey: String,
    batchSize: Int,
    requestDelay: Int
  )

  case class AppConfig(
    solana: SolanaConfig
  )

  case class SolanaConfig(
    rpc: SolanaRpcConfig
  )

  lazy val config: AppConfig = ConfigSource.default.loadOrThrow[AppConfig]
}