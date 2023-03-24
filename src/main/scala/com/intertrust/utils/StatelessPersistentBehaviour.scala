package com.intertrust.utils

import akka.NotUsed
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.ActorContext
import akka.persistence.typed.scaladsl.{Effect, RetentionCriteria}
import com.intertrust.protocol.PersistableEvent

trait StatelessPersistentBehaviour[Command, Event <: PersistableEvent]
  extends PersistentBehaviour[Command, Event, NotUsed] {
  type StatelessEffect = Effect[Event, NotUsed]
  def commandHandler(command: Command): StatelessEffect
  final def startingState: NotUsed = NotUsed.getInstance()
  final def commandHandler(state: NotUsed, command: Command): Effect[Event, NotUsed] = commandHandler(command)
  final def eventHandler(state: NotUsed, event: Event): NotUsed = state
  final def retentionCriteria: Option[RetentionCriteria] = None
  final def onSnapshotRecover(state: NotUsed): Unit = ()
}

object StatelessPersistentBehaviour {
  def apply[Command, Event <: PersistableEvent](
    create: ActorContext[Command] => StatelessPersistentBehaviour[Command, Event]
  ): Behavior[Command] = PersistentBehaviour(create)
}