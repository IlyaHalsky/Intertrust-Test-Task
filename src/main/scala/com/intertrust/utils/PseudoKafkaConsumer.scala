package com.intertrust.utils

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.scaladsl.Effect
import com.intertrust.protocol.{PersistableEvent, PersistableState, TimeTick, WithTimestamp}

import java.time.Instant
import scala.annotation.tailrec

object PseudoKafkaConsumer {
  def apply[Message <: WithTimestamp](
    actorName: String,
    eventQueue: Iterator[Message],
    sendTo: ActorRef[Message]
  ): Behavior[TimeTick] =
    StatefulPersistentBehaviour(PseudoKafkaConsumer(actorName, _, eventQueue, sendTo))
}

case class ConsumedMessage[Message<: WithTimestamp](offset: Int, nextMessage: Option[Message]) extends PersistableEvent

case class CurrentOffset[Message<: WithTimestamp](offset: Int, nextMessage: Option[Message]) extends PersistableState

case class PseudoKafkaConsumer[Message<: WithTimestamp](actorName: String, context: ActorContext[TimeTick], eventQueue: Iterator[Message], sendTo: ActorRef[Message])
  extends StatefulPersistentBehaviour[TimeTick, ConsumedMessage[Message], CurrentOffset[Message]] {
  val eventIterator: Iterator[(Message, Int)] = eventQueue.zipWithIndex
  def startingState: CurrentOffset[Message] = CurrentOffset(0, None)

  @tailrec
  private def consumeUntilTime(targetTime: Instant, currentOffset: Int, consumed: List[Message]): (List[Message], ConsumedMessage[Message]) =
  eventIterator.nextOption() match {
    case None => (consumed.reverse, ConsumedMessage(currentOffset, None))
    case Some((message, offset)) if message.timestamp.isAfter(targetTime) => (consumed.reverse, ConsumedMessage(offset, Some(message)))
    case Some((message, offset)) => consumeUntilTime(targetTime, offset, message :: consumed)
  }

  def commandHandler(state: CurrentOffset[Message], command: TimeTick): Effect[ConsumedMessage[Message], CurrentOffset[Message]] =
    if (state.nextMessage.exists(_.timestamp.isAfter(command.time))) Effect.none
    else {
      val (messages, event) = consumeUntilTime(command.time, state.offset, state.nextMessage.toList)
      Effect.persist(event).thenRun(_ => messages.foreach(sendTo ! _))
    }

  def eventHandler(state: CurrentOffset[Message], event: ConsumedMessage[Message]): CurrentOffset[Message] =
    state.copy(offset = event.offset, nextMessage = event.nextMessage)
  def onSnapshotRecover(state: CurrentOffset[Message]): Unit = {
    scrollIterator(state.offset)
  }

  @tailrec
  private def scrollIterator(targetOffset: Int): Unit =
    eventIterator.nextOption() match {
      case Some((_, offset)) if targetOffset > offset => scrollIterator(targetOffset)
      case _ => ()
    }
}
