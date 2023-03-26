package com.intertrust

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import com.intertrust.behaviours.{Alerts, Personnel}
import com.intertrust.parsers.{MovementEventParser, TurbineEventParser}
import com.intertrust.protocol.Alert
import com.intertrust.utils.PseudoKafkaConsumer

import scala.io.Source

object Simulator {

  def create(): Behavior[Alert] =
    Behaviors.setup { context =>
      val alerts = context.spawn(Alerts("alerts"), "alerts")
      val personnel = context.spawn(Personnel("personnel", alerts), "personnel")
      val movementEvents = new MovementEventParser().parseEvents(Source.fromResource("movements.csv"))
      val movementConsumer = context.spawn(PseudoKafkaConsumer("movementConsumer",movementEvents, personnel), "movementConsumer")
      
      val turbineEvents = new TurbineEventParser().parseEvents(Source.fromResource("turbines.csv"))
      Behaviors.stopped
    }

  def main(args: Array[String]): Unit = {
    val system = ActorSystem(create(), "simulator")
  }
}
