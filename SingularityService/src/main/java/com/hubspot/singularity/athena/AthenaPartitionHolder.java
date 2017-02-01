package com.hubspot.singularity.athena;

import java.util.List;

public class AthenaPartitionHolder {
  private final List<AthenaPartitionWithValue> partitionsWithValues;
  private final String queryStatement;

  AthenaPartitionHolder(List<AthenaPartitionWithValue> partitionsWithValues, String queryStatement) {
    this.partitionsWithValues = partitionsWithValues;
    this.queryStatement = queryStatement;
  }

  public List<AthenaPartitionWithValue> getPartitionsWithValues() {
    return partitionsWithValues;
  }

  public String getQueryStatement() {
    return queryStatement;
  }
}
