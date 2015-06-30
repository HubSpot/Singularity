package com.hubspot.mesos.json;

import java.util.Objects;

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

  @Override
  public String toString() {
    return "MesosFileChunkObject[" +
            "data='" + data + '\'' +
            ", offset=" + offset +
            ']';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MesosFileChunkObject that = (MesosFileChunkObject) o;
    return Objects.equals(offset, that.offset) &&
            Objects.equals(data, that.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(data, offset);
  }
}
