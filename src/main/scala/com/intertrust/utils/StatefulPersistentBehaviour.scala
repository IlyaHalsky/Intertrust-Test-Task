package com.intertrust.utils

import akka.NotUsed
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.ActorContext
import akka.persistence.typed.scaladsl.RetentionCriteria
import com.intertrust.protocol.{PersistableEvent, PersistableState}

trait StatefulPersistentBehaviour[Command, Event <: PersistableEvent, State <: PersistableState]
  extends PersistentBehaviour[Command, Event, State] {
  def numberOfEvents: Int = 50
  def keepNSnapshots: Int = 3
  final def retentionCriteria: Option[RetentionCriteria] = Some(RetentionCriteria.snapshotEvery(numberOfEvents, keepNSnapshots))
}

object StatefulPersistentBehaviour {
  def apply[Command, Event <: PersistableEvent, State <: PersistableState](
    create: ActorContext[Command] => StatefulPersistentBehaviour[Command, Event, State]
  ): Behavior[Command] = PersistentBehaviour(create)
}