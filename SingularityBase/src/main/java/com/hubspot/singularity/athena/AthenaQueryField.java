package com.hubspot.singularity.athena;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AthenaQueryField {
  private final String field;
  private final ComparisonOperator comparisonOperator;
  private final String value;
  private final AthenaFieldType type;

  @JsonCreator
  public AthenaQueryField(@JsonProperty("field") String field,
                          @JsonProperty("comparisonOperator") ComparisonOperator comparisonOperator,
                          @JsonProperty("value") String value,
                          @JsonProperty("type") AthenaFieldType type) {
    this.field = field;
    this.comparisonOperator = comparisonOperator;
    this.value = value;
    this.type = type;
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

  public AthenaFieldType getType() {
    return type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    AthenaQueryField that = (AthenaQueryField) o;

    if (field != null ? !field.equals(that.field) : that.field != null) {
      return false;
    }
    if (comparisonOperator != that.comparisonOperator) {
      return false;
    }
    if (value != null ? !value.equals(that.value) : that.value != null) {
      return false;
    }
    return type == that.type;
  }

  @Override
  public int hashCode() {
    int result = field != null ? field.hashCode() : 0;
    result = 31 * result + (comparisonOperator != null ? comparisonOperator.hashCode() : 0);
    result = 31 * result + (value != null ? value.hashCode() : 0);
    result = 31 * result + (type != null ? type.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "AthenaQueryField{" +
        "field='" + field + '\'' +
        ", comparisonOperator=" + comparisonOperator +
        ", value='" + value + '\'' +
        ", type=" + type +
        '}';
  }
}
