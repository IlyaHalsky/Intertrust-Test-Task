package com.intertrust

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import com.intertrust.behaviours.{Alerts, Clock, Personnel, WindFarm}
import com.intertrust.parsers.{MovementEventParser, TurbineEventParser}
import com.intertrust.protocol.TimeEndTypes.TimeEnd
import com.intertrust.utils.PseudoKafkaConsumer

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}
import scala.io.Source

object Simulator {
  /**
    * Simulator settings:
    * With settings "22.11.2015 23:59:59", 5, 60 times would be following:
    * Timestamp, Simulation Time
    * 0, 22.11.2015 23:59:59
    * 5, 23.11.2015 00:00:59
    * 10, 24.11.2015 00:01:59
    * ...
    * */
  // Simulator start time, end time, in case of recovery test, change to desired stop time (in log time)
  val startTime: String = "22.11.2015 23:59:59"
  val stopTime: String = "29.11.2015 23:59:59"
  // Simulator changes time every n milliseconds
  val tickEveryMillisecond: Int = 10
  // Simulator changes time for m seconds, based on logs set it to an odd number below 60 seconds
  val tickForSeconds: Int = 29


  private lazy val timestampFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").withZone(ZoneId.of("UTC"))
  val startTimeInstant: Instant = Instant.from(timestampFormat.parse(startTime))
  val stopTimeInstant: Instant = Instant.from(timestampFormat.parse(stopTime))

  def create(): Behavior[TimeEnd] =
    Behaviors.setup { context =>
      context.setLoggerName("simulator")
      context.log.info("Starting simulation, estimated run time from {} to {}", Instant.now(), Instant.now().plusMillis((stopTimeInstant.toEpochMilli - startTimeInstant.toEpochMilli) / (tickForSeconds * 1000) * tickEveryMillisecond))
      val alerts = context.spawn(Alerts("alerts"), "alerts")

      val personnel = context.spawn(Personnel("personnel", alerts), "personnel")
      val windFarm = context.spawn(WindFarm("wind-farm", alerts), "wind-farm")

      val movementEvents = new MovementEventParser().parseEvents(Source.fromResource("movements.csv"))
      val movementConsumer = context.spawn(PseudoKafkaConsumer("movement-consumer", movementEvents, personnel, windFarm), "movement-consumer")

      val turbineEvents = new TurbineEventParser().parseEvents(Source.fromResource("turbines.csv"))
      val turbineConsumer = context.spawn(PseudoKafkaConsumer("turbine-consumer", turbineEvents, windFarm), "turbine-consumer")

      val clock = context.spawn(Clock(startTimeInstant, stopTimeInstant, tickEveryMillisecond, tickForSeconds * 1000, movementConsumer :: turbineConsumer :: windFarm :: Nil, context.self), "clock")
      Behaviors.receiveMessage { timeEnd =>
        context.log.info("Clock ended simulation, shutting down")
        Behaviors.stopped
      }
    }

  def main(args: Array[String]): Unit = {
    val system = ActorSystem(create(), "simulator")
  }
}
