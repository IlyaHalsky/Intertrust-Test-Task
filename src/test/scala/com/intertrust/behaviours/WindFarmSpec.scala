package com.intertrust.behaviours

import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import akka.persistence.testkit.PersistenceTestKitPlugin
import akka.persistence.testkit.scaladsl.PersistenceTestKit
import com.intertrust.protocol.TurbineStatus.{Broken, Working}
import com.intertrust.protocol._
import com.intertrust.utils.CreateChild
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.Instant
import scala.language.implicitConversions

class WindFarmSpec
  extends ScalaTestWithActorTestKit(PersistenceTestKitPlugin.config.withFallback(ConfigFactory.defaultApplication()))
    with AnyWordSpecLike with Matchers with BeforeAndAfterEach {

  val persistenceTestKit: PersistenceTestKit = PersistenceTestKit(system)

  override def beforeEach(): Unit = {
    persistenceTestKit.clearAll()
    persistenceTestKit.resetPolicy()
  }

  "WindFarm behaviour" should {
    "spawn child" when {
      "on first message TurbineEvent" in {
        val alerts = TestProbe[Alert]()
        val windFarm = spawn(WindFarm("test-wind-farm", alerts.ref))

        val message = TurbineEvent("test-turbine", Working, 1.0, Instant.ofEpochMilli(0))
        windFarm ! message

        val expectedPersistedEvent = CreateChild(message.turbineId)
        persistenceTestKit.expectNextPersisted("test-wind-farm", expectedPersistedEvent)
      }
      "on first message WorkerTurbineMove" in {
        val alerts = TestProbe[Alert]()
        val windFarm = spawn(WindFarm("test-wind-farm", alerts.ref))

        val message = WorkerEnter( "test-turbine", Instant.ofEpochMilli(0))
        windFarm ! message

        val expectedPersistedEvent = CreateChild(message.turbineId)
        persistenceTestKit.expectNextPersisted("test-wind-farm", expectedPersistedEvent)
      }
      "not on the second TurbineEvent" in {
        val alerts = TestProbe[Alert]()
        val windFarm = spawn(WindFarm("test-wind-farm", alerts.ref))

        val message = TurbineEvent("test-turbine", Working, 1.0, Instant.ofEpochMilli(0))
        windFarm ! message
        val expectedPersistedEvent = CreateChild(message.turbineId)
        persistenceTestKit.expectNextPersisted("test-wind-farm", expectedPersistedEvent)

        val message2 = TurbineEvent("test-turbine", Broken, 1.0, Instant.ofEpochMilli(1))
        windFarm ! message2
        persistenceTestKit.expectNothingPersisted("test-wind-farm")
      }
      "not on the second WorkerTurbineMove" in {
        val alerts = TestProbe[Alert]()
        val windFarm = spawn(WindFarm("test-wind-farm", alerts.ref))

        val message = WorkerEnter( "test-turbine", Instant.ofEpochMilli(0))
        windFarm ! message
        val expectedPersistedEvent = CreateChild(message.turbineId)
        persistenceTestKit.expectNextPersisted("test-wind-farm", expectedPersistedEvent)

        val message2 = WorkerEnter( "test-turbine", Instant.ofEpochMilli(1))
        windFarm ! message2
        persistenceTestKit.expectNothingPersisted("test-wind-farm")
      }
    }
    "forward events" when {
      "TurbineAlert" in {
        val alerts = TestProbe[Alert]()
        val personnel = spawn(WindFarm("test-wind-farm", alerts.ref))

        val message = TurbineAlert(Instant.ofEpochMilli(0), "test-turbine", "test error")
        personnel ! message
        persistenceTestKit.expectNothingPersisted("test-wind-farm")
        alerts.expectMessage(message)
      }
    }
  }
}
