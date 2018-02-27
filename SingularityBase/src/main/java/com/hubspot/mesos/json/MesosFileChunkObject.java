package com.hubspot.mesos.json;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A portion of a file from a task sandbox")
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

  @Schema(description = "Content of this portion of the file")
  public String getData() {
    return data;
  }

  @Schema(description = "Offset in bytes of this content")
  public long getOffset() {
    return offset;
  }

  @Schema(description = "The next offset to fetch to continue from the end of the content in this object", nullable = true)
  public Optional<Long> getNextOffset() {
    return nextOffset;
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
    return offset == that.offset &&
        Objects.equals(data, that.data) &&
        Objects.equals(nextOffset, that.nextOffset);
  }

  @Override
  public int hashCode() {
    return Objects.hash(data, offset, nextOffset);
  }

  @Override
  public String toString() {
    return "MesosFileChunkObject{" +
        "data='" + data + '\'' +
        ", offset=" + offset +
        ", nextOffset=" + nextOffset +
        '}';
  }
}
