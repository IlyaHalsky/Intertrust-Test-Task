package com.intertrust.utils

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.persistence.typed.{PersistenceId, RecoveryCompleted}
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import com.intertrust.{PersistableEvent, PersistableState}

object SnapshottingBehaviour {
  def apply[Command, Event <: PersistableEvent, State <: PersistableState](
    persistenceId: String,
    emptyState: State,
    commandHandler: (State, Command) => Effect[Event, State],
    eventHandler: ActorContext[Command] => (State, Event) => State,
    onRecover: ActorContext[Command] => State => Unit = (_: ActorContext[Command]) => (_: State) => (),
  ): Behavior[Command] = Behaviors.setup { context =>
    context.setLoggerName(persistenceId)
    EventSourcedBehavior[Command, Event, State](
      persistenceId = PersistenceId.ofUniqueId(persistenceId),
      emptyState = emptyState,
      commandHandler = commandHandler,
      eventHandler = eventHandler(context),
    )
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 10))
      .receiveSignal {
        case (state, RecoveryCompleted) => onRecover(context)(state)
      }
  }
}
