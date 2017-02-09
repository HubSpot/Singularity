package com.hubspot.singularity.athena;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.hubspot.singularity.OrderDirection;

public class AthenaQueryHelper {
  private static final Logger LOG = LoggerFactory.getLogger(AthenaQueryHelper.class);

  public static final List<ComparisonOperator> START_TIME_COMPARISON_OPERATORS =
      Arrays.asList(ComparisonOperator.EQUAL, ComparisonOperator.GREATER_THAN, ComparisonOperator.GREATER_THAN_OR_EQUAL_TO);
  public static final List<ComparisonOperator> END_TIME_COMPARISON_OPERATORS =
      Arrays.asList(ComparisonOperator.EQUAL, ComparisonOperator.LESS_THAN, ComparisonOperator.LESS_THAN_OR_EQUAL_TO);

  public static String createDatabaseQuery(String name) {
    return String.format("CREATE DATABASE %s", name);
  }

  // TODO
  public static String alterTableQuery(AthenaTable newTable, AthenaTable oldTable) {
    return "";
  }

  public static String createTableQuery(String database, AthenaTable table) {
    StringBuilder builder = new StringBuilder();
    builder.append("CREATE EXTERNAL TABLE IF NOT EXISTS ");
    builder.append(database);
    builder.append(".");
    builder.append(table.getName());
    builder.append(" (");
    for (AthenaField field : table.getFields()) {
        if (field.getType().isPrimitive()) {
          builder.append("`");
          builder.append(field.getName());
          builder.append("` ");
          builder.append(field.getType());
          if (!isLast(table.getFields(), field)) {
            builder.append(", ");
          }
        } else {
          throw new RuntimeException("Support for non-primitive fields not yet implemented");
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
        StringBuilder hiveFormatQueryBuilder = new StringBuilder("ROW FORMAT SERDE 'org.apache.hive.hcatalog.data.JsonSerDe' with serdeproperties ( 'paths'='");
        for (AthenaField field : table.getFields()) {
          hiveFormatQueryBuilder.append(field.getName());
          if (!isLast(table.getFields(), field)) {
            hiveFormatQueryBuilder.append(",");
          }
        }
        hiveFormatQueryBuilder.append("' )");
        return hiveFormatQueryBuilder.toString();
      case OPENX_JSON:
        return "ROW FORMAT SERDE 'org.openx.data.jsonserde.JsonSerDe' with serdeproperties ( 'serialization.format'='1' )";
      default:
        throw new RuntimeException(String.format("No implementation for data format %s", table.getDataFormat()));
    }
  }

  public static String dropTableQuery(String database, String name) {
    return String.format("DROP TABLE IF EXISTS %s.%s", database, name);
  }

  public static String loadHiveFormattedPartitionsQuery(String database, String tableName) {
    return String.format("MSCK REPAIR TABLE %s.%s", database, tableName);
  }

  public static String showPartitionsQuery(String database, String tableName) {
    return String.format("SHOW PARTITIONS %s.%s", database, tableName);
  }

  public static String generateSelectQuerySql(String database, AthenaQuery query) {
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
    builder.append(database);
    builder.append(".");
    builder.append(query.getTableName());
    if (!query.getWhereFields().isEmpty()) {
      builder.append(" WHERE ");
      for (AthenaQueryField field : query.getWhereFields()) {
        builder.append(String.format("%s %s ", field.getField(), field.getComparisonOperator().getValue()));
        if (field.getType().isRequireQuotes()) {
          builder.append("'");
        }
        builder.append(field.getValue());
        if (field.getType().isRequireQuotes()) {
          builder.append("'");
        }
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
        StringBuilder partitionBuilder = new StringBuilder("(");
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
          partitionBuilder.append(partition.getField());
          partitionBuilder.append("='");
          partitionBuilder.append(value);
          partitionBuilder.append("'");
          if (!isLast(table.getPartitions(), partition)) {
            partitionBuilder.append(",");
          }
        }
        partitionBuilder.append(") location '");
        partitionBuilder.append(locationBuilder.toString());
        partitionBuilder.append("'");
        partitions.put(partitionsWithValues, partitionBuilder.toString());
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

  public static long getTimeFromPartitionFields(List<AthenaQueryField> partitionFieldsQueried, boolean start) throws AthenaQueryException {
    Optional<String> month = Optional.absent();
    Optional<String> day = Optional.absent();
    Optional<String> year = Optional.absent();

    List<ComparisonOperator> validComparisonOperators = start ? START_TIME_COMPARISON_OPERATORS : END_TIME_COMPARISON_OPERATORS;


    for (AthenaQueryField field : partitionFieldsQueried) {
      switch (AthenaPartitionType.valueOf(field.getField().toUpperCase())) {
        case YEAR:
          if (validComparisonOperators.contains(field.getComparisonOperator())) {
            year = Optional.of(field.getValue());
          }
          break;
        case MONTH:
          if (validComparisonOperators.contains(field.getComparisonOperator())) {
            month = Optional.of(field.getValue());
          }
          break;
        case DAY:
          if (validComparisonOperators.contains(field.getComparisonOperator())) {
            day = Optional.of(field.getValue());
          }
          break;
        default:
          break;
      }
    }

    Calendar calendar = Calendar.getInstance();
    Calendar now = Calendar.getInstance();
    now.setTimeInMillis(System.currentTimeMillis());
    LOG.trace("Found year: {}, day: {}, month: {} (comparisons: {})", year, day, month, validComparisonOperators);
    if (day.isPresent() && month.isPresent() && year.isPresent()) {
      calendar.set(Integer.parseInt(year.get()), Integer.parseInt(month.get()) - 1, Integer.parseInt(day.get()));
    } else if (month.isPresent() && year.isPresent()) {
      calendar.set(Integer.parseInt(year.get()), Integer.parseInt(month.get()) - 1, 1);
      if (!start) {
        int lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        calendar.set(Integer.parseInt(year.get()), Integer.parseInt(month.get()) - 1, lastDay);
      }
    } else if (year.isPresent()) {
      calendar.set(Integer.parseInt(year.get()), start ? 0 : 11, 1);
      if (!start) {
        int lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        calendar.set(Integer.parseInt(year.get()), 11, lastDay);
      }
    } else {
      throw new AthenaQueryException("Must specify a time range to search");
    }
    if (start) {
      calendar.set(Calendar.HOUR_OF_DAY, 0);
      calendar.set(Calendar.MINUTE, 0);
      calendar.set(Calendar.SECOND, 0);
      calendar.set(Calendar.MILLISECOND, 0);
    } else {
      calendar.set(Calendar.HOUR_OF_DAY, 23);
      calendar.set(Calendar.MINUTE, 59);
      calendar.set(Calendar.SECOND, 59);
      calendar.set(Calendar.MILLISECOND, 999);
    }
    return calendar.getTimeInMillis();
  }

  public static List<AthenaQueryField> getPartitionFields(AthenaQuery query, AthenaTable table) {
    List<AthenaQueryField> partitionFieldsQueried = new ArrayList<>();
    for (AthenaQueryField field : query.getWhereFields()) {
      for (AthenaPartitionType partitionType : table.getPartitions()) {
        if (field.getField().equals(partitionType.getField())) {
          partitionFieldsQueried.add(field);
        }
      }
    }
    return partitionFieldsQueried;
  }
}
