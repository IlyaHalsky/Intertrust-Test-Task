package com.intertrust.behaviours

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.scaladsl.Effect
import com.intertrust.protocol.TurbineStatus.{Broken, Working}
import com.intertrust.protocol._
import com.intertrust.utils.StatefulPersistentBehaviour

import java.time.Instant
import java.time.temporal.ChronoUnit

object Turbine {
  def apply(actorName: String, manager: ActorRef[WindFarmCommand]): Behavior[TurbineCommand] =
    StatefulPersistentBehaviour(Turbine(actorName, manager, _))
}

trait TurbineStatusChange extends PersistableEvent

case class Broke(timestamp: Instant) extends TurbineStatusChange

case class Fixed() extends TurbineStatusChange // case class in order to avoid serialization issues

case class WorkerEnter() extends TurbineStatusChange

case class WorkerExit(timestamp: Instant, workerId: String) extends TurbineStatusChange

case class Reported() extends TurbineStatusChange

case class TurbineState(broken: Boolean, hadWorker: Option[String], reportAt: Option[Instant]) extends PersistableState {
  def changeState(event: TurbineStatusChange): TurbineState =
    event match {
      case Broke(timestamp) => copy(broken = true, reportAt = Some(timestamp.plus(4, ChronoUnit.HOURS)))
      case _: Fixed => copy(broken = false, hadWorker = None, reportAt = None)
      case _: WorkerEnter => copy(reportAt = None)
      case WorkerExit(timestamp, workerId) if broken => copy(hadWorker = Some(workerId), reportAt = Some(timestamp.plus(3, ChronoUnit.MINUTES)))
      case _: Reported => copy(reportAt = None)
      case _ => this
    }

  def generateAlert(time: Instant): Option[String] =
    if (reportAt.exists(reportAt => time.isAfter(reportAt))) {
      hadWorker match {
        case Some(workerId) => Some(s"Turbine is still broken, worker $workerId unable to help")
        case None => Some(s"Turbine is still broken after 4 hours")
      }

    } else None
}

case class Turbine(
  actorName: String,
  manager: ActorRef[WindFarmCommand],
  context: ActorContext[TurbineCommand],
) extends StatefulPersistentBehaviour[TurbineCommand, TurbineStatusChange, TurbineState] {
  def startingState: TurbineState = TurbineState(broken = false, hadWorker = None, None)
  def commandHandler(state: TurbineState, command: TurbineCommand): Effect[TurbineStatusChange, TurbineState] =
    command match {
      case TurbineEvent(_, Working, _, _) if state.broken => Effect.persist(Fixed())
      case TurbineEvent(_, Broken, _, timestamp) if !state.broken => Effect.persist(Broke(timestamp))
        .thenRun((_: TurbineState) => manager ! TurbineAlert(timestamp, actorName, "Turbine broke"))
      case _: WorkerEnter if state.broken => Effect.persist(WorkerEnter())
      case exit: WorkerExit if state.broken => Effect.persist(WorkerExit(exit.timestamp, exit.workerId))
      case TimeTick(time) => state.generateAlert(time) match {
        case Some(message) => Effect.persist(Reported())
          .thenRun((_: TurbineState) => manager ! TurbineAlert(time, actorName, message))
        case None => Effect.none
      }
      case _ => Effect.none
    }
  def eventHandler(state: TurbineState, event: TurbineStatusChange): TurbineState =
    state.changeState(event)
  def onSnapshotRecover(state: TurbineState): Unit = ()
}
