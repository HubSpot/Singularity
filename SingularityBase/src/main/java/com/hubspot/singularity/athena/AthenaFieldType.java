package com.hubspot.singularity.athena;

public enum AthenaFieldType {
  STRING(true, true), TINYINT(true, false), SMALLINT(true, false), INT(true, false), BIGINT(true, false), BOOLEAN(true, false), FLOAT(true, false),
  DOUBLE(true, false), BINARY(true, false), TIMESTAMP(true, true), DATE(true, true), VARCHAR(true, true), CHAR(true, true), DECIMAL(true, false),
  ARRAY(false, true), MAP(false, true), STRUCT(false, true), UNIONTYPE(false, true), INTEGER(true, false);

  private final boolean primitive;
  private final boolean requireQuotes;

  AthenaFieldType(boolean primitive, boolean requireQuotes) {
    this.primitive = primitive;
    this.requireQuotes = requireQuotes;
  }

  public boolean isPrimitive() {
    return primitive;
  }

  public boolean isRequireQuotes() {
    return requireQuotes;
  }
}
