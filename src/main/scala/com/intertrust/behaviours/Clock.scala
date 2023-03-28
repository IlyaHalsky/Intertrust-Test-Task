package com.intertrust.behaviours

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.intertrust.protocol.TimeEndTypes.TimeEnd
import com.intertrust.protocol.{TimeEnd, TimeTick}

import java.time.Instant
import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}

object Clock {
  sealed trait ClockMessages

  case object Terminate extends ClockMessages

  case object Tick extends ClockMessages

  def apply(
    startTime: Instant, stopTime: Instant,
    intervalMillis: Long, tickAmountMillis: Long,
    sendTo: List[ActorRef[TimeTick]],
    manager: ActorRef[TimeEnd],
  ): Behavior[ClockMessages] =
    Behaviors.setup { context =>
      val startTimeMillis = System.currentTimeMillis()
      context.setLoggerName("clock")
      Behaviors.withTimers { timers =>
        timers.startTimerAtFixedRate("tick", Tick, FiniteDuration(intervalMillis, MILLISECONDS))
        Behaviors.receiveMessage {
          case Tick =>
            val ticksPassed = (System.currentTimeMillis() - startTimeMillis) / intervalMillis
            val newTime = startTime.plusMillis(tickAmountMillis * ticksPassed)
            context.log.debug(f"Current simulation time {}", newTime)
            sendTo.foreach(_ ! TimeTick(newTime))
            if (newTime.isAfter(stopTime)) {
              timers.cancelAll()
              manager ! TimeEnd
              context.self ! Terminate
              terminating()
            } else Behaviors.same
          case Terminate => Behaviors.same
        }
      }
    }

  private def terminating(): Behavior[ClockMessages] =
    Behaviors.receiveMessage {
      case Tick => Behaviors.same
      case Terminate => Behaviors.stopped
    }
}