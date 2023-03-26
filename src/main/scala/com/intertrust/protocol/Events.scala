package com.intertrust.protocol

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id
import enumeratum.{Enum, EnumEntry}

import java.time.Instant

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

sealed trait Movement extends EnumEntry

object Movement extends Enum[Movement] {
  override val values: IndexedSeq[Movement] = findValues

  case object Enter extends Movement

  case object Exit extends Movement

}

trait WithTimestamp extends Persistable {
  def timestamp: Instant
}

case class TurbineEvent(turbineId: String, status: TurbineStatus, generation: Double, timestamp: Instant) extends PersistableEvent with WithTimestamp

case class MovementEvent(engineerId: String, location: Location, movement: Movement, timestamp: Instant) extends PersonnelCommand with WithTimestamp
