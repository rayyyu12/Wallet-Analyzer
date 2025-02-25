name := "solana-wallet-analyzer"
version := "0.1.0"
scalaVersion := "2.13.12"

libraryDependencies ++= Seq(
  "com.softwaremill.sttp.client3" %% "core" % "3.8.13",
  "com.typesafe.play" %% "play-json" % "2.9.4",
  "org.scalatest" %% "scalatest" % "3.2.15" % Test,
  "ch.qos.logback" % "logback-classic" % "1.4.7",
  "com.github.pureconfig" %% "pureconfig" % "0.17.4"  // For config management
)

// Custom settings for RPC configuration
lazy val rpcEndpoint = settingKey[String]("Solana RPC endpoint URL")
lazy val rpcApiKey = settingKey[String]("API key for the RPC service")
lazy val batchSize = settingKey[Int]("Number of transactions to fetch in each batch")
lazy val requestDelay = settingKey[Int]("Delay between API requests in milliseconds")

// Default values (can be overridden via command line with -D flags)
rpcEndpoint := sys.props.getOrElse("rpcEndpoint", "https://api.mainnet-beta.solana.com")
rpcApiKey := sys.props.getOrElse("rpcApiKey", "")
batchSize := sys.props.getOrElse("batchSize", "5").toInt
requestDelay := sys.props.getOrElse("requestDelay", "2000").toInt

// Generate config file from settings
Compile / resourceGenerators += Def.task {
  val file = (Compile / resourceManaged).value / "application.conf"
  IO.write(file, s"""
    |solana {
    |  rpc {
    |    endpoint = "${rpcEndpoint.value}"
    |    api-key = "${rpcApiKey.value}"
    |    batch-size = ${batchSize.value}
    |    request-delay = ${requestDelay.value}
    |  }
    |}
    |""".stripMargin)
  Seq(file)
}.taskValue