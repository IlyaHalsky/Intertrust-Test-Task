package com.intertrust.behaviours

import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import akka.persistence.testkit.PersistenceTestKitPlugin
import akka.persistence.testkit.scaladsl.PersistenceTestKit
import com.intertrust.protocol.Movement.Exit
import com.intertrust.protocol._
import com.intertrust.test_utils.TestUtils
import com.intertrust.utils.CreateChild
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.language.implicitConversions

class PersonnelSpec
  extends ScalaTestWithActorTestKit(PersistenceTestKitPlugin.config.withFallback(ConfigFactory.defaultApplication()))
    with AnyWordSpecLike with Matchers with BeforeAndAfterEach with TestUtils {

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
        val personnel = spawn(Personnel(personnelId, alerts.ref, windFarm.ref))

        val message = moveVessel()
        personnel ! message

        val expectedPersistedEvent = CreateChild(message.engineerId)
        persistenceTestKit.expectNextPersisted(personnelId, expectedPersistedEvent)
      }
      "not on the second" in {
        val alerts = TestProbe[Alert]()
        val windFarm = TestProbe[WindFarmCommand]()
        val personnel = spawn(Personnel(personnelId, alerts.ref, windFarm.ref))

        val message = moveVessel()
        personnel ! message
        val expectedPersistedEvent = CreateChild(message.engineerId)
        persistenceTestKit.expectNextPersisted(personnelId, expectedPersistedEvent)

        val message2 = moveVessel(1, Exit)
        personnel ! message2
        persistenceTestKit.expectNothingPersisted(personnelId)
      }
    }
    "forward events" when {
      "MovementAlert" in {
        val alerts = TestProbe[Alert]()
        val windFarm = TestProbe[WindFarmCommand]()
        val personnel = spawn(Personnel(personnelId, alerts.ref, windFarm.ref))

        val message = movementAlert(0, "test error")
        personnel ! message
        persistenceTestKit.expectNothingPersisted(personnelId)
        alerts.expectMessage(message)
      }
      "WorkerTurbineMove:WorkerEnter" in {
        val alerts = TestProbe[Alert]()
        val windFarm = TestProbe[WindFarmCommand]()
        val personnel = spawn(Personnel(personnelId, alerts.ref, windFarm.ref))

        val message = workerMoveTurbine()
        personnel ! message
        persistenceTestKit.expectNothingPersisted(personnelId)
        windFarm.expectMessage(message)
      }
      "WorkerTurbineMove:WorkerExit" in {
        val alerts = TestProbe[Alert]()
        val windFarm = TestProbe[WindFarmCommand]()
        val personnel = spawn(Personnel(personnelId, alerts.ref, windFarm.ref))

        val message = workerMoveTurbine(enter = Exit)
        personnel ! message
        persistenceTestKit.expectNothingPersisted(personnelId)
        windFarm.expectMessage(message)
      }
    }
  }
}
