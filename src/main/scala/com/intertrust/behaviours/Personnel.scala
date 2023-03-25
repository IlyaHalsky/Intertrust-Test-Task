package com.intertrust.behaviours

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.scaladsl.Effect
import com.intertrust.protocol.{MovementEvent, PersistableEvent}
import com.intertrust.utils.{CreateChild, ManagerBehaviour, ManagerState}

object Personnel {
  def apply(actorName: String): Behavior[PersonnelCommand] =
    ManagerBehaviour(Personnel(actorName, _))
}

trait PersonnelCommand

trait PersonnelEvent extends PersistableEvent

case class PersonError(personId: String, error: String) extends PersonnelCommand

case class PersonnelState(children: List[ActorRef[MovementEvent]])
  extends ManagerState[PersonnelCommand, MovementEvent] {
  def addChild(childRef: ActorRef[MovementEvent]): PersonnelState =
    copy(children = childRef :: children)
  def createChild(managerRef: ActorRef[PersonnelCommand], childName: String): Behavior[MovementEvent] =
    Person(childName, managerRef)
}

case class Personnel(actorName: String, context: ActorContext[PersonnelCommand])
  extends ManagerBehaviour[PersonnelCommand, PersonnelEvent, MovementEvent, PersonnelState] {
  def startingState: PersonnelState = PersonnelState(Nil)
  def commandHandler(state: PersonnelState, command: PersonnelCommand): Effect[PersonnelEvent, PersonnelState] = ???
  def eventHandler(state: PersonnelState, event: PersonnelEvent): PersonnelState =
    event match {
      case event: CreateChild => handleCreateChild(state, event)
    }
}

