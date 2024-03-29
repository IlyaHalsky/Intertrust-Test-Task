package com.intertrust.protocol

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind._
import enumeratum.EnumEntry

trait JSerializer[Entry <: EnumEntry] extends JsonSerializer[Entry] {
  def serialize(value: Entry, gen: JsonGenerator, serializers: SerializerProvider): Unit = {
    gen.writeStartObject()
    gen.writeStringField("type", value.entryName)
    gen.writeEndObject()
  }
}

trait JDeserializer[Entry <: EnumEntry] extends JsonDeserializer[Entry] {
  def enumeration: enumeratum.Enum[Entry]

  def deserialize(p: JsonParser, ctxt: DeserializationContext): Entry = {
    val node: JsonNode = p.getCodec.readTree(p)
    enumeration.withName(node.get("type").asText())
  }
}

class MovementSerializer extends JSerializer[Movement] {}

class MovementDeserializer extends JDeserializer[Movement] {
  def enumeration: enumeratum.Enum[Movement] = Movement
}

class TurbineStatusSerializer extends JSerializer[TurbineStatus] {}

class TurbineStatusDeserializer extends JDeserializer[TurbineStatus] {
  def enumeration: enumeratum.Enum[TurbineStatus] = TurbineStatus
}