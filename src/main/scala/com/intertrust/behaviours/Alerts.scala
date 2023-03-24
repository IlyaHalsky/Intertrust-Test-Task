package com.intertrust.behaviours

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.ActorContext
import akka.persistence.typed.scaladsl.Effect
import com.intertrust.protocol.{Alert, MovementAlert, TurbineAlert}
import com.intertrust.utils.StatelessPersistentBehaviour

object Alerts {
  def apply(actorName: String): Behavior[Alert] =
    StatelessPersistentBehaviour(Alerts(actorName, _))
}

case class Alerts(actorName: String, context: ActorContext[Alert])
  extends StatelessPersistentBehaviour[Alert, Alert] {
  def commandHandler(command: Alert): StatelessEffect =
    Effect.persist(command).thenRun { _ =>
      command match {
        case turbine: TurbineAlert => context.log.error(turbine.error)
        case movement: MovementAlert => context.log.error(movement.error)
      }
    }
}
