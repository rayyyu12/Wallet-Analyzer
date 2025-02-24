name := "solana-wallet-analyzer"
version := "0.1.0"
scalaVersion := "2.13.12"

libraryDependencies ++= Seq(
  "com.softwaremill.sttp.client3" %% "core" % "3.8.13",
  "com.typesafe.play" %% "play-json" % "2.9.4",
  "org.scalatest" %% "scalatest" % "3.2.15" % Test,
  "ch.qos.logback" % "logback-classic" % "1.4.7"
)