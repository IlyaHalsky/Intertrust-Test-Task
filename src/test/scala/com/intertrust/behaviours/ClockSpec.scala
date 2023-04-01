package com.intertrust.behaviours

import akka.actor.testkit.typed.FishingOutcome.{Complete, Continue}
import akka.actor.testkit.typed.scaladsl.ManualTime

import scala.concurrent.duration._
import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import com.intertrust.protocol.TimeEndTypes.TimeEnd
import com.intertrust.protocol.{TimeEnd, TimeTick}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.Instant

class ClockSpec extends ScalaTestWithActorTestKit(ManualTime.config) with AnyWordSpecLike with Matchers {
  val manualTime: ManualTime = ManualTime()
  "Clock behaviour" should {
    "produce TimeTick commands for period 2022-01-20T00:00:00.00Z to 2022-01-21T00:00:00.00Z" when {
      val startTime = Instant.parse("2022-01-20T00:00:00.00Z")
      val endTime = Instant.parse("2022-01-21T00:00:00.00Z")
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
