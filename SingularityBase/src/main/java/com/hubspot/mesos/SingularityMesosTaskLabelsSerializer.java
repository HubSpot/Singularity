package com.hubspot.mesos;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class SingularityMesosTaskLabelsSerializer extends JsonSerializer<SingularityMesosTaskLabels> {
  @Override
  public void serialize(SingularityMesosTaskLabels labels, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
    jsonGenerator.writeStartArray();
    for (SingularityMesosTaskLabel label : labels.getLabels()) {
      jsonGenerator.writeObject(label);
    }
    jsonGenerator.writeEndArray();
  }
}
