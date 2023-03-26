package com.intertrust.utils

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.scaladsl.{Effect, RetentionCriteria}
import com.intertrust.protocol.PersistableEvent

object ManagerBehaviour {
  def apply[Command, ChildCommand](
    create: ActorContext[Command] => ManagerBehaviour[Command, ChildCommand]
  ): Behavior[Command] = PersistentBehaviour(create)
}

case class CreateChild(childName: String) extends PersistableEvent

trait ManagerBehaviour[Command, ChildCommand]
  extends PersistentBehaviour[Command, CreateChild, ManagerState[ChildCommand]] {
  def createChild(childName: String): Behavior[ChildCommand]
  def commandToCreateChild(command: Command): Option[CreateChild]
  def handleCommand(state: ManagerState[ChildCommand], command: Command): Unit

  final def startingState: ManagerState[ChildCommand] = ManagerState[ChildCommand](Map.empty)(createChild)
  def commandHandler(state: ManagerState[ChildCommand], command: Command): Effect[CreateChild, ManagerState[ChildCommand]] =
    (commandToCreateChild(command) match {
      case Some(createChild) => Effect.persist[CreateChild, ManagerState[ChildCommand]](createChild)
      case None => Effect.none[CreateChild, ManagerState[ChildCommand]]
    }).thenRun(state => handleCommand(state, command))
  final def eventHandler(state: ManagerState[ChildCommand], event: CreateChild): ManagerState[ChildCommand] =
    state.spawnChild(event.childName, context)
  final def retentionCriteria: Option[RetentionCriteria] = None
  final def onSnapshotRecover(state: ManagerState[ChildCommand]): Unit = ()
}

case class ManagerState[ChildCommand](childrenMap: Map[String, ActorRef[ChildCommand]])(createChild: String => Behavior[ChildCommand]) {
  def spawnChild(childName: String, context: ActorContext[_]): ManagerState[ChildCommand] =
    if (childrenMap.contains(childName)) this
    else {
      val child = context.spawn(createChild(childName), childName)
      copy(childrenMap = childrenMap.updated(childName, child))(createChild)
    }
  def sendToChild(childName: String, message: ChildCommand, context: ActorContext[_]) =
    childrenMap.get(childName) match {
      case Some(child) => child ! message
      case None => context.log.error(s"Child $childName doesn't exist to receive $message")
    }
}
