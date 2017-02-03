package com.hubspot.singularity.athena;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class UpdatePartitionsRequest {
  private final String tableName;
  private final long start;
  private final Optional<Long> end;

  @JsonCreator
  public UpdatePartitionsRequest(@JsonProperty("tableName") String tableName,
                                 @JsonProperty("start") long start,
                                 @JsonProperty("end") Optional<Long> end) {
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

  public Optional<Long> getEnd() {
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
    if (tableName != null ? !tableName.equals(that.tableName) : that.tableName != null) {
      return false;
    }
    return end != null ? end.equals(that.end) : that.end == null;
  }

  @Override
  public int hashCode() {
    int result = tableName != null ? tableName.hashCode() : 0;
    result = 31 * result + (int) (start ^ (start >>> 32));
    result = 31 * result + (end != null ? end.hashCode() : 0);
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
