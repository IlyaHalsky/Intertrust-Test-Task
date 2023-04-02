package com.intertrust.behaviours

import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import akka.persistence.testkit.PersistenceTestKitPlugin
import akka.persistence.testkit.scaladsl.PersistenceTestKit
import com.intertrust.protocol.Movement.Exit
import com.intertrust.protocol._
import com.intertrust.test_utils.TestUtils
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.language.implicitConversions

class PersonSpec
  extends ScalaTestWithActorTestKit(PersistenceTestKitPlugin.config.withFallback(ConfigFactory.defaultApplication()))
    with AnyWordSpecLike with Matchers with BeforeAndAfterEach with TestUtils {

  val persistenceTestKit: PersistenceTestKit = PersistenceTestKit(system)

  override def beforeEach(): Unit = {
    persistenceTestKit.clearAll()
    persistenceTestKit.resetPolicy()
  }

  "Person behaviour" should {
    "persist" when {
      "on first enter" in {
        val parent = TestProbe[PersonnelCommand]()
        val person = spawn(Person(engineerId, parent.ref))

        val message = moveVessel()
        person ! message

        val expectedPersistedEvent = PersonLocationChange(Some(message.location))
        persistenceTestKit.expectNextPersisted(engineerId, expectedPersistedEvent)
      }
      "not on second enter" in {
        val parent = TestProbe[PersonnelCommand]()
        val person = spawn(Person(engineerId, parent.ref))

        val message = moveVessel()
        person ! message
        val expectedPersistedEvent = PersonLocationChange(Some(message.location))
        persistenceTestKit.expectNextPersisted(engineerId, expectedPersistedEvent)

        val message2 = moveVessel(1)
        person ! message2
        persistenceTestKit.expectNothingPersisted(engineerId)
      }
      "persist on enter - exit" in {
        val parent = TestProbe[PersonnelCommand]()
        val person = spawn(Person(engineerId, parent.ref))

        val message = moveVessel()
        person ! message
        val expectedPersistedEvent = PersonLocationChange(Some(message.location))
        persistenceTestKit.expectNextPersisted(engineerId, expectedPersistedEvent)

        val message2 = moveVessel(1, Exit)
        person ! message2
        val expectedPersistedEvent2 = PersonLocationChange(None)
        persistenceTestKit.expectNextPersisted(engineerId, expectedPersistedEvent2)
      }
      "not on second exit" in {
        val parent = TestProbe[PersonnelCommand]()
        val person = spawn(Person(engineerId, parent.ref))

        val message = moveVessel()
        person ! message
        val expectedPersistedEvent = PersonLocationChange(Some(message.location))
        persistenceTestKit.expectNextPersisted(engineerId, expectedPersistedEvent)

        val message2 = moveVessel(1, Exit)
        person ! message2
        val expectedPersistedEvent2 = PersonLocationChange(None)
        persistenceTestKit.expectNextPersisted(engineerId, expectedPersistedEvent2)

        val message3 = moveVessel(2, Exit)
        person ! message3
        persistenceTestKit.expectNothingPersisted(engineerId)
      }
      "persist on enter - invalid exit" in {
        val parent = TestProbe[PersonnelCommand]()
        val person = spawn(Person(engineerId, parent.ref))

        val message = moveVessel()
        person ! message
        val expectedPersistedEvent = PersonLocationChange(Some(message.location))
        persistenceTestKit.expectNextPersisted(engineerId, expectedPersistedEvent)

        val message2 = moveVessel(1, Exit, invalidVesselId)
        person ! message2
        val expectedPersistedEvent2 = PersonLocationChange(None)
        persistenceTestKit.expectNextPersisted(engineerId, expectedPersistedEvent2)
      }
      "persist on enter - invalid enter" in {
        val parent = TestProbe[PersonnelCommand]()
        val person = spawn(Person(engineerId, parent.ref))

        val message = moveVessel()
        person ! message
        val expectedPersistedEvent = PersonLocationChange(Some(message.location))
        persistenceTestKit.expectNextPersisted(engineerId, expectedPersistedEvent)

        val message2 = moveVessel(1, vesselId = invalidVesselId)
        person ! message2
        val expectedPersistedEvent2 = PersonLocationChange(Some(message2.location))
        persistenceTestKit.expectNextPersisted(engineerId, expectedPersistedEvent2)
      }
    }
    "not send alert" when {
      "enter - exit vessel" in {
        val parent = TestProbe[PersonnelCommand]()
        val person = spawn(Person(engineerId, parent.ref))

        val message = moveVessel()
        person ! message
        parent.expectNoMessage()

        val message2 = moveVessel(1, Exit)
        person ! message2
        parent.expectNoMessage()
      }
      "enter - exit turbine" in {
        val parent = TestProbe[PersonnelCommand]()
        val person = spawn(Person(engineerId, parent.ref))

        val message = moveTurbine()
        person ! message
        parent.expectMessageType[WorkerTurbineMove]

        val message2 = moveTurbine(1, Exit)
        person ! message2
        parent.expectMessageType[WorkerTurbineMove]
      }
    }
    "send alert" when {
      "enter with no exit" in {
        val parent = TestProbe[PersonnelCommand]()
        val person = spawn(Person(engineerId, parent.ref))

        val message = moveVessel()
        person ! message
        parent.expectNoMessage()

        val message2 = moveVessel(1, vesselId = invalidVesselId)
        person ! message2
        parent.expectMessage(movementAlert(1, s"Entered new location $invalidVesselId without exiting $vesselId"))
      }
      "enter same location again" in {
        val parent = TestProbe[PersonnelCommand]()
        val person = spawn(Person(engineerId, parent.ref))

        val message = moveVessel()
        person ! message
        parent.expectNoMessage()

        val message2 = moveVessel(1)
        person ! message2
        parent.expectMessage(movementAlert(1, s"Entered location $vesselId again"))
      }
      "exit location != previous location" in {
        val parent = TestProbe[PersonnelCommand]()
        val person = spawn(Person(engineerId, parent.ref))

        val message = moveVessel()
        person ! message
        parent.expectNoMessage()

        val message2 = moveVessel(1, Exit, invalidVesselId)
        person ! message2
        parent.expectMessage(movementAlert(1, s"Exited $invalidVesselId without exiting $vesselId"))
      }
      "exit location with no previous location" in {
        val parent = TestProbe[PersonnelCommand]()
        val person = spawn(Person(engineerId, parent.ref))

        val message = moveVessel(enter = Exit)
        person ! message
        parent.expectMessage(movementAlert(0, s"Exited $vesselId without entering it first"))
      }
    }
    "send worker movement" when {
      "on turbine enter" in {
        val parent = TestProbe[PersonnelCommand]()
        val person = spawn(Person(engineerId, parent.ref))

        val message = moveTurbine()
        person ! message
        parent.expectMessage(workerMoveTurbine())
      }
      "on turbine exit" in {
        val parent = TestProbe[PersonnelCommand]()
        val person = spawn(Person(engineerId, parent.ref))

        val message = moveTurbine()
        person ! message
        parent.expectMessage(workerMoveTurbine())

        val message2 = moveTurbine(1, Exit)
        person ! message2
        parent.expectMessage(workerMoveTurbine(1, Exit))
      }
      "exit unvisited turbine" in {
        val parent = TestProbe[PersonnelCommand]()
        val person = spawn(Person(engineerId, parent.ref))

        val message = moveTurbine(enter = Exit)
        person ! message
        parent.expectMessage(workerMoveTurbine(enter = Exit))
      }
    }
  }
}
