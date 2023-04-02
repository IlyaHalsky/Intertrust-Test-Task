package com.intertrust.behaviours

import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import akka.persistence.testkit.PersistenceTestKitPlugin
import akka.persistence.testkit.scaladsl.PersistenceTestKit
import com.intertrust.behaviours.Turbine
import com.intertrust.protocol.Movement.Exit
import com.intertrust.protocol._
import com.intertrust.test_utils.{TestUtils, _}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.Instant
import scala.concurrent.duration._
import scala.language.implicitConversions

class TurbineSpec
  extends ScalaTestWithActorTestKit(PersistenceTestKitPlugin.config.withFallback(ConfigFactory.defaultApplication()))
    with AnyWordSpecLike with Matchers with BeforeAndAfterEach with TestUtils {

  val persistenceTestKit: PersistenceTestKit = PersistenceTestKit(system)

  override def beforeEach(): Unit = {
    persistenceTestKit.clearAll()
    persistenceTestKit.resetPolicy()
  }

  "Turbine behaviour" should {
    "persist" when {
      "broke twice" in {
        val parent = TestProbe[WindFarmCommand]()
        val person = spawn(Turbine(turbineId, parent.ref))

        val message = turbineBroke()
        person ! message
        val expectedPersistedEvent = BrokeEvent(plus(0))
        persistenceTestKit.expectNextPersisted(turbineId, expectedPersistedEvent)

        val message2 = turbineBroke(1)
        person ! message2
        persistenceTestKit.expectNothingPersisted(turbineId)
      }
      "broke then fixed" in {
        val parent = TestProbe[WindFarmCommand]()
        val person = spawn(Turbine(turbineId, parent.ref))

        val message = turbineBroke()
        person ! message
        val expectedPersistedEvent = BrokeEvent(plus(0))
        persistenceTestKit.expectNextPersisted(turbineId, expectedPersistedEvent)

        val message2 = turbineWorking(1)
        person ! message2
        val expectedPersistedEvent2 = RepairedEvent()
        persistenceTestKit.expectNextPersisted(turbineId, expectedPersistedEvent2)
      }
      "working then worker enter" in {
        val parent = TestProbe[WindFarmCommand]()
        val person = spawn(Turbine(turbineId, parent.ref))

        val message = workerMoveTurbine()
        person ! message
        persistenceTestKit.expectNothingPersisted(turbineId)
      }
      "broke then worker enter" in {
        val parent = TestProbe[WindFarmCommand]()
        val person = spawn(Turbine(turbineId, parent.ref))

        val message = turbineBroke()
        person ! message
        val expectedPersistedEvent = BrokeEvent(plus(0))
        persistenceTestKit.expectNextPersisted(turbineId, expectedPersistedEvent)

        val message2 = workerMoveTurbine(1)
        person ! message2
        val expectedPersistedEvent2 = WorkerEnterEvent()
        persistenceTestKit.expectNextPersisted(turbineId, expectedPersistedEvent2)
      }
      "working then worker exit" in {
        val parent = TestProbe[WindFarmCommand]()
        val person = spawn(Turbine(turbineId, parent.ref))

        val message = workerMoveTurbine(enter = Exit)
        person ! message
        persistenceTestKit.expectNothingPersisted(turbineId)
      }
      "broke then worker exit" in {
        val parent = TestProbe[WindFarmCommand]()
        val person = spawn(Turbine(turbineId, parent.ref))

        val message = turbineBroke()
        person ! message
        val expectedPersistedEvent = BrokeEvent(plus(0))
        persistenceTestKit.expectNextPersisted(turbineId, expectedPersistedEvent)

        val message2 = workerMoveTurbine(1, Exit)
        person ! message2
        val expectedPersistedEvent2 = WorkerExitEvent(plus(1), "test-engineer")
        persistenceTestKit.expectNextPersisted(turbineId, expectedPersistedEvent2)
      }
      "broke then alerted" in {
        val parent = TestProbe[WindFarmCommand]()
        val person = spawn(Turbine(turbineId, parent.ref))

        val message = turbineBroke()
        person ! message
        val expectedPersistedEvent = BrokeEvent(plus(0))
        persistenceTestKit.expectNextPersisted(turbineId, expectedPersistedEvent)

        val message2 = TimeTick(plus(4.hours) + 1)
        person ! message2
        val expectedPersistedEvent2 = AlertReportedEvent()
        persistenceTestKit.expectNextPersisted(turbineId, expectedPersistedEvent2)
      }
    }
    "not send alert" when {
      "broke then fixed" in {
        val parent = TestProbe[WindFarmCommand]()
        val person = spawn(Turbine(turbineId, parent.ref))

        val message = turbineBroke()
        person ! message
        parent.expectMessageType[TurbineAlert]

        val message2 = TimeTick(plus(3.hours))
        person ! message2
        parent.expectNoMessage()

        val message3 = turbineWorking()
        person ! message3
        parent.expectNoMessage()

        val message4 = TimeTick(plus(5.hours))
        person ! message4
        parent.expectNoMessage()
      }
      "broke then after second report" in {
        val parent = TestProbe[WindFarmCommand]()
        val person = spawn(Turbine(turbineId, parent.ref))

        val message = turbineBroke()
        person ! message
        parent.expectMessageType[TurbineAlert]

        val message2 = TimeTick(plus(4.hours) + 1)
        person ! message2
        parent.expectMessageType[TurbineAlert]

        val message3 = TimeTick(plus(5.hours))
        person ! message3
        parent.expectNoMessage()
      }
      "broke then worker enter" in {
        val parent = TestProbe[WindFarmCommand]()
        val person = spawn(Turbine(turbineId, parent.ref))

        val message = turbineBroke()
        person ! message
        parent.expectMessageType[TurbineAlert]

        val message2 = workerMoveTurbine(3.hours.toMillis)
        person ! message2
        parent.expectNoMessage()

        val message3 = TimeTick(plus(5.hours))
        person ! message3
        parent.expectNoMessage()
      }
    }
    "send alert" when {
      "broke" in {
        val parent = TestProbe[WindFarmCommand]()
        val person = spawn(Turbine(turbineId, parent.ref))

        val message = turbineBroke()
        person ! message
        parent.expectMessage(turbineAlert(0, "Turbine broke"))
      }
      "broke and 4 hours passed" in {
        val parent = TestProbe[WindFarmCommand]()
        val person = spawn(Turbine(turbineId, parent.ref))

        val message = turbineBroke()
        person ! message
        parent.expectMessageType[TurbineAlert]

        val message2 = TimeTick(plus(4.hours) + 1)
        person ! message2
        parent.expectMessage(turbineAlert(4.hours.toMillis + 1, "Turbine is still broken after 4 hours"))
      }
      "broke after worker exited" in {
        val parent = TestProbe[WindFarmCommand]()
        val person = spawn(Turbine(turbineId, parent.ref))

        val message = turbineBroke()
        person ! message
        parent.expectMessageType[TurbineAlert]

        val message2 = workerMoveTurbine(3.hours.toMillis, Exit)
        person ! message2
        parent.expectNoMessage()

        val message3 = TimeTick(plus(3.hours) + 3.minutes + 1)
        person ! message3
        parent.expectMessage(turbineAlert(3.hours.toMillis + 3.minutes.toMillis + 1, "Turbine is still broken, worker test-engineer unable to help"))
      }
    }
  }
}
