package com.hubspot.singularity.athena;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.OrderDirection;

public class AthenaQuery {
  private final String type; // TODO enum this (SELECT, SELECT DISTINCT, etc)
  private final String tableName;
  private final String requestId;
  private final List<String> selectFields;
  private final List<AthenaQueryField> whereFields;
  private final Optional<OrderDirection> orderDirection;
  private final Optional<String> orderBy;
  private final Optional<Integer> limit;

  @JsonCreator
  public AthenaQuery(@JsonProperty("type") String type,
                     @JsonProperty("tableName") String tableName,
                     @JsonProperty("requestId") String requestId,
                     @JsonProperty("selectFields") List<String> selectFields,
                     @JsonProperty("whereFields") List<AthenaQueryField> whereFields,
                     @JsonProperty("orderDirection") Optional<OrderDirection> orderDirection,
                     @JsonProperty("orderBy") Optional<String> orderBy,
                     @JsonProperty("limit") Optional<Integer> limit) {
    this.type = type;
    this.tableName = tableName;
    this.requestId = requestId;
    this.selectFields = selectFields;
    this.whereFields = whereFields;
    this.orderDirection = orderDirection;
    this.orderBy = orderBy;
    this.limit = limit;
  }

  public String getType() {
    return type;
  }

  public String getTableName() {
    return tableName;
  }

  public String getRequestId() {
    return requestId;
  }

  public List<String> getSelectFields() {
    return selectFields;
  }

  public List<AthenaQueryField> getWhereFields() {
    return whereFields;
  }

  public Optional<OrderDirection> getOrderDirection() {
    return orderDirection;
  }

  public Optional<String> getOrderBy() {
    return orderBy;
  }

  public Optional<Integer> getLimit() {
    return limit;
  }
}
