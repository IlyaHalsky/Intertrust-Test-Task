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
import scala.concurrent.duration._

import java.time.Instant
import scala.language.implicitConversions

class PersonSpec
  extends ScalaTestWithActorTestKit(PersistenceTestKitPlugin.config.withFallback(ConfigFactory.defaultApplication()))
    with AnyWordSpecLike with Matchers with BeforeAndAfterEach {

  val persistenceTestKit: PersistenceTestKit = PersistenceTestKit(system)

  override def beforeEach(): Unit = {
    persistenceTestKit.clearAll()
    persistenceTestKit.resetPolicy()
  }

  "Person behaviour" should {
    "persist" when {
      "on first enter" in {
        val parent = TestProbe[PersonnelCommand]()
        val person = spawn(Person("test-engineer", parent.ref))

        val message = MovementEvent("test-engineer", Vessel("123"), Enter, Instant.ofEpochMilli(0))
        person ! message

        val expectedPersistedEvent = PersonLocationChange(Some(message.location))
        persistenceTestKit.expectNextPersisted("test-engineer", expectedPersistedEvent)
      }
      "not on second enter" in {
        val parent = TestProbe[PersonnelCommand]()
        val person = spawn(Person("test-engineer", parent.ref))

        val message = MovementEvent("test-engineer", Vessel("123"), Enter, Instant.ofEpochMilli(0))
        person ! message
        val expectedPersistedEvent = PersonLocationChange(Some(message.location))
        persistenceTestKit.expectNextPersisted("test-engineer", expectedPersistedEvent)

        val message2 = MovementEvent("test-engineer", Vessel("123"), Enter, Instant.ofEpochMilli(1))
        person ! message2
        persistenceTestKit.expectNothingPersisted("test-engineer")
      }
      "persist on enter - exit" in {
        val parent = TestProbe[PersonnelCommand]()
        val person = spawn(Person("test-engineer", parent.ref))

        val message = MovementEvent("test-engineer", Vessel("123"), Enter, Instant.ofEpochMilli(0))
        person ! message
        val expectedPersistedEvent = PersonLocationChange(Some(message.location))
        persistenceTestKit.expectNextPersisted("test-engineer", expectedPersistedEvent)

        val message2 = MovementEvent("test-engineer", Vessel("123"), Exit, Instant.ofEpochMilli(1))
        person ! message2
        val expectedPersistedEvent2 = PersonLocationChange(None)
        persistenceTestKit.expectNextPersisted("test-engineer", expectedPersistedEvent2)
      }
      "not on second exit" in {
        val parent = TestProbe[PersonnelCommand]()
        val person = spawn(Person("test-engineer", parent.ref))

        val message = MovementEvent("test-engineer", Vessel("123"), Enter, Instant.ofEpochMilli(0))
        person ! message
        val expectedPersistedEvent = PersonLocationChange(Some(message.location))
        persistenceTestKit.expectNextPersisted("test-engineer", expectedPersistedEvent)

        val message2 = MovementEvent("test-engineer", Vessel("123"), Exit, Instant.ofEpochMilli(1))
        person ! message2
        val expectedPersistedEvent2 = PersonLocationChange(None)
        persistenceTestKit.expectNextPersisted("test-engineer", expectedPersistedEvent2)

        val message3 = MovementEvent("test-engineer", Vessel("123"), Exit, Instant.ofEpochMilli(2))
        person ! message2
        persistenceTestKit.expectNothingPersisted("test-engineer")
      }
      "persist on enter - invalid exit" in {
        val parent = TestProbe[PersonnelCommand]()
        val person = spawn(Person("test-engineer", parent.ref))

        val message = MovementEvent("test-engineer", Vessel("123"), Enter, Instant.ofEpochMilli(0))
        person ! message
        val expectedPersistedEvent = PersonLocationChange(Some(message.location))
        persistenceTestKit.expectNextPersisted("test-engineer", expectedPersistedEvent)

        val message2 = MovementEvent("test-engineer", Vessel("1234"), Exit, Instant.ofEpochMilli(1))
        person ! message2
        val expectedPersistedEvent2 = PersonLocationChange(None)
        persistenceTestKit.expectNextPersisted("test-engineer", expectedPersistedEvent2)
      }
      "persist on enter - invalid enter" in {
        val parent = TestProbe[PersonnelCommand]()
        val person = spawn(Person("test-engineer", parent.ref))

        val message = MovementEvent("test-engineer", Vessel("123"), Enter, Instant.ofEpochMilli(0))
        person ! message
        val expectedPersistedEvent = PersonLocationChange(Some(message.location))
        persistenceTestKit.expectNextPersisted("test-engineer", expectedPersistedEvent)

        val message2 = MovementEvent("test-engineer", Vessel("1234"), Enter, Instant.ofEpochMilli(1))
        person ! message2
        val expectedPersistedEvent2 = PersonLocationChange(Some(message2.location))
        persistenceTestKit.expectNextPersisted("test-engineer", expectedPersistedEvent2)
      }
    }
    "not send alert" when {
      "enter - exit vessel" in {
        val parent = TestProbe[PersonnelCommand]()
        val person = spawn(Person("test-engineer", parent.ref))

        val message = MovementEvent("test-engineer", Vessel("123"), Enter, Instant.ofEpochMilli(0))
        person ! message
        parent.expectNoMessage()

        val message2 = MovementEvent("test-engineer", Vessel("123"), Exit, Instant.ofEpochMilli(1))
        person ! message2
        parent.expectNoMessage()
      }
      "enter - exit turbine" in {
        val parent = TestProbe[PersonnelCommand]()
        val person = spawn(Person("test-engineer", parent.ref))

        val message = MovementEvent("test-engineer", Turbine("123"), Enter, Instant.ofEpochMilli(0))
        person ! message
        parent.expectMessageType[WorkerTurbineMove]

        val message2 = MovementEvent("test-engineer", Turbine("123"), Exit, Instant.ofEpochMilli(1))
        person ! message2
        parent.expectMessageType[WorkerTurbineMove]
      }
    }
    "send alert" when {
      "enter with no exit" in {
        val parent = TestProbe[PersonnelCommand]()
        val person = spawn(Person("test-engineer", parent.ref))

        val message = MovementEvent("test-engineer", Vessel("123"), Enter, Instant.ofEpochMilli(0))
        person ! message
        parent.expectNoMessage()

        val message2 = MovementEvent("test-engineer", Vessel("1234"), Enter, Instant.ofEpochMilli(1))
        person ! message2
        parent.expectMessage(MovementAlert(Instant.ofEpochMilli(1), "test-engineer", s"Entered new location 1234 without exiting 123"))
      }
      "enter same location again" in {
        val parent = TestProbe[PersonnelCommand]()
        val person = spawn(Person("test-engineer", parent.ref))

        val message = MovementEvent("test-engineer", Vessel("123"), Enter, Instant.ofEpochMilli(0))
        person ! message
        parent.expectNoMessage()

        val message2 = MovementEvent("test-engineer", Vessel("123"), Enter, Instant.ofEpochMilli(1))
        person ! message2
        parent.expectMessage(MovementAlert(Instant.ofEpochMilli(1), "test-engineer", "Entered location 123 again"))
      }
      "exit location != previous location" in {
        val parent = TestProbe[PersonnelCommand]()
        val person = spawn(Person("test-engineer", parent.ref))

        val message = MovementEvent("test-engineer", Vessel("123"), Enter, Instant.ofEpochMilli(0))
        person ! message
        parent.expectNoMessage()

        val message2 = MovementEvent("test-engineer", Vessel("1234"), Exit, Instant.ofEpochMilli(1))
        person ! message2
        parent.expectMessage(MovementAlert(Instant.ofEpochMilli(1), "test-engineer", "Exited 1234 without exiting 123"))
      }
      "exit location with no previous location" in {
        val parent = TestProbe[PersonnelCommand]()
        val person = spawn(Person("test-engineer", parent.ref))

        val message = MovementEvent("test-engineer", Vessel("123"), Exit, Instant.ofEpochMilli(0))
        person ! message
        parent.expectMessage(MovementAlert(Instant.ofEpochMilli(0), "test-engineer", "Exited 123 without entering it first"))
      }
    }
    "send worker movement" when {
      "on turbine enter" in {
        val parent = TestProbe[PersonnelCommand]()
        val person = spawn(Person("test-engineer", parent.ref))

        val message = MovementEvent("test-engineer", Turbine("123"), Enter, Instant.ofEpochMilli(0))
        person ! message
        parent.expectMessage(WorkerEnter("123", Instant.ofEpochMilli(0)))
      }
      "on turbine exit" in {
        val parent = TestProbe[PersonnelCommand]()
        val person = spawn(Person("test-engineer", parent.ref))

        val message = MovementEvent("test-engineer", Turbine("123"), Enter, Instant.ofEpochMilli(0))
        person ! message
        parent.expectMessage(WorkerEnter("123", Instant.ofEpochMilli(0)))

        val message2 = MovementEvent("test-engineer", Turbine("123"), Exit, Instant.ofEpochMilli(1))
        person ! message2
        parent.expectMessage(WorkerExit("123", Instant.ofEpochMilli(1)))
      }
      "exit unvisited turbine" in {
        val parent = TestProbe[PersonnelCommand]()
        val person = spawn(Person("test-engineer", parent.ref))

        val message = MovementEvent("test-engineer", Turbine("123"), Exit, Instant.ofEpochMilli(0))
        person ! message
        parent.expectMessage(WorkerExit("123", Instant.ofEpochMilli(0)))
      }
    }
  }
}
