package com.intertrust

import akka.actor.{ActorSystem, Props}
import com.intertrust.actors.AlertsActor
import com.intertrust.parsers.{MovementEventParser, TurbineEventParser}

import scala.io.Source

object Simulator {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("simulator")

    val alertsActor = system.actorOf(Props(classOf[AlertsActor]), "alerts")

    // Needed to modify those lines, since file name was incorrect and getClass didn't work, so I used Class Loader
    val movementEvents = new MovementEventParser().parseEvents(Source.fromResource("movements.csv"))
    val turbineEvents = new TurbineEventParser().parseEvents(Source.fromResource("turbines.csv"))
    // TODO: Implement events processing that sends alerts to the `alertsActor`

    system.terminate()
  }
}
