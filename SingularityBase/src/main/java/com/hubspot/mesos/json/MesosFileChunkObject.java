package com.hubspot.mesos.json;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class MesosFileChunkObject {
  private final String data;
  private final long offset;
  private final Optional<Long> nextOffset;

  @JsonCreator
  public MesosFileChunkObject(@JsonProperty("data") String data, @JsonProperty("offset") long offset, @JsonProperty("nextOffset") Optional<Long> nextOffset) {
    this.data = data;
    this.offset = offset;
    this.nextOffset = nextOffset;
  }

  public String getData() {
    return data;
  }

  public long getOffset() {
    return offset;
  }

  public Optional<Long> getNextOffset() {
    return nextOffset;
  }

  @Override
  public String toString() {
    return "MesosFileChunkObject[" +
            "data='" + data + '\'' +
            ", offset=" + offset +
            ", nextOffset=" + nextOffset +
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
            Objects.equals(data, that.data) &&
            Objects.equals(nextOffset, that.nextOffset);
  }

  @Override
  public int hashCode() {
    return Objects.hash(data, offset, nextOffset);
  }
}
