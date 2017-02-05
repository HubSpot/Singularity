package com.hubspot.singularity.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.athena.jdbc.shaded.com.amazonaws.services.athena.model.ColumnInfo;
import com.amazonaws.athena.jdbc.shaded.com.amazonaws.services.athena.model.GetQueryResultsResult;
import com.amazonaws.athena.jdbc.shaded.com.amazonaws.services.athena.model.QueryExecutionStatus;
import com.amazonaws.athena.jdbc.shaded.com.amazonaws.services.athena.model.ResultRow;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.athena.AthenaField;
import com.hubspot.singularity.athena.AthenaFieldType;
import com.hubspot.singularity.athena.AthenaModule;
import com.hubspot.singularity.athena.AthenaPartitionType;
import com.hubspot.singularity.athena.AthenaPartitionWithValue;
import com.hubspot.singularity.athena.AthenaQuery;
import com.hubspot.singularity.athena.AthenaQueryBuilder;
import com.hubspot.singularity.athena.AthenaQueryException;
import com.hubspot.singularity.athena.AthenaQueryField;
import com.hubspot.singularity.athena.AthenaQueryInfo;
import com.hubspot.singularity.athena.AthenaQueryResults;
import com.hubspot.singularity.athena.AthenaQueryRunner;
import com.hubspot.singularity.athena.AthenaQueryStatus;
import com.hubspot.singularity.athena.AthenaTable;
import com.hubspot.singularity.athena.ComparisonOperator;
import com.hubspot.singularity.config.AthenaConfig;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.Transcoder;

public class AthenaQueryManager extends CuratorAsyncManager {
  private static final Logger LOG = LoggerFactory.getLogger(AthenaQueryManager.class);

  private final Transcoder<AthenaQueryInfo> queryResultTranscoder;
  private final Transcoder<AthenaTable> tableTranscoder;
  private final AthenaQueryRunner queryRunner;
  private final Optional<AthenaConfig> athenaConfig;
  private final ListeningExecutorService queryExecutorService;

  private static final String DEFAULT_QUERY_USER = "default";
  private static final String ATHENA_ROOT = "/athena";
  private static final String QUERIES_PATH = ATHENA_ROOT + "/queries";
  private static final String TABLES_PATH = ATHENA_ROOT + "/tables";

  private static final String PARTITION_FIELD_NAME = "partition";

  private static final List<ComparisonOperator> START_TIME_COMPARISON_OPERATORS =
      Arrays.asList(ComparisonOperator.EQUAL, ComparisonOperator.GREATER_THAN, ComparisonOperator.GREATER_THAN_OR_EQUAL_TO);
  private static final List<ComparisonOperator> END_TIME_COMPARISON_OPERATORS =
      Arrays.asList(ComparisonOperator.EQUAL, ComparisonOperator.LESS_THAN, ComparisonOperator.LESS_THAN_OR_EQUAL_TO);

  @Inject
  public AthenaQueryManager(CuratorFramework curator,
                            SingularityConfiguration configuration,
                            MetricRegistry metricRegistry,
                            Transcoder<AthenaQueryInfo> queryResultTranscoder,
                            Transcoder<AthenaTable> tableTranscoder,
                            AthenaQueryRunner queryRunner,
                            Optional<AthenaConfig> athenaConfig,
                            @Named(AthenaModule.ATHENA_QUERY_EXECUTOR) ListeningExecutorService queryExecutorService) {
    super(curator, configuration, metricRegistry);
    this.queryResultTranscoder = queryResultTranscoder;
    this.tableTranscoder = tableTranscoder;
    this.queryRunner = queryRunner;
    this.athenaConfig = athenaConfig;
    this.queryExecutorService = queryExecutorService;
  }

  // ZK Operations
  private String getUserIdFoprQuery(Optional<SingularityUser> user) {
    return user.isPresent() ? user.get().getId() : DEFAULT_QUERY_USER;
  }

  private String getQueryResultPath(Optional<SingularityUser> user, String id) {
    return ZKPaths.makePath(QUERIES_PATH, getUserIdFoprQuery(user), id);
  }

  private String getQueryResultsForUserPath(Optional<SingularityUser> user) {
    return ZKPaths.makePath(QUERIES_PATH, getUserIdFoprQuery(user));
  }

  public Optional<AthenaQueryInfo> getQueryInfo(Optional<SingularityUser> user, String id) {
    return getData(getQueryResultPath(user, id), queryResultTranscoder);
  }

  public List<AthenaQueryInfo> getQueriesForUser(Optional<SingularityUser> user) {
    return getAsyncChildren(getQueryResultsForUserPath(user), queryResultTranscoder);
  }

  public SingularityCreateResult saveQueryResult(Optional<SingularityUser> user, AthenaQueryInfo queryResult) {
    return save(getQueryResultPath(user, queryResult.getId()), queryResult, queryResultTranscoder);
  }

  private String getTablePath(String name) {
    return ZKPaths.makePath(TABLES_PATH, name);
  }

  public Optional<AthenaTable> getTable(String name) {
    return getData(getTablePath(name), tableTranscoder);
  }

  public List<AthenaTable> getTables() {
    return getAsyncChildren(TABLES_PATH, tableTranscoder);
  }

  public SingularityCreateResult saveTable(AthenaTable table) {
    return save(getTablePath(table.getName()), table, tableTranscoder);
  }

  public SingularityDeleteResult removeTable(String name) {
    return delete(getTablePath(name));
  }

  // Query Methods
  public AthenaTable createTableThrows(final Optional<SingularityUser> user, AthenaTable table) throws Exception {
    String sql;
    Optional<AthenaTable> maybeExistingTable = getTable(table.getName());
    if (maybeExistingTable.isPresent()) {
      sql = AthenaQueryBuilder.alterTableQuery(table, maybeExistingTable.get());
    } else {
      sql = AthenaQueryBuilder.createTableQuery(table);
    }
    AthenaQueryInfo result = runQuery(user, sql);
    if (result.getStatus() != AthenaQueryStatus.SUCCEEDED) {
      throw new AthenaQueryException(String.format("Query %s failed (%s)", sql, result.getExceptionMessage()));
    }

    return table;
  }

  public void deleteTable(final Optional<SingularityUser> user, final String name) throws Exception {
    String sql = AthenaQueryBuilder.dropTableQuery(name);
    AthenaQueryInfo result = runQuery(user, sql);
    if (result.getStatus() != AthenaQueryStatus.SUCCEEDED) {
      throw new AthenaQueryException(String.format("Query %s failed (%s)", sql, result.getExceptionMessage()));
    }
  }

  // Partitions
  public boolean updatePartitions(final Optional<SingularityUser> user, AthenaTable table, long start, Optional<Long> maybeEnd) throws AthenaQueryException {
    long end = maybeEnd.or(System.currentTimeMillis());
    AthenaQueryInfo showPartitionsQuery = runQuery(user, AthenaQueryBuilder.showPartitionsQuery(athenaConfig.get().getDatabaseName(), table.getName()));
    Set<List<AthenaPartitionWithValue>> partitions = new HashSet<>();
    Optional<AthenaQueryResults> showPartitionsResults = getQueryResults(user, showPartitionsQuery.getQueryExecutionId(), null, 10);
    while (showPartitionsResults.isPresent() && (showPartitionsResults.get().getNextToken() != null || !showPartitionsResults.get().getResults().isEmpty())) {
      for (Map<String, String> result : showPartitionsResults.get().getResults()) {
        if (result.containsKey(PARTITION_FIELD_NAME)) {
          partitions.add(fromShowPartitionsResult(result.get(PARTITION_FIELD_NAME)));
        }
      }
    }

    Map<List<AthenaPartitionWithValue>, String> partitionsToStatements = AthenaQueryBuilder.getPartitionsWithStatements(table, start, end);
    Set<List<AthenaPartitionWithValue>> partitionsToAdd = partitionsToStatements.keySet();
    partitionsToAdd.removeAll(partitions);

    boolean updatedAll = true;
    for (List<AthenaPartitionWithValue> partitionToAdd : partitionsToAdd) {
      // TODO - Run in parallel
      AthenaQueryInfo queryInfo = runQuery(user, AthenaQueryBuilder.addPartitionQuery(athenaConfig.get().getDatabaseName(), table.getName(), partitionsToStatements.get(partitionToAdd)));
      if (queryInfo.getStatus() != AthenaQueryStatus.SUCCEEDED) {
        updatedAll = false;
      }
    }

    return updatedAll;
  }

  private List<AthenaPartitionWithValue> fromShowPartitionsResult(String showPartitionResult) {
    List<String> partitions = Arrays.asList(showPartitionResult.split("/"));
    List<AthenaPartitionWithValue> partitionsWithValues = new ArrayList<>();
    for (String partition : partitions) {
      String[] split = partition.split("=");
      partitionsWithValues.add(new AthenaPartitionWithValue(AthenaPartitionType.valueOf(split[0].toUpperCase()), split[1]));
    }
    return partitionsWithValues;
  }

  // Athena Query Running
  public AthenaQueryInfo runQueryAsync(Optional<SingularityUser> user, AthenaQuery query) {
    String singularityId = UUID.randomUUID().toString();
    final String sql = AthenaQueryBuilder.generateSelectQuerySql(query);
    AthenaQueryInfo result = new AthenaQueryInfo(singularityId, null, sql, AthenaQueryStatus.UPDATING_PARTITIONS, Collections.<AthenaField>emptyList(), Optional.<String>absent());
    saveQueryResult(user, result);
    startParitionUpdateAndRunQuery(user, singularityId, query, sql);
    return result;
  }

  private void startParitionUpdateAndRunQuery(final Optional<SingularityUser> user, final String singularityId, final AthenaQuery query, final String sql) {
    ListenableFuture<Boolean> partitionUpdate = queryExecutorService.submit(updatePartitionsCallable(user, singularityId, query, sql));
    Futures.addCallback(partitionUpdate, new FutureCallback<Boolean>() {
      @Override
      public void onSuccess(@Nullable Boolean result) {
        if (result != null && result) {
          try {
            runRawQueryAsync(user, sql, singularityId);
          } catch (AthenaQueryException aqe) {
            LOG.error("Error running query {} with singularity id {}", sql, singularityId, aqe);
            saveQueryResult(user, new AthenaQueryInfo(singularityId, null, sql, AthenaQueryStatus.FAILED, Collections.<AthenaField>emptyList(), Optional.of(aqe.getMessage())));
          }
        } else {
          LOG.error("Could not update partitions for query {} with singularity id {}", sql, singularityId);
          saveQueryResult(user, new AthenaQueryInfo(singularityId, null, sql, AthenaQueryStatus.FAILED, Collections.<AthenaField>emptyList(), Optional.of("Could not update partitions")));
        }
      }

      @Override
      public void onFailure(Throwable t) {
        LOG.error("Error running query {} with singularity id {}", sql, singularityId, t);
        saveQueryResult(user, new AthenaQueryInfo(singularityId, null, sql, AthenaQueryStatus.FAILED, Collections.<AthenaField>emptyList(), Optional.of(t.getMessage())));
      }
    });
  }

  public AthenaQueryInfo runRawQueryAsync(final Optional<SingularityUser> user, final String sql, final String singularityId) throws AthenaQueryException {
    AthenaQueryInfo result = new AthenaQueryInfo(singularityId, queryRunner.runQuery(sql), sql, AthenaQueryStatus.RUNNING, Collections.<AthenaField>emptyList(), Optional.<String>absent());
    saveQueryResult(user, result);
    queryExecutorService.submit(watchQueryCallable(user, singularityId, result.getQueryExecutionId(), result.getSql()));
    return result;
  }

  public AthenaQueryInfo runQuery(final Optional<SingularityUser> user, final String sql) throws AthenaQueryException {
    String singularityId = UUID.randomUUID().toString();
    String queryExecutionId = queryRunner.runQuery(sql);
    try {
      return queryExecutorService.submit(watchQueryCallable(user, singularityId, queryExecutionId, sql)).get();
    } catch (InterruptedException ie) {
      LOG.error("Interrupted while running query {} with id {}", sql, queryExecutionId, ie);
      return new AthenaQueryInfo(singularityId, queryExecutionId, sql, AthenaQueryStatus.FAILED, Collections.<AthenaField>emptyList(), Optional.of(ie.getMessage()));
    } catch (ExecutionException ee) {
      LOG.error("Error running query {} with id {}", sql, queryExecutionId, ee);
      return new AthenaQueryInfo(singularityId, queryExecutionId, sql, AthenaQueryStatus.FAILED, Collections.<AthenaField>emptyList(), Optional.of(ee.getMessage()));
    }
  }

  private Callable<AthenaQueryInfo> watchQueryCallable(final Optional<SingularityUser> user, final String singularityId, final String queryExecutionId, final String sql) {
    return new Callable<AthenaQueryInfo>() {
      @Override
      public AthenaQueryInfo call() throws Exception {
        try {
          QueryExecutionStatus status = queryRunner.getQueryExecutionStatus(queryExecutionId);
          while (status.getState().equals("RUNNING") || status.getState().equals("SUBMITTED")) {
            Thread.sleep(1000);
            status = queryRunner.getQueryExecutionStatus(queryExecutionId);
          }
          AthenaQueryInfo result;
          switch (status.getState()) {
            case "SUCCEEDED":
              List<AthenaField> fields = fieldsFromColumnInfos(queryRunner.getQueryResults(queryExecutionId, null, 1).getResultSet().getColumnInfos());
              result = new AthenaQueryInfo(singularityId, queryExecutionId, sql, AthenaQueryStatus.SUCCEEDED, fields, Optional.<String>absent());
              saveQueryResult(user, result);
              break;
            case "FAILED":
            case "CANCELED":
            default:
              result = new AthenaQueryInfo(singularityId, queryExecutionId, sql, AthenaQueryStatus.FAILED, Collections.<AthenaField>emptyList(), Optional.of(status.getStateChangeReason()));
              saveQueryResult(user, result);
              break;
          }
          return result;
        } catch (Exception e) {
          LOG.error("Error waiting for query result with id {}", queryExecutionId, e);
          try {
            queryRunner.cancelQuery(queryExecutionId);
          } catch (Exception cancelException) {
            LOG.error("Could not cancel query {}", queryExecutionId, cancelException);
          }
          AthenaQueryInfo result = new AthenaQueryInfo(singularityId, queryExecutionId, sql, AthenaQueryStatus.FAILED, Collections.<AthenaField>emptyList(), Optional.of(e.getMessage()));
          saveQueryResult(user, result);
          return result;
        }
      }
    };
  }

  private Callable<Boolean> updatePartitionsCallable(final Optional<SingularityUser> user, final String singularityId, final AthenaQuery query, final String sql) {
    return new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        Optional<AthenaTable> maybeExistingTable = getTable(query.getTableName());
        if (!maybeExistingTable.isPresent()) {
          return false;
        }

        List<AthenaQueryField> partitionFieldsQueried = getPartitionFields(query, maybeExistingTable.get());
        long start = getTimeFromPartitionFields(partitionFieldsQueried, START_TIME_COMPARISON_OPERATORS);
        long end = getTimeFromPartitionFields(partitionFieldsQueried, END_TIME_COMPARISON_OPERATORS);
        return updatePartitions(user, maybeExistingTable.get(), start, Optional.of(end));
      }
    };
  }

  private long getTimeFromPartitionFields(List<AthenaQueryField> partitionFieldsQueried, List<ComparisonOperator> validComparisonOperators) throws AthenaQueryException {
    Optional<String> month = Optional.absent();
    Optional<String> day = Optional.absent();
    Optional<String> year = Optional.absent();


    for (AthenaQueryField field : partitionFieldsQueried) {
      switch (AthenaPartitionType.valueOf(field.getField())) {
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
    if (day.isPresent() && month.isPresent() && year.isPresent()) {
      calendar.set(Integer.parseInt(year.get()), Integer.parseInt(month.get()), Integer.parseInt(day.get()));
    } else if (month.isPresent() && year.isPresent()) {
      calendar.set(Integer.parseInt(year.get()), Integer.parseInt(month.get()), 0);
    } else if (year.isPresent()) {
      calendar.set(Integer.parseInt(year.get()), 0, 0);
    } else {
      throw new AthenaQueryException("Must specify a time range to search");
    }
    return calendar.getTimeInMillis();
  }

  private List<AthenaQueryField> getPartitionFields(AthenaQuery query, AthenaTable table) {
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

  private List<AthenaField> fieldsFromColumnInfos(List<ColumnInfo> columnInfos) {
    List<AthenaField> fields = new ArrayList<>();
    for (ColumnInfo columnInfo : columnInfos) {
      fields.add(new AthenaField(columnInfo.getName(), AthenaFieldType.valueOf(columnInfo.getType().toUpperCase()), columnInfo.getLabel()));
    }
    return fields;
  }

  public Optional<AthenaQueryResults> getQueryResults(Optional<SingularityUser> user, String queryExecutionId, String token, int pageSize) throws AthenaQueryException {
    Optional<AthenaQueryInfo> queryInfo = getQueryInfo(user, queryExecutionId);
    // TODO - separate http statuses here
    if (!queryInfo.isPresent()) {
      return Optional.absent();
    } else if (queryInfo.get().getStatus() == AthenaQueryStatus.FAILED) {
      throw new AthenaQueryException(String.format("Cannot get results of failed query. Failed due to: %s", queryInfo.get().getExceptionMessage()));
    } else if (queryInfo.get().getStatus() == AthenaQueryStatus.RUNNING) {
      throw new AthenaQueryException(String.format("Query %s is still running, try again later", queryExecutionId));
    }

    GetQueryResultsResult resultsResult = queryRunner.getQueryResults(queryExecutionId, token, pageSize);
    List<AthenaField> fields = fieldsFromColumnInfos(resultsResult.getResultSet().getColumnInfos());
    List<Map<String, String>> results = new ArrayList<>();
    int index = 0;
    for (ResultRow resultRow : resultsResult.getResultSet().getResultRows()) {
      Map<String, String> rowData = new HashMap<>();
      for (String value : resultRow.getData()) {
        rowData.put(fields.get(index).getName(), value);
      }
      results.add(rowData);
    }
    return Optional.of(new AthenaQueryResults(queryInfo.get(), results, pageSize, token, resultsResult.getNextToken()));
  }
}
