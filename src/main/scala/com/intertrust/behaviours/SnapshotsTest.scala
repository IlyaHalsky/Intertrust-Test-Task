package com.intertrust.behaviours

import akka.actor.typed.Behavior
import akka.persistence.typed.scaladsl.Effect
import com.intertrust.utils.SnapshottingBehaviour
import com.intertrust.{PersistableEvent, PersistableState}

object SnapshotsTest {
  case class Hi(name: String)

  case class Hello(name: String) extends PersistableEvent

  case class Helloooooooo(name: String) extends PersistableState

  def apply(name: String): Behavior[Hi] =
    SnapshottingBehaviour[Hi, Hello, Helloooooooo](
      name,
      Helloooooooo(""),
      (state, command) => Effect.persist(Hello(command.name)),
      context => (state, event) => {
        context.log.info(state.toString)
        state.copy(name = state.name + event.toString)
      },
      context => state => context.log.info("Recovered with {}", state.toString)
    )
}
