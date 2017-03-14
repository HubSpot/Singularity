package com.hubspot.singularity.athena;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.OrderDirection;

public class AthenaQuery {
  private final String type; // TODO enum this (SELECT, SELECT DISTINCT, etc)
  private final String tableName;
  private final List<String> selectFields;
  private final List<AthenaQueryField> whereFields;
  private final Optional<OrderDirection> orderDirection;
  private final Optional<String> orderBy;
  private final Optional<Integer> limit;

  @JsonCreator
  public AthenaQuery(@JsonProperty("type") String type,
                     @JsonProperty("tableName") String tableName,
                     @JsonProperty("selectFields") List<String> selectFields,
                     @JsonProperty("whereFields") List<AthenaQueryField> whereFields,
                     @JsonProperty("orderDirection") Optional<OrderDirection> orderDirection,
                     @JsonProperty("orderBy") Optional<String> orderBy,
                     @JsonProperty("limit") Optional<Integer> limit) {
    this.type = type;
    this.tableName = tableName;
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

  @JsonIgnore
  public String getRequestId() {
    for (AthenaQueryField field : whereFields) {
      if (field.getField().equals("requestId")) {
        return field.getValue();
      }
    }
    return "";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    AthenaQuery query = (AthenaQuery) o;

    if (type != null ? !type.equals(query.type) : query.type != null) {
      return false;
    }
    if (tableName != null ? !tableName.equals(query.tableName) : query.tableName != null) {
      return false;
    }
    if (selectFields != null ? !selectFields.equals(query.selectFields) : query.selectFields != null) {
      return false;
    }
    if (whereFields != null ? !whereFields.equals(query.whereFields) : query.whereFields != null) {
      return false;
    }
    if (orderDirection != null ? !orderDirection.equals(query.orderDirection) : query.orderDirection != null) {
      return false;
    }
    if (orderBy != null ? !orderBy.equals(query.orderBy) : query.orderBy != null) {
      return false;
    }
    return limit != null ? limit.equals(query.limit) : query.limit == null;
  }

  @Override
  public int hashCode() {
    int result = type != null ? type.hashCode() : 0;
    result = 31 * result + (tableName != null ? tableName.hashCode() : 0);
    result = 31 * result + (selectFields != null ? selectFields.hashCode() : 0);
    result = 31 * result + (whereFields != null ? whereFields.hashCode() : 0);
    result = 31 * result + (orderDirection != null ? orderDirection.hashCode() : 0);
    result = 31 * result + (orderBy != null ? orderBy.hashCode() : 0);
    result = 31 * result + (limit != null ? limit.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "AthenaQuery{" +
        "type='" + type + '\'' +
        ", tableName='" + tableName + '\'' +
        ", selectFields=" + selectFields +
        ", whereFields=" + whereFields +
        ", orderDirection=" + orderDirection +
        ", orderBy=" + orderBy +
        ", limit=" + limit +
        '}';
  }
}
