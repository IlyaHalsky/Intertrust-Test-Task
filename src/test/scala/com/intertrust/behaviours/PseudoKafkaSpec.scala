package com.intertrust.behaviours

import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import akka.persistence.testkit.PersistenceTestKitPlugin
import akka.persistence.testkit.scaladsl.PersistenceTestKit
import com.intertrust.protocol.{Event, TimeTick}
import com.intertrust.test_utils._
import com.intertrust.utils.PseudoKafkaConsumer
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.language.implicitConversions

class PseudoKafkaSpec
  extends ScalaTestWithActorTestKit(PersistenceTestKitPlugin.config.withFallback(ConfigFactory.defaultApplication()))
    with AnyWordSpecLike with Matchers with BeforeAndAfterEach with TestUtils {

  val persistenceTestKit: PersistenceTestKit = PersistenceTestKit(system)

  override def beforeEach(): Unit = {
    persistenceTestKit.clearAll()
    persistenceTestKit.resetPolicy()
  }

  def iterator(messages: Event*): Iterator[Event] =
    messages.iterator

  "PseudoKafka behaviour" should {
    "produce messages" when {
      "log is one message" in {
        val message = moveVessel()
        val queue = iterator(message)
        val inbox = TestProbe[Event]()
        val kafkaTestKit = spawn(PseudoKafkaConsumer(kafkaId, queue, inbox.ref))
        kafkaTestKit ! TimeTick(plus(100))
        inbox.expectMessage(message)
      }
      "multiple messages at once" in {
        val queue = iterator(
          moveVessel(1),
          moveVessel(2),
          moveVessel(3),
          moveVessel(100),
          moveVessel(101),
        )
        val inbox = TestProbe[Event]()
        val kafkaTestKit = spawn(PseudoKafkaConsumer(kafkaId, queue, inbox.ref))
        kafkaTestKit ! TimeTick(plus(4))
        val messages = inbox.receiveMessages(3)
        assert(messages == Seq[Event](moveVessel(1), moveVessel(2), moveVessel(3)))
        inbox.expectNoMessage()
      }
    }
    "not produce messages" when {
      "messages in the future" in {
        val queue = iterator(
          moveVessel(100),
          moveVessel(500),
          moveVessel(600),
        )
        val inbox = TestProbe[Event]()
        val kafkaTestKit = spawn(PseudoKafkaConsumer(kafkaId, queue, inbox.ref))
        kafkaTestKit ! TimeTick(plus(99))
        inbox.expectNoMessage()
      }
    }
  }
}
