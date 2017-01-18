package com.hubspot.mesos.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.google.common.base.Charsets;

import java.io.IOException;

public class UTF8StringDeserializer extends JsonDeserializer<UTF8String>{
  @Override
  public UTF8String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
    p.getTextCharacters()
    byte[] utf8Bytes = p.getText().getBytes(Charsets.UTF_8);
    return new UTF8String(utf8Bytes);
  }
}
