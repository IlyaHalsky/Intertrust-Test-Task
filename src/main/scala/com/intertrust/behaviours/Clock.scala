package com.intertrust.behaviours

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import com.intertrust.protocol.TimeEndTypes.TimeEnd
import com.intertrust.protocol.{TimeEnd, TimeTick}

import java.time.Instant
import scala.concurrent.duration._

object Clock {
  sealed trait ClockMessages

  case object Terminate extends ClockMessages

  case object Tick extends ClockMessages

  case class ClockParams(
    startTime: Instant, stopTime: Instant,
    intervalMillis: Long, tickAmountMillis: Long,
    sendTo: List[ActorRef[TimeTick]],
    manager: ActorRef[TimeEnd],
    context: ActorContext[ClockMessages],
    timers: TimerScheduler[ClockMessages]
  )

  def apply(
    startTime: Instant, stopTime: Instant,
    intervalMillis: Long, tickAmountMillis: Long,
    sendTo: List[ActorRef[TimeTick]],
    manager: ActorRef[TimeEnd],
  ): Behavior[ClockMessages] =
    Behaviors.setup { context =>
      context.setLoggerName("clock")
      Behaviors.withTimers { timers =>
        timers.startTimerAtFixedRate("tick", Tick, intervalMillis.millis)
        receiveTicks(0, ClockParams(startTime, stopTime, intervalMillis, tickAmountMillis, sendTo, manager, context, timers))
      }
    }

  private def receiveTicks(ticksReceived: Long, params: ClockParams): Behavior[ClockMessages] = {
    import params._
    Behaviors.receiveMessage {
      case Tick =>
        val newTime = startTime.plusMillis(tickAmountMillis * ticksReceived)
        context.log.debug(f"Current simulation time {}", newTime)
        sendTo.foreach(_ ! TimeTick(newTime))
        if (newTime.compareTo(stopTime) >= 0) {
          timers.cancelAll()
          manager ! TimeEnd
          context.self ! Terminate
          context.log.info("Terminating time at {} simulation time", newTime)
          terminating()
        } else receiveTicks(ticksReceived + 1, params)
      case Terminate => Behaviors.same
    }
  }

  private def terminating(): Behavior[ClockMessages] =
    Behaviors.receiveMessage {
      case Tick => Behaviors.same
      case Terminate => Behaviors.stopped
    }
}