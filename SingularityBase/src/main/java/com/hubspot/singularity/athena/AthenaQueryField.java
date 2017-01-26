package com.hubspot.singularity.athena;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AthenaQueryField {
  private final String field;
  private final ComparisonOperator comparisonOperator;
  private final String value;

  @JsonCreator
  public AthenaQueryField(@JsonProperty("field") String field, @JsonProperty("comparisonOperator") ComparisonOperator comparisonOperator, @JsonProperty("value") String value) {
    this.field = field;
    this.comparisonOperator = comparisonOperator;
    this.value = value;
  }

  public String getField() {
    return field;
  }

  public ComparisonOperator getComparisonOperator() {
    return comparisonOperator;
  }

  public String getValue() {
    return value;
  }

  @JsonIgnore
  public String toQueryString() {
    return String.format("%s %s '%s'", field, comparisonOperator.getValue(), value);
  }
}
