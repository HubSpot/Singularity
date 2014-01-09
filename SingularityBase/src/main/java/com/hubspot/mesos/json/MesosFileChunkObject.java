package com.hubspot.mesos.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MesosFileChunkObject {
  private final String data;
  private final long offset;

  @JsonCreator
  public MesosFileChunkObject(@JsonProperty("data") String data, @JsonProperty("offset") long offset) {
    this.data = data;
    this.offset = offset;
  }

  public String getData() {
    return data;
  }

  public long getOffset() {
    return offset;
  }
}
