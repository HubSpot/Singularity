package com.hubspot.mesos;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class SingularityDockerParametersSerializer extends JsonSerializer<SingularityDockerParameters> {
  @Override
  public void serialize(SingularityDockerParameters dockerParameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
    if (dockerParameters.hasDuplicateKey()) {
      jsonGenerator.writeStartArray();
      for (SingularityDockerParameter parameter : dockerParameters.getParameters()) {
        jsonGenerator.writeObject(parameter);
      }
      jsonGenerator.writeEndArray();
    } else {
      jsonGenerator.writeObject(dockerParameters.toMap());
    }
  }
}
