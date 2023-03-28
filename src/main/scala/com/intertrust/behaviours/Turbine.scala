package com.intertrust.behaviours

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.scaladsl.Effect
import com.intertrust.protocol._
import com.intertrust.utils.StatefulPersistentBehaviour

object Turbine {
  def apply(actorName: String, manager: ActorRef[WindFarmCommand]): Behavior[Event] =
    StatefulPersistentBehaviour(Turbine(actorName, manager, _))
}

case class TurbineStatusChange() extends PersistableEvent

case class TurbineState() extends PersistableState {
}

case class Turbine(
  actorName: String,
  manager: ActorRef[WindFarmCommand],
  context: ActorContext[Event],
) extends StatefulPersistentBehaviour[Event, TurbineStatusChange, TurbineState] {
  def startingState: TurbineState = ???
  def commandHandler(state: TurbineState, command: Event): Effect[TurbineStatusChange, TurbineState] = ???
  def eventHandler(state: TurbineState, event: TurbineStatusChange): TurbineState = ???
  def onSnapshotRecover(state: TurbineState): Unit = ???
}
