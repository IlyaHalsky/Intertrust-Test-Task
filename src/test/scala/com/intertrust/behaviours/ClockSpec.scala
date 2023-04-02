package com.intertrust.behaviours

import akka.actor.testkit.typed.scaladsl.{ManualTime, ScalaTestWithActorTestKit, TestProbe}
import com.intertrust.protocol.TimeEndTypes.TimeEnd
import com.intertrust.protocol.{TimeEnd, TimeTick}
import com.intertrust.test_utils._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.temporal.ChronoUnit
import scala.concurrent.duration._

class ClockSpec extends ScalaTestWithActorTestKit(ManualTime.config) with AnyWordSpecLike with Matchers with TestUtils {
  val manualTime: ManualTime = ManualTime()
  "Clock behaviour" should {
    "produce TimeTick commands for period of 24 hours" when {
      val startTime = testStart.truncatedTo(ChronoUnit.DAYS)
      val endTime = startTime + 24.hours
      "tick rate 10, interval 1 hour will produce 25 ticks" in {
        val hourMillis = 1.hour.toMillis
        val tickProbe = TestProbe[TimeTick]()
        val endProbe = TestProbe[TimeEnd]()
        val clockBehaviour = Clock(
          startTime = startTime,
          stopTime = endTime,
          intervalMillis = 10,
          tickAmountMillis = hourMillis,
          sendTo = tickProbe.ref :: Nil,
          manager = endProbe.ref
        )

        spawn(clockBehaviour)
        for {
          i <- 0 until 25
        } {
          manualTime.timePasses(10.millis)
          tickProbe.expectMessage(TimeTick(startTime.plusMillis(hourMillis * i)))
        }
        manualTime.timePasses(10.millis)
        endProbe.expectMessage(TimeEnd)
      }
    }
  }
}
