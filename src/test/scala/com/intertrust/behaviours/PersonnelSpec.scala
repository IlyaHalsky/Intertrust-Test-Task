package com.intertrust.behaviours

import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import akka.persistence.testkit.PersistenceTestKitPlugin
import akka.persistence.testkit.scaladsl.PersistenceTestKit
import com.intertrust.protocol.Movement.{Enter, Exit}
import com.intertrust.protocol._
import com.intertrust.utils.CreateChild
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.Instant
import scala.language.implicitConversions

class PersonnelSpec
  extends ScalaTestWithActorTestKit(PersistenceTestKitPlugin.config.withFallback(ConfigFactory.defaultApplication()))
    with AnyWordSpecLike with Matchers with BeforeAndAfterEach {

  val persistenceTestKit: PersistenceTestKit = PersistenceTestKit(system)

  override def beforeEach(): Unit = {
    persistenceTestKit.clearAll()
    persistenceTestKit.resetPolicy()
  }

  "Personnel behaviour" should {
    "spawn child" when {
      "on first message MovementEvent" in {
        val alerts = TestProbe[Alert]()
        val windFarm = TestProbe[WindFarmCommand]()
        val personnel = spawn(Personnel("test-personnel", alerts.ref, windFarm.ref))

        val message = MovementEvent("test-engineer", Vessel("123"), Enter, Instant.ofEpochMilli(0))
        personnel ! message

        val expectedPersistedEvent = CreateChild(message.engineerId)
        persistenceTestKit.expectNextPersisted("test-personnel", expectedPersistedEvent)
      }
      "not on the second" in {
        val alerts = TestProbe[Alert]()
        val windFarm = TestProbe[WindFarmCommand]()
        val personnel = spawn(Personnel("test-personnel", alerts.ref, windFarm.ref))

        val message = MovementEvent("test-engineer", Vessel("123"), Enter, Instant.ofEpochMilli(0))
        personnel ! message
        val expectedPersistedEvent = CreateChild(message.engineerId)
        persistenceTestKit.expectNextPersisted("test-personnel", expectedPersistedEvent)

        val message2 = MovementEvent("test-engineer", Vessel("123"), Exit, Instant.ofEpochMilli(1))
        personnel ! message2
        persistenceTestKit.expectNothingPersisted("test-personnel")
      }
    }
    "forward events" when {
      "MovementAlert" in {
        val alerts = TestProbe[Alert]()
        val windFarm = TestProbe[WindFarmCommand]()
        val personnel = spawn(Personnel("test-personnel", alerts.ref, windFarm.ref))

        val message = MovementAlert(Instant.ofEpochMilli(0), "test-engineer", "test error")
        personnel ! message
        persistenceTestKit.expectNothingPersisted("test-personnel")
        alerts.expectMessage(message)
      }
      "WorkerTurbineMove:WorkerEnter" in {
        val alerts = TestProbe[Alert]()
        val windFarm = TestProbe[WindFarmCommand]()
        val personnel = spawn(Personnel("test-personnel", alerts.ref, windFarm.ref))

        val message = WorkerEnter("test-turbine", Instant.ofEpochMilli(0))
        personnel ! message
        persistenceTestKit.expectNothingPersisted("test-personnel")
        windFarm.expectMessage(message)
      }
      "WorkerTurbineMove:WorkerExit" in {
        val alerts = TestProbe[Alert]()
        val windFarm = TestProbe[WindFarmCommand]()
        val personnel = spawn(Personnel("test-personnel", alerts.ref, windFarm.ref))

        val message = WorkerExit("test-turbine", Instant.ofEpochMilli(0))
        personnel ! message
        persistenceTestKit.expectNothingPersisted("test-personnel")
        windFarm.expectMessage(message)
      }
    }
  }
}
