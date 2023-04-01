package com.intertrust.behaviours

import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import akka.persistence.testkit.PersistenceTestKitPlugin
import akka.persistence.testkit.scaladsl.PersistenceTestKit
import com.intertrust.protocol.Movement.{Enter, Exit}
import com.intertrust.protocol.TurbineStatus.{Broken, Working}
import com.intertrust.protocol.{TimeTick, TurbineAlert, TurbineEvent, WindFarmCommand, WorkerEnterTurbine, WorkerExitTurbine}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._
import java.time.Instant
import scala.language.implicitConversions

class TurbineSpec
  extends ScalaTestWithActorTestKit(PersistenceTestKitPlugin.config.withFallback(ConfigFactory.defaultApplication()))
    with AnyWordSpecLike with Matchers with BeforeAndAfterEach {

  val persistenceTestKit: PersistenceTestKit = PersistenceTestKit(system)

  override def beforeEach(): Unit = {
    persistenceTestKit.clearAll()
    persistenceTestKit.resetPolicy()
  }

  "Turbine behaviour" should {
    "persist" when {
      "broke twice" in {
        val parent = TestProbe[WindFarmCommand]()
        val person = spawn(Turbine("test-turbine", parent.ref))

        val message = TurbineEvent("test-turbine", Broken, 1.0, Instant.ofEpochMilli(0))
        person ! message
        val expectedPersistedEvent = Broke(Instant.ofEpochMilli(0))
        persistenceTestKit.expectNextPersisted("test-turbine", expectedPersistedEvent)

        val message2 = TurbineEvent("test-turbine", Broken, 1.0, Instant.ofEpochMilli(1))
        person ! message2
        persistenceTestKit.expectNothingPersisted("test-turbine")
      }
      "broke then fixed" in {
        val parent = TestProbe[WindFarmCommand]()
        val person = spawn(Turbine("test-turbine", parent.ref))

        val message = TurbineEvent("test-turbine", Broken, 1.0, Instant.ofEpochMilli(0))
        person ! message
        val expectedPersistedEvent = Broke(Instant.ofEpochMilli(0))
        persistenceTestKit.expectNextPersisted("test-turbine", expectedPersistedEvent)

        val message2 = TurbineEvent("test-turbine", Working, 1.0, Instant.ofEpochMilli(1))
        person ! message2
        val expectedPersistedEvent2 = Fixed()
        persistenceTestKit.expectNextPersisted("test-turbine", expectedPersistedEvent2)
      }
      "working then worker enter" in {
        val parent = TestProbe[WindFarmCommand]()
        val person = spawn(Turbine("test-turbine", parent.ref))

        val message = WorkerEnterTurbine("test-engineer", "test-turbine", Instant.ofEpochMilli(0))
        person ! message
        persistenceTestKit.expectNothingPersisted("test-turbine")
      }
      "broke then worker enter" in {
        val parent = TestProbe[WindFarmCommand]()
        val person = spawn(Turbine("test-turbine", parent.ref))

        val message = TurbineEvent("test-turbine", Broken, 1.0, Instant.ofEpochMilli(0))
        person ! message
        val expectedPersistedEvent = Broke(Instant.ofEpochMilli(0))
        persistenceTestKit.expectNextPersisted("test-turbine", expectedPersistedEvent)

        val message2 = WorkerEnterTurbine("test-engineer", "test-turbine", Instant.ofEpochMilli(1))
        person ! message2
        val expectedPersistedEvent2 = WorkerEnter()
        persistenceTestKit.expectNextPersisted("test-turbine", expectedPersistedEvent2)
      }
      "working then worker exit" in {
        val parent = TestProbe[WindFarmCommand]()
        val person = spawn(Turbine("test-turbine", parent.ref))

        val message = WorkerExitTurbine("test-engineer", "test-turbine", Instant.ofEpochMilli(0))
        person ! message
        persistenceTestKit.expectNothingPersisted("test-turbine")
      }
      "broke then worker exit" in {
        val parent = TestProbe[WindFarmCommand]()
        val person = spawn(Turbine("test-turbine", parent.ref))

        val message = TurbineEvent("test-turbine", Broken, 1.0, Instant.ofEpochMilli(0))
        person ! message
        val expectedPersistedEvent = Broke(Instant.ofEpochMilli(0))
        persistenceTestKit.expectNextPersisted("test-turbine", expectedPersistedEvent)

        val message2 = WorkerExitTurbine("test-engineer", "test-turbine", Instant.ofEpochMilli(1))
        person ! message2
        val expectedPersistedEvent2 = WorkerExit(Instant.ofEpochMilli(1), "test-engineer")
        persistenceTestKit.expectNextPersisted("test-turbine", expectedPersistedEvent2)
      }
      "broke then alerted" in {
        val parent = TestProbe[WindFarmCommand]()
        val person = spawn(Turbine("test-turbine", parent.ref))

        val message = TurbineEvent("test-turbine", Broken, 1.0, Instant.ofEpochMilli(0))
        person ! message
        val expectedPersistedEvent = Broke(Instant.ofEpochMilli(0))
        persistenceTestKit.expectNextPersisted("test-turbine", expectedPersistedEvent)

        val message2 = TimeTick(Instant.ofEpochMilli(4.hours.toMillis + 1))
        person ! message2
        val expectedPersistedEvent2 = Reported()
        persistenceTestKit.expectNextPersisted("test-turbine", expectedPersistedEvent2)
      }
    }
    "not send alert" when {
      "broke then fixed" in {
        val parent = TestProbe[WindFarmCommand]()
        val person = spawn(Turbine("test-turbine", parent.ref))

        val message = TurbineEvent("test-turbine", Broken, 1.0, Instant.ofEpochMilli(0))
        person ! message
        parent.expectMessageType[TurbineAlert]

        val message2 = TimeTick(Instant.ofEpochMilli(3.hours.toMillis))
        person ! message2
        parent.expectNoMessage()

        val message3 = TimeTick(Instant.ofEpochMilli(4.hours.toMillis))
        person ! message3
        parent.expectNoMessage()
      }
      "broke then after second report" in {
        val parent = TestProbe[WindFarmCommand]()
        val person = spawn(Turbine("test-turbine", parent.ref))

        val message = TurbineEvent("test-turbine", Broken, 1.0, Instant.ofEpochMilli(0))
        person ! message
        parent.expectMessageType[TurbineAlert]

        val message2 = TimeTick(Instant.ofEpochMilli(4.hours.toMillis + 1))
        person ! message2
        parent.expectMessageType[TurbineAlert]

        val message3 = TimeTick(Instant.ofEpochMilli(5.hours.toMillis))
        person ! message3
        parent.expectNoMessage()
      }
      "broke then worker enter" in {
        val parent = TestProbe[WindFarmCommand]()
        val person = spawn(Turbine("test-turbine", parent.ref))

        val message = TurbineEvent("test-turbine", Broken, 1.0, Instant.ofEpochMilli(0))
        person ! message
        parent.expectMessageType[TurbineAlert]

        val message2 = WorkerEnterTurbine("test-engineer", "test-turbine", Instant.ofEpochMilli(3.hours.toMillis))
        person ! message2
        parent.expectNoMessage()

        val message3 = TimeTick(Instant.ofEpochMilli(5.hours.toMillis))
        person ! message3
        parent.expectNoMessage()
      }
    }
    "send alert" when {
      "broke" in {
        val parent = TestProbe[WindFarmCommand]()
        val person = spawn(Turbine("test-turbine", parent.ref))

        val message = TurbineEvent("test-turbine", Broken, 1.0, Instant.ofEpochMilli(0))
        person ! message
        parent.expectMessage(TurbineAlert(Instant.ofEpochMilli(0), "test-turbine", "Turbine broke"))
      }
      "broke and 4 hours passed" in {
        val parent = TestProbe[WindFarmCommand]()
        val person = spawn(Turbine("test-turbine", parent.ref))

        val message = TurbineEvent("test-turbine", Broken, 1.0, Instant.ofEpochMilli(0))
        person ! message
        parent.expectMessageType[TurbineAlert]

        val message2 = TimeTick(Instant.ofEpochMilli(4.hours.toMillis + 1))
        person ! message2
        parent.expectMessage(TurbineAlert(Instant.ofEpochMilli(4.hours.toMillis + 1), "test-turbine", "Turbine is still broken after 4 hours"))
      }
      "broke after worker exited" in {
        val parent = TestProbe[WindFarmCommand]()
        val person = spawn(Turbine("test-turbine", parent.ref))

        val message = TurbineEvent("test-turbine", Broken, 1.0, Instant.ofEpochMilli(0))
        person ! message
        parent.expectMessageType[TurbineAlert]

        val message2 = WorkerExitTurbine("test-engineer", "test-turbine", Instant.ofEpochMilli(3.hours.toMillis))
        person ! message2
        parent.expectNoMessage()

        val message3 = TimeTick(Instant.ofEpochMilli(3.hours.toMillis + 3.minutes.toMillis + 1))
        person ! message3
        parent.expectMessage(TurbineAlert(Instant.ofEpochMilli(3.hours.toMillis + 3.minutes.toMillis + 1), "test-turbine", "Turbine is still broken, worker test-engineer unable to help"))
      }
    }
  }
}
