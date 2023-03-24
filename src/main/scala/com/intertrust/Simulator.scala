package com.intertrust

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import com.intertrust.behaviours.Alerts
import com.intertrust.parsers.{MovementEventParser, TurbineEventParser}
import com.intertrust.protocol.Alert

import scala.io.Source

object Simulator {

  def apply(): Behavior[Alert] =
    Behaviors.setup { context =>
      val alerts = context.spawn(Alerts("alerts"), "alerts")
      Behaviors.stopped
    }

  def main(args: Array[String]): Unit = {
    val system = ActorSystem(Simulator(), "simulator")

    // Needed to modify those lines, since file name was incorrect and getClass didn't work, so I used Class Loader
    val movementEvents = new MovementEventParser().parseEvents(Source.fromResource("movements.csv"))
    val turbineEvents = new TurbineEventParser().parseEvents(Source.fromResource("turbines.csv"))
    // TODO: Implement events processing that sends alerts to the `alertsActor`
  }
}
