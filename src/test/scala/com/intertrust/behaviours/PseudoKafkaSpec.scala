package com.intertrust.behaviours

import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestInbox, TestProbe}
import akka.persistence.testkit.PersistenceTestKitPlugin
import akka.persistence.testkit.scaladsl.PersistenceTestKit
import com.intertrust.protocol.{Event, TestMessage, TimeTick}
import com.intertrust.utils.PseudoKafkaConsumer
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import scala.concurrent.duration._

import java.time.Instant
import scala.language.implicitConversions

class PseudoKafkaSpec
  extends ScalaTestWithActorTestKit(PersistenceTestKitPlugin.config.withFallback(ConfigFactory.defaultApplication()))
    with AnyWordSpecLike with Matchers with BeforeAndAfterEach {

  val persistenceTestKit: PersistenceTestKit = PersistenceTestKit(system)

  override def beforeEach(): Unit = {
    persistenceTestKit.clearAll()
    persistenceTestKit.resetPolicy()
  }

  def iterator(messages: TestMessage*): Iterator[TestMessage] =
    messages.iterator

  "PseudoKafka behaviour" should {
    "produce messages" when {
      "log is one message" in {
        val message: TestMessage = 0
        val queue = iterator(message)
        val inbox = TestProbe[TestMessage]()
        val kafkaTestKit = spawn(PseudoKafkaConsumer("test-kafka", queue, inbox.ref))
        kafkaTestKit ! TimeTick(Instant.ofEpochMilli(100))
        inbox.expectMessage(message)
      }
      "multiple messages at once" in {
        val queue = iterator(
          1, 2, 3, 10
        )
        val inbox = TestProbe[TestMessage]()
        val kafkaTestKit = spawn(PseudoKafkaConsumer("test-kafka", queue, inbox.ref))
        kafkaTestKit ! TimeTick(Instant.ofEpochMilli(4))
        val messages = inbox.receiveMessages(3)
        assert(messages == Seq[TestMessage](1, 2, 3))
        inbox.expectNoMessage()
      }
    }
    "not produce messages" when {
      "messages in the future" in {
        val queue = iterator(
          100, 500, 600
        )
        val inbox = TestProbe[TestMessage]()
        val kafkaTestKit = spawn(PseudoKafkaConsumer("test-kafka", queue, inbox.ref))
        kafkaTestKit ! TimeTick(Instant.ofEpochMilli(99))
        inbox.expectNoMessage()
      }
    }
  }
}
