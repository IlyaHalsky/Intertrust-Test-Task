package com.intertrust.protocol

import java.time.Instant
import scala.language.implicitConversions

case class TestMessage(timestamp: Instant, message: String) extends Event // Had to move test class here for serialization

object TestMessage {
  def apply(millis: Long): TestMessage =
    TestMessage(Instant.ofEpochMilli(millis), millis.toString)

  implicit def longToTestMessage(long: Long): TestMessage =
    TestMessage(long)
}


