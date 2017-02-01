package com.hubspot.singularity.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

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
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.athena.AthenaField;
import com.hubspot.singularity.athena.AthenaModule;
import com.hubspot.singularity.athena.AthenaPartitionType;
import com.hubspot.singularity.athena.AthenaPartitionWithValue;
import com.hubspot.singularity.athena.AthenaQuery;
import com.hubspot.singularity.athena.AthenaQueryBuilder;
import com.hubspot.singularity.athena.AthenaQueryException;
import com.hubspot.singularity.athena.AthenaQueryInfo;
import com.hubspot.singularity.athena.AthenaQueryResults;
import com.hubspot.singularity.athena.AthenaQueryRunner;
import com.hubspot.singularity.athena.AthenaQueryStatus;
import com.hubspot.singularity.athena.AthenaTable;
import com.hubspot.singularity.config.AthenaConfig;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.Transcoder;

public class AthenaQueryManager extends CuratorAsyncManager {
  private static final Logger LOG = LoggerFactory.getLogger(AthenaQueryManager.class);

  private final Transcoder<AthenaQueryInfo> queryResultTranscoder;
  private final Transcoder<AthenaTable> tableTranscoder;
  private final AthenaQueryRunner queryRunner;
  private final Optional<AthenaConfig> athenaConfig;
  private final ExecutorService queryExecutorService;

  private static final String DEFAULT_QUERY_USER = "default";
  private static final String ATHENA_ROOT = "/athena";
  private static final String QUERIES_PATH = ATHENA_ROOT + "/queries";
  private static final String TABLES_PATH = ATHENA_ROOT + "/tables";

  private static final String PARTITION_FIELD_NAME = "partition";


  @Inject
  public AthenaQueryManager(CuratorFramework curator,
                            SingularityConfiguration configuration,
                            MetricRegistry metricRegistry,
                            Transcoder<AthenaQueryInfo> queryResultTranscoder,
                            Transcoder<AthenaTable> tableTranscoder,
                            AthenaQueryRunner queryRunner,
                            Optional<AthenaConfig> athenaConfig,
                            @Named(AthenaModule.ATHENA_QUERY_EXECUTOR) ExecutorService queryExecutorService) {
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
    return save(getQueryResultPath(user, queryResult.getQueryExecutionId()), queryResult, queryResultTranscoder);
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
  public Optional<AthenaTable> updatePartitions(final Optional<SingularityUser> user, String tableName, long start, long end) throws AthenaQueryException {
    Optional<AthenaTable> maybeExistingTable = getTable(tableName);
    if (!maybeExistingTable.isPresent()) {
      return maybeExistingTable;
    }
    AthenaTable table = maybeExistingTable.get();
    AthenaQueryInfo showPartitionsQuery = runQuery(user, AthenaQueryBuilder.showPartitionsQuery(athenaConfig.get().getDatabaseName(), tableName));
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

    for (List<AthenaPartitionWithValue> partitionToAdd : partitionsToAdd) {
      // TODO - Run in parallel
      runQuery(user, AthenaQueryBuilder.addPartitionQuery(athenaConfig.get().getDatabaseName(), tableName, partitionsToStatements.get(partitionToAdd)));
    }

    return Optional.absent();
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
    // TODO - update partitions + run query
    String sql = AthenaQueryBuilder.generateSelectQuerySql(query);
    return null;
  }

  public AthenaQueryInfo runRawQueryAsync(final Optional<SingularityUser> user, final String sql) throws AthenaQueryException {
    AthenaQueryInfo result = new AthenaQueryInfo(queryRunner.runQuery(sql), sql, AthenaQueryStatus.RUNNING, Collections.<AthenaField>emptyList(), Optional.<String>absent());
    saveQueryResult(user, result);
    queryExecutorService.submit(watchQueryCallable(user, result.getQueryExecutionId(), result.getSql()));
    return result;
  }

  public AthenaQueryInfo runQuery(final Optional<SingularityUser> user, final String sql) throws AthenaQueryException {
    String queryExecutionId = queryRunner.runQuery(sql);
    try {
      return queryExecutorService.submit(watchQueryCallable(user, queryExecutionId, sql)).get();
    } catch (InterruptedException ie) {
      LOG.error("Interrupted while running query {} with id {}", sql, queryExecutionId, ie);
      return new AthenaQueryInfo(queryExecutionId, sql, AthenaQueryStatus.FAILED, Collections.<AthenaField>emptyList(), Optional.of(ie.getMessage()));
    } catch (ExecutionException ee) {
      LOG.error("Error running query {} with id {}", sql, queryExecutionId, ee);
      return new AthenaQueryInfo(queryExecutionId, sql, AthenaQueryStatus.FAILED, Collections.<AthenaField>emptyList(), Optional.of(ee.getMessage()));
    }
  }

  private Callable<AthenaQueryInfo> watchQueryCallable(final Optional<SingularityUser> user, final String queryExecutionId, final String sql) {
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
              result = new AthenaQueryInfo(queryExecutionId, sql, AthenaQueryStatus.SUCCEEDED, fields, Optional.<String>absent());
              saveQueryResult(user, result);
              break;
            case "FAILED":
            case "CANCELED":
            default:
              result = new AthenaQueryInfo(queryExecutionId, sql, AthenaQueryStatus.FAILED, Collections.<AthenaField>emptyList(), Optional.of(status.getStateChangeReason()));
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
          AthenaQueryInfo result = new AthenaQueryInfo(queryExecutionId, sql, AthenaQueryStatus.FAILED, Collections.<AthenaField>emptyList(), Optional.of(e.getMessage()));
          saveQueryResult(user, result);
          return result;
        }
      }
    };
  }

  private List<AthenaField> fieldsFromColumnInfos(List<ColumnInfo> columnInfos) {
    List<AthenaField> fields = new ArrayList<>();
    for (ColumnInfo columnInfo : columnInfos) {
      fields.add(new AthenaField(columnInfo.getName(), columnInfo.getType(), columnInfo.getLabel()));
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
