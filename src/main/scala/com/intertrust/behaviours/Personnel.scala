package com.intertrust.behaviours

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.{ActorRef, Behavior}
import com.intertrust.protocol._
import com.intertrust.utils.{CreateChild, ManagerBehaviour, ManagerState}

object Personnel {
  def apply(actorName: String, alerts: ActorRef[Alert], windFarm: ActorRef[WindFarmCommand]): Behavior[PersonnelCommand] =
    ManagerBehaviour(Personnel(actorName, alerts, windFarm, _))
}

case class Personnel(actorName: String, alerts: ActorRef[Alert], windFarm: ActorRef[WindFarmCommand], context: ActorContext[PersonnelCommand])
  extends ManagerBehaviour[PersonnelCommand, MovementEvent] {
  def createChild(childName: String): Behavior[MovementEvent] = Person(childName, context.self)
  def commandToCreateChild(command: PersonnelCommand): Option[CreateChild] = command match {
    case me: MovementEvent => Some(CreateChild(me.engineerId))
    case _ => None
  }
  def handleCommand(state: ManagerState[MovementEvent], command: PersonnelCommand): Unit = command match {
    case me: MovementEvent => state.sendToChild(me.engineerId, me, context)
    case ma: MovementAlert => alerts ! ma
    case move: WorkerTurbineMove => windFarm ! move
    case _ => ()
  }
}

