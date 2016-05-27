package com.hubspot.mesos.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Optional;

import java.util.Objects;

public class MesosBinaryChunkObject {
  @JsonDeserialize(using = UTF8StringDeserializer.class)
  @JsonSerialize(using = UTF8StringSerializer.class)
  private final UTF8String data;

  private final long offset;
  private final Optional<Long> nextOffset;

  @JsonCreator
  public MesosBinaryChunkObject(@JsonProperty("data") UTF8String data, @JsonProperty("offset") long offset, @JsonProperty("nextOffset") Optional<Long> nextOffset) {
    this.data = data;
    this.offset = offset;
    this.nextOffset = nextOffset;
  }

  public UTF8String getData() {
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
    return "MesosBinaryChunkObject[" +
        "data='" + data + "'" +
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
    MesosBinaryChunkObject that = (MesosBinaryChunkObject) o;
    return Objects.equals(offset, that.offset) &&
        Objects.equals(data, that.data) &&
        Objects.equals(nextOffset, that.nextOffset);
  }

  @Override
  public int hashCode() {
    return Objects.hash(data, offset, nextOffset);
  }
}
