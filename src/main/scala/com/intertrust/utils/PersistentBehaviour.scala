package com.intertrust.utils

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import akka.persistence.typed.{PersistenceId, RecoveryCompleted}
import com.intertrust.protocol.PersistableEvent

trait PersistentBehaviour[Command, Event <: PersistableEvent, State] {
  def actorName: String
  def context: ActorContext[Command]
  def startingState: State
  def commandHandler(state: State, command: Command): Effect[Event, State]
  def eventHandler(state: State, event: Event): State
  def retentionCriteria: Option[RetentionCriteria]
  def onSnapshotRecover(state: State): Unit
}

object PersistentBehaviour {
  def apply[Command, Event <: PersistableEvent, State](
    create: ActorContext[Command] => PersistentBehaviour[Command, Event, State]
  ): Behavior[Command] = {
    Behaviors.setup { context =>
      val wrapper = create(context)
      context.setLoggerName(wrapper.actorName)
      EventSourcedBehavior[Command, Event, State](
        persistenceId = PersistenceId.ofUniqueId(wrapper.actorName),
        emptyState = wrapper.startingState,
        commandHandler = wrapper.commandHandler,
        eventHandler = wrapper.eventHandler,
      ).withRetention(wrapper.retentionCriteria.getOrElse(RetentionCriteria.disabled))
        .receiveSignal {
          case (state, RecoveryCompleted) => wrapper.onSnapshotRecover(state)
        }
    }
  }
}

