package com.hubspot.singularity.athena;

import com.fasterxml.jackson.annotation.JsonIgnore;

public enum AthenaPartitionType {
  REQUESTID("requestId", "string"), YEAR("year", "string"), MONTH("month", "string"), DAY("day", "string");

  private final String field;
  private final String type;

  AthenaPartitionType(String field, String type) {
    this.field = field;
    this.type = type;
  }

  public String getField() {
    return field;
  }

  public String getType() {
    return type;
  }

  @JsonIgnore
  public String toQueryFriendlyString() {
    return String.format("`%s` %s", field, type);
  }
}
