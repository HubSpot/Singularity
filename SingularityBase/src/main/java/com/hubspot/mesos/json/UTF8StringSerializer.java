package com.hubspot.mesos.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class UTF8StringSerializer extends JsonSerializer<UTF8String> {
  @Override
  public void serialize(UTF8String value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
    gen.writeUTF8String(value.getData(), value.getOffset(), value.getLength());
  }
}
