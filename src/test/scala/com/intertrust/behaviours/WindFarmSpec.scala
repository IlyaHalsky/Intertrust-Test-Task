package com.intertrust.behaviours

import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import akka.persistence.testkit.PersistenceTestKitPlugin
import akka.persistence.testkit.scaladsl.PersistenceTestKit
import com.intertrust.protocol.TurbineStatus.{Broken, Working}
import com.intertrust.protocol._
import com.intertrust.test_utils.TestUtils
import com.intertrust.utils.CreateChild
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.Instant
import scala.language.implicitConversions

class WindFarmSpec
  extends ScalaTestWithActorTestKit(PersistenceTestKitPlugin.config.withFallback(ConfigFactory.defaultApplication()))
    with AnyWordSpecLike with Matchers with BeforeAndAfterEach with TestUtils{

  val persistenceTestKit: PersistenceTestKit = PersistenceTestKit(system)

  override def beforeEach(): Unit = {
    persistenceTestKit.clearAll()
    persistenceTestKit.resetPolicy()
  }

  "WindFarm behaviour" should {
    "spawn child" when {
      "on first message TurbineEvent" in {
        val alerts = TestProbe[Alert]()
        val windFarm = spawn(WindFarm(windFarmId, alerts.ref))

        val message = turbineWorking()
        windFarm ! message

        val expectedPersistedEvent = CreateChild(message.turbineId)
        persistenceTestKit.expectNextPersisted(windFarmId, expectedPersistedEvent)
      }
      "on first message WorkerTurbineMove" in {
        val alerts = TestProbe[Alert]()
        val windFarm = spawn(WindFarm(windFarmId, alerts.ref))

        val message = workerMoveTurbine()
        windFarm ! message

        val expectedPersistedEvent = CreateChild(message.turbineId)
        persistenceTestKit.expectNextPersisted(windFarmId, expectedPersistedEvent)
      }
      "not on the second TurbineEvent" in {
        val alerts = TestProbe[Alert]()
        val windFarm = spawn(WindFarm(windFarmId, alerts.ref))

        val message = turbineWorking()
        windFarm ! message
        val expectedPersistedEvent = CreateChild(message.turbineId)
        persistenceTestKit.expectNextPersisted(windFarmId, expectedPersistedEvent)

        val message2 = turbineBroke(1)
        windFarm ! message2
        persistenceTestKit.expectNothingPersisted(windFarmId)
      }
      "not on the second WorkerTurbineMove" in {
        val alerts = TestProbe[Alert]()
        val windFarm = spawn(WindFarm(windFarmId, alerts.ref))

        val message = workerMoveTurbine()
        windFarm ! message
        val expectedPersistedEvent = CreateChild(message.turbineId)
        persistenceTestKit.expectNextPersisted(windFarmId, expectedPersistedEvent)

        val message2 = workerMoveTurbine(1)
        windFarm ! message2
        persistenceTestKit.expectNothingPersisted(windFarmId)
      }
    }
    "forward events" when {
      "TurbineAlert" in {
        val alerts = TestProbe[Alert]()
        val personnel = spawn(WindFarm(windFarmId, alerts.ref))

        val message = turbineAlert(0, "test alert")
        personnel ! message
        persistenceTestKit.expectNothingPersisted(windFarmId)
        alerts.expectMessage(message)
      }
    }
  }
}
