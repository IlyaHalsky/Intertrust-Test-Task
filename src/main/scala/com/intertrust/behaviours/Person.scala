package com.intertrust.behaviours

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.scaladsl.{Effect, EffectBuilder}
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
      case (Movement.Enter, Some(lastLocation)) if location == lastLocation =>
        Some(s"Entered location ${location.id} again")
      case (Movement.Exit, Some(lastLocation)) if location != lastLocation =>
        Some(s"Exited ${location.id} without exiting ${lastLocation.id}")
      case (Movement.Exit, None) =>
        Some(s"Exited ${location.id} without entering it first")
      case _ => None
    }
  }

  def generateMovement(movementEvent: MovementEvent): Option[WorkerTurbineMove] = {
    (movementEvent.movement, movementEvent.location, lastLocation) match {
      case (Movement.Exit, Turbine(id), _) => Some(WorkerExit(id, movementEvent.timestamp))
      case (Movement.Enter, t@Turbine(id), prevLocation) if !prevLocation.contains(t) => Some(WorkerEnter(id, movementEvent.timestamp))
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

  private def reportTurbineMove(command: MovementEvent, prevState: PersonState): Unit =
    prevState.generateMovement(command).foreach(manager ! _)

  private def persistCommand(state: PersonState, event: MovementEvent): EffectBuilder[PersonLocationChange, PersonState] =
    event.movement match {
      case Movement.Enter if !state.lastLocation.contains(event.location) => Effect.persist(PersonLocationChange(Some(event.location)))
      case Movement.Exit if state.lastLocation.nonEmpty => Effect.persist(PersonLocationChange(None))
      case _ => Effect.none
    }

  def commandHandler(state: PersonState, command: MovementEvent): Effect[PersonLocationChange, PersonState] =
    persistCommand(state, command)
      .thenRun((_: PersonState) => reportTurbineMove(command, state))
      .thenRun((_: PersonState) => reportError(command.timestamp, state.checkLocationChange(command.location, command.movement)))

  def eventHandler(state: PersonState, event: PersonLocationChange): PersonState =
    state.changeLocation(event.newLocation)

  def onSnapshotRecover(state: PersonState): Unit = ()
}
