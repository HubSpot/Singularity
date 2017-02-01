package com.hubspot.singularity.athena;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdatePartitionsRequest {
  private final String tableName;
  private final long start;
  private final long end;

  @JsonCreator
  public UpdatePartitionsRequest(@JsonProperty("tableName") String tableName,
                                 @JsonProperty("start") long start,
                                 @JsonProperty("end") long end) {
    this.tableName = tableName;
    this.start = start;
    this.end = end;
  }

  public String getTableName() {
    return tableName;
  }

  public long getStart() {
    return start;
  }

  public long getEnd() {
    return end;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    UpdatePartitionsRequest that = (UpdatePartitionsRequest) o;

    if (start != that.start) {
      return false;
    }
    if (end != that.end) {
      return false;
    }
    return tableName != null ? tableName.equals(that.tableName) : that.tableName == null;
  }

  @Override
  public int hashCode() {
    int result = tableName != null ? tableName.hashCode() : 0;
    result = 31 * result + (int) (start ^ (start >>> 32));
    result = 31 * result + (int) (end ^ (end >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "UpdatePartitionsRequest{" +
        "tableName='" + tableName + '\'' +
        ", start=" + start +
        ", end=" + end +
        '}';
  }
}
