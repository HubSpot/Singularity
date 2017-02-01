package com.hubspot.singularity.athena;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
      builder.append(field.toQueryFriendlyString());
      if (!isLast(table.getFields(), field)) {
        builder.append(", ");
      }
    }
    builder.append(") PARTITIONED BY (");
    for (AthenaPartitionType partition : table.getPartitions()) {
      builder.append(partition.toQueryFriendlyString());
      if (!isLast(table.getPartitions(), partition)) {
        builder.append(", ");
      }
    }
    builder.append(") ");
    builder.append(getDataFormatQuery(table));
    builder.append(" LOCATION '");
    builder.append(table.getLocation());
    builder.append("';");
    return builder.toString();
  }

  private static String getDataFormatQuery(AthenaTable table) {
    switch(table.getDataFormat()) {
      case HIVE_JSON:
        StringBuilder dataFormatQueryBuilder = new StringBuilder("ROW FORMAT SERDE 'org.apache.hive.hcatalog.data.JsonSerDe' with serdeproperties ( 'paths'='");
        for (AthenaField field : table.getFields()) {
          dataFormatQueryBuilder.append("`");
          dataFormatQueryBuilder.append(field.getName());
          dataFormatQueryBuilder.append("`");
          if (!isLast(table.getFields(), field)) {
            dataFormatQueryBuilder.append(",");
          }
        }
        dataFormatQueryBuilder.append("' )");
        return dataFormatQueryBuilder.toString();
      default:
        // TODO - more implementations
        return "";
    }
  }

  public static String dropTableQuery(String name) {
    return String.format("DROP TABLE IF EXISTS %s", name);
  }

  public static String loadHiveFormattedPartitionsQuery(String database, String tableName) {
    return String.format("MSCK REPAIR TABLE %s.%s", database, tableName);
  }

  public static String showPartitionsQuery(String database, String tableName) {
    return String.format("SHOW PARTITIONS %s.%s", database, tableName);
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
    if (!query.getWhereFields().isEmpty()) {
      builder.append(" WHERE ");
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

  public static String addPartitionQuery(String database, String tableName, String partitionStatement) {
    return String.format("ALTER TABLE %s.%s ADD PARTITION %s;", database, tableName, partitionStatement);
  }

  public static Map<List<AthenaPartitionWithValue>, String> getPartitionsWithStatements(AthenaTable table, long start, long end) {
    Map<List<AthenaPartitionWithValue>, String> partitions = new HashMap<>();
    boolean partitionByDay = table.getPartitions().contains(AthenaPartitionType.DAY);
    boolean partitionByMonth = table.getPartitions().contains(AthenaPartitionType.MONTH);

    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(start);

    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.HOUR_OF_DAY, 0);

    while (calendar.getTimeInMillis() < end) {
      for (String requestId : table.getAllowedRequestIds()) {
        StringBuilder paritionBuilder = new StringBuilder("(");
        StringBuilder locationBuilder = new StringBuilder(table.getLocation());
        List<AthenaPartitionWithValue> partitionsWithValues = new ArrayList<>();
        for (AthenaPartitionType partition : table.getPartitions()) {
          locationBuilder.append("/");
          String value = null;
          switch (partition) {
            case REQUESTID:
              value = requestId;
              locationBuilder.append(value);
              break;
            case YEAR:
              value = Integer.toString(calendar.get(Calendar.YEAR));
              locationBuilder.append(value);
              break;
            case MONTH:
              value = String.format("%02d", calendar.get(Calendar.MONTH) + 1);
              locationBuilder.append(value);
              break;
            case DAY:
              value = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH));
              locationBuilder.append(value);
              break;
          }
          partitionsWithValues.add(new AthenaPartitionWithValue(partition, value));
          paritionBuilder.append(partition.getField());
          paritionBuilder.append("='");
          paritionBuilder.append(value);
          paritionBuilder.append("'");
          if (!isLast(table.getPartitions(), partition)) {
            paritionBuilder.append(",");
          }
        }
        paritionBuilder.append(") location '");
        paritionBuilder.append(locationBuilder.toString());
        paritionBuilder.append("'");
        partitions.put(partitionsWithValues, paritionBuilder.toString());
      }

      if (partitionByDay) {
        calendar.add(Calendar.DAY_OF_YEAR, 1);
      } else if (partitionByMonth) {
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.add(Calendar.MONTH, 1);
      } else {
        calendar.set(Calendar.MONTH, 0);
        calendar.add(Calendar.YEAR, 1);
      }
    }
    return partitions;
  }
}
