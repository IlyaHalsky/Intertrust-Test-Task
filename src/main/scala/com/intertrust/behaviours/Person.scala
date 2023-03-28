package com.intertrust.behaviours

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.scaladsl.Effect
import com.intertrust.protocol._
import com.intertrust.utils.StatefulPersistentBehaviour

import java.time.Instant

object Person {
  def apply(actorName: String, manager: ActorRef[PersonnelCommand]): Behavior[MovementEvent] =
    StatefulPersistentBehaviour(Person(actorName, manager, _))
}

case class PersonLocationChange(newLocation: Option[Location]) extends PersistableEvent

case class PersonState(lastLocation: Option[Location]) extends PersistableState {
  def checkLocationChange(location: Location, movement: Movement): Option[String] = {
    (movement, lastLocation) match {
      case (Movement.Enter, Some(lastLocation)) if location != lastLocation =>
        Some(s"Entered new location ${location.id} without exiting ${lastLocation.id}")
      case (Movement.Exit, None) =>
        Some(s"Exited ${location.id} without entering it")
      case _ => None
    }
  }

  def changeLocation(newLocation: Option[Location]): PersonState =
    PersonState(newLocation)
}

case class Person(
  actorName: String,
  manager: ActorRef[PersonnelCommand],
  context: ActorContext[MovementEvent],
) extends StatefulPersistentBehaviour[MovementEvent, PersonLocationChange, PersonState] {
  def startingState: PersonState = PersonState(None)

  private def reportError(movementTime: Instant, message: Option[String]): Unit =
    message.foreach(manager ! MovementAlert(movementTime, actorName, _))

  private def commandToEvent(event: MovementEvent): PersonLocationChange =
    event.movement match {
      case Movement.Enter => PersonLocationChange(Some(event.location))
      case Movement.Exit => PersonLocationChange(None)
    }

  def commandHandler(state: PersonState, command: MovementEvent): Effect[PersonLocationChange, PersonState] =
    Effect.persist(commandToEvent(command)).thenRun(_ => reportError(command.timestamp, state.checkLocationChange(command.location, command.movement)))

  def eventHandler(state: PersonState, event: PersonLocationChange): PersonState =
    state.changeLocation(event.newLocation)

  def onSnapshotRecover(state: PersonState): Unit = ()
}
