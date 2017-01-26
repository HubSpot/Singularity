package com.hubspot.singularity.athena;

import java.util.List;

import com.hubspot.singularity.OrderDirection;

public class AthenaQueryBuilder {
  public static String createDatabaseQuery(String name) {
    return String.format("CREATE DATABASE %s", name);
  }

  public static String alterTableQuery(AthenaTable newTable, AthenaTable oldTable) {
    return "";
  }

  public static String createTableQuery(AthenaTable table) {
    StringBuilder builder = new StringBuilder();
    builder.append("CREATE EXTERNAL TABLE IF NOT EXISTS ");
    builder.append(table.getName());
    builder.append(" (");
    for (AthenaField field : table.getFields()) {
      builder.append(field.toTableCreateString());
      if (!isLast(table.getFields(), field)) {
        builder.append(", ");
      }
    }
    builder.append(") PARTITIONED BY (");
    for (AthenaField partition : table.getPartitions()) {
      builder.append(partition.toTableCreateString());
      if (!isLast(table.getPartitions(), partition)) {
        builder.append(", ");
      }
    }
    builder.append(") ROW FORMAT ");
    builder.append(table.getRowFormat());
    builder.append(") LOCATION '");
    builder.append(table.getLocation());
    builder.append("';");
    return builder.toString();
  }

  public static String dropTableQuery(String name) {
    return String.format("DROP TABLE IF EXISTS %s", name);
  }

  public static String loadHiveFormattedPartitionsQuery(String tableName) {
    return String.format("MSCK REPAIR TABLE %s", tableName);
  }

  public static String generateSelectQuerySql(AthenaQuery query) {
    StringBuilder builder = new StringBuilder();
    builder.append(query.getType());
    builder.append(" ");
    for (String field : query.getSelectFields()) {
      builder.append(field);
      if (!isLast(query.getSelectFields(), field)) {
        builder.append(",");
      }
    }
    builder.append(" FROM ");
    builder.append(query.getTableName());
    builder.append(" WHERE requestId = '");
    builder.append(query.getRequestId());
    builder.append("'");
    if (!query.getWhereFields().isEmpty()) {
      builder.append(" and ");
      for (AthenaQueryField field : query.getWhereFields()) {
        builder.append(field.toQueryString());
        if (!isLast(query.getWhereFields(), field)) {
          builder.append(" and ");
        }
      }
    }
    if (query.getOrderBy().isPresent()) {
      builder.append(" ORDER BY ");
      builder.append(query.getOrderBy().get());
      builder.append(" ");
      builder.append(query.getOrderDirection().or(OrderDirection.DESC));
    }
    if (query.getLimit().isPresent()) {
      builder.append(" LIMIT ");
      builder.append(query.getLimit().get());
    }
    builder.append(";");
    return builder.toString();
  }

  private static <T> boolean isLast(List<T> list, T item) {
    return (list.indexOf(item) == list.size() - 1);
  }
}
