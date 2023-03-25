package com.intertrust.utils

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.{ActorRef, Behavior}
import com.intertrust.protocol.{PersistableEvent, PersistableState}

case class CreateChild(childName: String) extends PersistableEvent

trait ManagerBehaviour[Command, Event <: PersistableEvent, ChildCommand, State <: ManagerState[Command, ChildCommand]]
  extends StatefulPersistentBehaviour[Command, Event, State] {

  def handleCreateChild(state: State, event: CreateChild): State =
    state.spawnChild(event.childName, context).map(state.addChild).getOrElse(state)

  final def onSnapshotRecover(state: State): Unit =
    state.respawnChildren(context)
}

trait ManagerState[Command, ChildCommand] extends PersistableState {
  def children: List[ActorRef[ChildCommand]]
  def createChild(managerRef: ActorRef[Command], childName: String): Behavior[ChildCommand]
  def addChild(childRef: ActorRef[ChildCommand]): this.type

  private lazy val childrenMap: Map[String, ActorRef[ChildCommand]] =
    children.map(ref => ref.path.name -> ref).toMap

  final def spawnChild(childName: String, context: ActorContext[Command]): Option[ActorRef[ChildCommand]] =
    if (!childrenMap.contains(childName)) Some(context.spawn(createChild(context.self, childName), childName))
    else None

  final def respawnChildren(context: ActorContext[Command]): Unit = {
    val childrenToCreate = children.map(_.path)
    val existingChildren = context.children.map(_.path).toSet
    childrenToCreate.filterNot(existingChildren).foreach(childPath => context.spawn(createChild(context.self, childPath.name), childPath.name))
  }

  final def sendToChild(childName: String, message: ChildCommand): Unit =
    childrenMap.get(childName).foreach(_ ! message)
}

object ManagerBehaviour {
  def apply[Command, Event <: PersistableEvent, ChildCommand, State <: ManagerState[Command, ChildCommand]](
    create: ActorContext[Command] => ManagerBehaviour[Command, Event, ChildCommand, State]
  ): Behavior[Command] = PersistentBehaviour(create)
}
