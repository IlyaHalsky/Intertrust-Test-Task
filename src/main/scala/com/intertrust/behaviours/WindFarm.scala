package com.intertrust.behaviours

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.{ActorRef, Behavior}
import com.intertrust.protocol.{Alert, Event, WindFarmCommand}
import com.intertrust.utils.{CreateChild, ManagerBehaviour, ManagerState}

object WindFarm {
  def apply(actorName: String, alerts: ActorRef[Alert]): Behavior[WindFarmCommand] =
    ManagerBehaviour(WindFarm(actorName, alerts, _))
}

case class WindFarm(actorName: String, alerts: ActorRef[Alert], context: ActorContext[WindFarmCommand])
  extends ManagerBehaviour[WindFarmCommand, Event] {
  def createChild(childName: String): Behavior[Event] = Turbine(childName, context.self)
  def commandToCreateChild(command: WindFarmCommand): Option[CreateChild] = ???
  def handleCommand(state: ManagerState[Event], command: WindFarmCommand): Unit = ???
}