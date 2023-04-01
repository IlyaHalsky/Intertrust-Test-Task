package com.intertrust.behaviours

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.{ActorRef, Behavior}
import com.intertrust.protocol.{Alert, TimeTick, TurbineAlert, TurbineCommand, TurbineEvent, WindFarmCommand, WorkerTurbineMove}
import com.intertrust.utils.{CreateChild, ManagerBehaviour, ManagerState}

object WindFarm {
  def apply(actorName: String, alerts: ActorRef[Alert]): Behavior[WindFarmCommand] =
    ManagerBehaviour(WindFarm(actorName, alerts, _))
}

case class WindFarm(actorName: String, alerts: ActorRef[Alert], context: ActorContext[WindFarmCommand])
  extends ManagerBehaviour[WindFarmCommand, TurbineCommand] {
  def createChild(childName: String): Behavior[TurbineCommand] = Turbine(childName, context.self)
  def commandToCreateChild(command: WindFarmCommand): Option[CreateChild] =
    command match {
      case te: TurbineEvent => Some(CreateChild(te.turbineId))
      case move: WorkerTurbineMove => Some(CreateChild(move.turbineId))
      case _ => None
    }
  def handleCommand(state: ManagerState[TurbineCommand], command: WindFarmCommand): Unit =
    command match {
      case te: TurbineEvent => state.sendToChild(te.turbineId, te, context)
      case move: WorkerTurbineMove => state.sendToChild(move.turbineId, move, context)
      case ta: TurbineAlert => alerts ! ta
      case tt: TimeTick => state.broadcastToChildren(tt)
    }
}