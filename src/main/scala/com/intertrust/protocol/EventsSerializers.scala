package com.intertrust.protocol

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonNode, JsonSerializer, SerializerProvider}

class MovementSerializer extends JsonSerializer[Movement] {
  def serialize(value: Movement, gen: JsonGenerator, serializers: SerializerProvider): Unit = {
    gen.writeStartObject()
    gen.writeStringField("type", value.entryName)
    gen.writeEndObject()
  }
}
class MovementDeserializer  extends JsonDeserializer[Movement]{
  def deserialize(p: JsonParser, ctxt: DeserializationContext): Movement = {
    val node: JsonNode = p.getCodec.readTree(p)
    Movement.withName(node.get("type").asText())
  }
}
