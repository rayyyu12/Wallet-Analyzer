package com.analyzer.models

import java.time.Instant

case class Trade(
  entryTime: Instant,
  exitTime: Option[Instant],
  entryPrice: Double,
  exitPrice: Option[Double],
  size: Double
)