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

sealed trait TurbineStatusChange extends PersistableEvent

case class BrokeEvent(timestamp: Instant) extends TurbineStatusChange

case class RepairedEvent() extends TurbineStatusChange // case class in order to avoid serialization issues

case class WorkerEnterEvent() extends TurbineStatusChange

case class WorkerExitEvent(timestamp: Instant, workerId: String) extends TurbineStatusChange

case class AlertReportedEvent() extends TurbineStatusChange

case class TurbineState(broken: Boolean, hadWorker: Option[String], reportAt: Option[Instant]) extends PersistableState {
  def changeState(event: TurbineStatusChange): TurbineState =
    event match {
      case BrokeEvent(timestamp) => copy(broken = true, reportAt = Some(timestamp.plus(4, ChronoUnit.HOURS)))
      case _: RepairedEvent => copy(broken = false, hadWorker = None, reportAt = None)
      case _: WorkerEnterEvent => copy(reportAt = None)
      case WorkerExitEvent(timestamp, workerId) if broken => copy(hadWorker = Some(workerId), reportAt = Some(timestamp.plus(3, ChronoUnit.MINUTES)))
      case _: AlertReportedEvent => copy(reportAt = None)
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
      case TurbineEvent(_, Working, _, _) if state.broken => Effect.persist(RepairedEvent())
      case TurbineEvent(_, Broken, _, timestamp) if !state.broken => Effect.persist(BrokeEvent(timestamp))
        .thenRun((_: TurbineState) => manager ! TurbineAlert(timestamp, actorName, "Turbine broke"))
      case _: WorkerEnterTurbine if state.broken => Effect.persist(WorkerEnterEvent())
      case exit: WorkerExitTurbine if state.broken => Effect.persist(WorkerExitEvent(exit.timestamp, exit.personId))
      case TimeTick(time) => state.generateAlert(time) match {
        case Some(message) => Effect.persist(AlertReportedEvent())
          .thenRun((_: TurbineState) => manager ! TurbineAlert(time, actorName, message))
        case None => Effect.none
      }
      case _ => Effect.none
    }
  def eventHandler(state: TurbineState, event: TurbineStatusChange): TurbineState =
    state.changeState(event)
  def onSnapshotRecover(state: TurbineState): Unit = ()
}
