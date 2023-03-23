package com.intertrust.utils

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import com.intertrust.PersistableEvent

object PersistentBehaviour {
  def apply[Command, Event <: PersistableEvent, State](
    persistenceId: String,
    emptyState: State,
    commandHandler: ActorContext[Command] => (State, Command) => Effect[Event, State],
    eventHandler: ActorContext[Command] => (State, Event) => State,
  ): Behavior[Command] = Behaviors.setup { context =>
    context.setLoggerName(persistenceId)
    EventSourcedBehavior[Command, Event, State](
      persistenceId = PersistenceId.ofUniqueId(persistenceId),
      emptyState = emptyState,
      commandHandler = commandHandler(context),
      eventHandler = eventHandler(context),
    )
  }
}
