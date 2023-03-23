package com.intertrust.behaviours

import akka.NotUsed
import akka.actor.typed.Behavior
import akka.persistence.typed.scaladsl.Effect
import com.intertrust.PersistableEvent
import com.intertrust.utils.PersistentBehaviour

import java.time.Instant

object Alerts {
  sealed trait Alert extends PersistableEvent

  case class TurbineAlert(timestamp: Instant, turbineId: String, error: String) extends Alert

  case class MovementAlert(timestamp: Instant, engineerId: String, error: String) extends Alert

  def apply(persistenceId: String): Behavior[Alert] =
    PersistentBehaviour[Alert, Alert, NotUsed](
      persistenceId = persistenceId,
      emptyState = NotUsed.getInstance(),
      commandHandler = context => (_, command) => Effect.persist(command).thenRun { _ =>
        command match {
          case turbine: TurbineAlert => context.log.error(turbine.error)
          case movement: MovementAlert => context.log.error(movement.error)
        }
      },
      eventHandler = _ => (state, _) => state
    )
}
