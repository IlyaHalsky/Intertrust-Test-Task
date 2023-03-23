package com.intertrust

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.intertrust.behaviours.{Alerts, SnapshotsTest}
import com.intertrust.behaviours.Alerts.{Alert, TurbineAlert}
import com.intertrust.behaviours.SnapshotsTest.Hi
import com.intertrust.parsers.{MovementEventParser, TurbineEventParser}

import java.time.Instant
import scala.io.Source

object Simulator {

  def apply(): Behavior[Alert] =
    Behaviors.setup { context =>
      val alerts = context.spawn(Alerts("alerts"), "alerts")
      val hi = context.spawn(SnapshotsTest("test"), "test")
      alerts ! TurbineAlert(Instant.now(), "123", "test")
      hi ! Hi("test")
      for {i <- 1 to 51} hi ! Hi(s"$i")
      Thread.sleep(1000)
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
