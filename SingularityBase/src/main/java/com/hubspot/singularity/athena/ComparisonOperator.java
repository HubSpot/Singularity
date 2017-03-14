package com.hubspot.singularity.athena;

public enum ComparisonOperator {
  EQUAL("="), EQUAL_COMAPRE_NULL("<=>"), NOT_EQUAL("!="), GREATER_THAN(">"), GREATER_THAN_OR_EQUAL_TO(">="), LESS_THAN("<"), LESS_THAN_OR_EQUAL_TO("<="),
  LIKE("LIKE"), NULL("IS NULL"), NOT_NULL("IS NOT NULL"), EXISTS("EXISTS");

  private final String value;

  ComparisonOperator(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
