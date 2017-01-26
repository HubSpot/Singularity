package com.hubspot.singularity.data;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.athena.jdbc.AthenaConnection;
import com.amazonaws.athena.jdbc.AthenaResultSet;
import com.amazonaws.athena.jdbc.AthenaStatement;
import com.amazonaws.athena.jdbc.AthenaStatementClient;
import com.amazonaws.athena.jdbc.shaded.com.amazonaws.services.athena.model.QueryExecutionState;
import com.amazonaws.athena.jdbc.shaded.guava.base.Strings;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.athena.AthenaConnectionProvider;
import com.hubspot.singularity.athena.AthenaModule;
import com.hubspot.singularity.athena.AthenaQueryBuilder;
import com.hubspot.singularity.athena.AthenaQueryException;
import com.hubspot.singularity.athena.AthenaQueryResult;
import com.hubspot.singularity.athena.AthenaQueryStatus;
import com.hubspot.singularity.athena.AthenaTable;
import com.hubspot.singularity.config.AthenaConfig;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.Transcoder;

public class AthenaQueryManager extends CuratorAsyncManager {
  private static final Logger LOG = LoggerFactory.getLogger(AthenaQueryManager.class);

  private final Transcoder<AthenaQueryResult> queryResultTranscoder;
  private final Transcoder<AthenaTable> tableTranscoder;
  private final AthenaConnectionProvider queryConnectionProvider;
  private final Optional<AthenaConfig> athenaConfig;
  private final ExecutorService queryExecutorService;

  private static final String DEFAULT_QUERY_USER = "default";
  private static final String ATHENA_ROOT = "/athena";
  private static final String QUERIES_PATH = ATHENA_ROOT + "/queries";
  private static final String TABLES_PATH = ATHENA_ROOT + "/tables";


  @Inject
  public AthenaQueryManager(CuratorFramework curator,
                            SingularityConfiguration configuration,
                            MetricRegistry metricRegistry,
                            Transcoder<AthenaQueryResult> queryResultTranscoder,
                            Transcoder<AthenaTable> tableTranscoder,
                            AthenaConnectionProvider queryConnectionProvider,
                            @Named(AthenaModule.ATHENA_QUERY_EXECUTOR) ExecutorService queryExecutorService) {
    super(curator, configuration, metricRegistry);
    this.queryResultTranscoder = queryResultTranscoder;
    this.tableTranscoder = tableTranscoder;
    this.queryConnectionProvider = queryConnectionProvider;
    this.athenaConfig = configuration.getAthenaConfig();
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

  public Optional<AthenaQueryResult> getQueryResult(Optional<SingularityUser> user, String id) {
    return getData(getQueryResultPath(user, id), queryResultTranscoder);
  }

  public List<AthenaQueryResult> getQueriesForUser(Optional<SingularityUser> user) {
    return getAsyncChildren(getQueryResultsForUserPath(user), queryResultTranscoder);
  }

  public SingularityCreateResult saveQueryResult(Optional<SingularityUser> user, AthenaQueryResult queryResult) {
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

  public SingularityDeleteResult dremoveTable(String name) {
    return delete(getTablePath(name));
  }

  // Query Methods
  public AthenaTable createTableThrows(AthenaTable table) throws Exception {
    String sql;
    Optional<AthenaTable> maybeExistingTable = getTable(table.getName());
    if (maybeExistingTable.isPresent()) {
      sql = AthenaQueryBuilder.alterTableQuery(table, maybeExistingTable.get());
    } else {
      sql = AthenaQueryBuilder.createTableQuery(table);
    }
    AthenaResultSet resultSet = runQuery(sql);
    AthenaStatementClient client = (AthenaStatementClient) resultSet.getClient();
    if (client.getQueryState() != QueryExecutionState.SUCCEEDED) {
      throw new AthenaQueryException(String.format("Query %s failed (%s)", sql, client.getQueryStatus().getStateChangeReason()));
    }

    return table;
  }

  public void deleteTable(String name) throws Exception {
    String sql = AthenaQueryBuilder.dropTableQuery(name);
    AthenaResultSet resultSet = runQuery(sql);
    AthenaStatementClient client = (AthenaStatementClient) resultSet.getClient();
    if (client.getQueryState() != QueryExecutionState.SUCCEEDED) {
      throw new AthenaQueryException(String.format("Query %s failed (%s)", sql, client.getQueryStatus().getStateChangeReason()));
    }
  }

  public Optional<AthenaTable> updatePartitions(String tableName, long start, long end) {
    Optional<AthenaTable> maybeExistingTable = getTable(tableName);
    if (!maybeExistingTable.isPresent()) {
      return maybeExistingTable;
    }

    // TODO - generate partitions in date range and based on listing of s3 bucket
    //   1. List partitions already active in table
    //   2. List keys in s3 based on format (important to find list fo requestIds
    //   3. generate all needed partitions to be added based on dates provided
    //   4. run query in batches to update partitions
    return Optional.absent();
  }

  // Athena Query Running
  public AthenaQueryResult runQueryAsync(final Optional<SingularityUser> user, final String sql) {
    final long start = System.currentTimeMillis();
    final String singularityQueryId = UUID.randomUUID().toString();
    ListenableFuture<AthenaResultSet> queryFuture = MoreExecutors
        .listeningDecorator(queryExecutorService)
        .submit(getQueryCallable(sql));

    Futures.addCallback(queryFuture, new FutureCallback<AthenaResultSet>() {
      @Override
      public void onSuccess(AthenaResultSet resultSet) {
        AthenaStatementClient client = (AthenaStatementClient) resultSet.getClient();
        AthenaQueryStatus status = client.getQueryState() == QueryExecutionState.SUCCEEDED ? AthenaQueryStatus.SUCCEEDED : AthenaQueryStatus.FAILED;
        String athenaQueryId = client.getQueryExecutionId();
        saveQueryResult(user,
            new AthenaQueryResult(
                singularityQueryId,
                Optional.of(athenaQueryId),
                sql,
                Optional.of(athenaConfig.get().getS3StagingBucket()),
                  Optional.of(generateResultPrefix(athenaQueryId)),
                status,
                Optional.<String>absent()));
        LOG.info("Ran query {} in {}ms", sql, System.currentTimeMillis() - start);
      }

      @Override
      public void onFailure(Throwable throwable) {
        LOG.error("Exception running query {}", sql, throwable);
        saveQueryResult(user, new AthenaQueryResult(singularityQueryId, Optional.<String>absent(), sql, Optional.<String>absent(), Optional.<String>absent(), AthenaQueryStatus.FAILED, Optional.of(throwable.getMessage())));
      }
    });

    return new AthenaQueryResult(singularityQueryId, Optional.<String>absent(), sql, Optional.<String>absent(), Optional.<String>absent(), AthenaQueryStatus.RUNNING, Optional.<String>absent());
  }

  public AthenaResultSet runQuery(final String sql) throws Exception {
    try (final AthenaConnection conn = queryConnectionProvider.getAthenaConnection()) {
      try (final AthenaStatement statement = (AthenaStatement) conn.createStatement()) {
        return (AthenaResultSet) statement.executeQuery(sql);
      } catch (Exception e) {
        LOG.error("Exception running query {}", sql, e);
        throw e;
      }
    } catch (Exception e) {
      LOG.error("Exception running query {}", sql, e);
      throw e;
    }
  }

  private String generateResultPrefix(String athenaQueryId) {
    if (Strings.isNullOrEmpty(athenaConfig.get().getS3StagingPrefix())) {
      return athenaQueryId;
    } else {
      return String.format("%s/%s", athenaConfig.get().getS3StagingPrefix(), athenaQueryId);
    }
  }

  private Callable<AthenaResultSet> getQueryCallable(final String sql) {
    return new Callable<AthenaResultSet>() {
      @Override
      public AthenaResultSet call() throws Exception {
        return runQuery(sql);
      }
    };
  }
}
