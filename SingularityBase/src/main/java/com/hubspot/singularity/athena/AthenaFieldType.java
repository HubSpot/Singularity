package com.hubspot.singularity.athena;

public enum AthenaFieldType {
  STRING(true), TINYINT(true), SMALLINT(true), INT(true), BIGINT(true), BOOLEAN(true), FLOAT(true),
  DOUBLE(true), BINARY(true), TIMESTAMP(true), DATE(true), VARCHAR(true), CHAR(true), DECIMAL(true),
  ARRAY(false), MAP(false), STRUCT(false), UNIONTYPE(false);

  private final boolean primitive;

  AthenaFieldType(boolean primitive) {
    this.primitive = primitive;
  }

  public boolean isPrimitive() {
    return primitive;
  }
}
