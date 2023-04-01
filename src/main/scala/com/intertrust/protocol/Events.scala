package com.intertrust.protocol

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import enumeratum.{Enum, EnumEntry}

import java.time.Instant


@JsonSerialize(using = classOf[TurbineStatusSerializer])
@JsonDeserialize(using = classOf[TurbineStatusDeserializer])
sealed trait TurbineStatus extends EnumEntry

object TurbineStatus extends Enum[TurbineStatus] {
  override val values: IndexedSeq[TurbineStatus] = findValues

  case object Working extends TurbineStatus

  case object Broken extends TurbineStatus

}

@JsonTypeInfo(use = Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  property = "type"
)
@JsonSubTypes(Array(
  new Type(value = classOf[Vessel]),
  new Type(value = classOf[Turbine])
)
)
trait Location extends Persistable {
  def id: String
}

case class Vessel(id: String) extends Location

case class Turbine(id: String) extends Location

@JsonSerialize(using = classOf[MovementSerializer])
@JsonDeserialize(using = classOf[MovementDeserializer])
sealed trait Movement extends EnumEntry

object Movement extends Enum[Movement] {
  override val values: IndexedSeq[Movement] = findValues

  case object Enter extends Movement

  case object Exit extends Movement

}

@JsonTypeInfo(use = Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  property = "type"
)
@JsonSubTypes(Array(
  new Type(value = classOf[TurbineEvent]),
  new Type(value = classOf[MovementEvent]),
  new Type(value = classOf[TestMessage]),
)
)
trait Event extends Persistable {
  def timestamp: Instant
}

case class TurbineEvent(turbineId: String, status: TurbineStatus, generation: Double, timestamp: Instant) extends PersistableEvent with Event with WindFarmCommand with TurbineCommand

case class MovementEvent(engineerId: String, location: Location, movement: Movement, timestamp: Instant) extends PersonnelCommand with Event
