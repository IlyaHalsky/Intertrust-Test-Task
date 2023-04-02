package com.intertrust

import java.time.Instant
import scala.concurrent.duration.FiniteDuration

package object test_utils {
  implicit class RichInstant(val instant: Instant) extends AnyVal {
    def +(duration: FiniteDuration): Instant = instant.plusMillis(duration.toMillis)
    def +(millis: Long): Instant = instant.plusMillis(millis)
  }
}
