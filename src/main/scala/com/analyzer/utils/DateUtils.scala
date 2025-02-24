package com.analyzer.utils

import java.time.Instant
import java.time.temporal.ChronoUnit

object DateUtils {
  def hoursBetween(start: Instant, end: Instant): Double = {
    ChronoUnit.MINUTES.between(start, end) / 60.0
  }
}