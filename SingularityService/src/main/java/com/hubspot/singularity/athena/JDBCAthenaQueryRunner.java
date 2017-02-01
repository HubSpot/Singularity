package com.hubspot.singularity.athena;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.athena.jdbc.AthenaServiceClient;
import com.amazonaws.athena.jdbc.AthenaServiceClientConfig;
import com.amazonaws.athena.jdbc.shaded.com.amazonaws.services.athena.model.GetQueryResultsResult;
import com.amazonaws.athena.jdbc.shaded.com.amazonaws.services.athena.model.QueryExecutionStatus;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.config.AthenaConfig;

public class JDBCAthenaQueryRunner implements AthenaQueryRunner {
  private static final Logger LOG = LoggerFactory.getLogger(JDBCAthenaQueryRunner.class);

  private final AthenaConfig athenaConfig;
  private final AthenaServiceClient athenaClient;

  @Inject
  public JDBCAthenaQueryRunner(Optional<AthenaConfig> athenaConfig) throws SQLException {
    this.athenaConfig = athenaConfig.get();
    AthenaServiceClientConfig clientConfig = new AthenaServiceClientConfig();
    clientConfig.setAwsAccessId(athenaConfig.get().getS3AccessKey());
    clientConfig.setAwsSecretKey(athenaConfig.get().getS3SecretKey());
    clientConfig.setS3StagingDir(String.format("s3://%s/%s", athenaConfig.get().getS3StagingBucket(), athenaConfig.get().getS3StagingPrefix()));
    this.athenaClient = new AthenaServiceClient(clientConfig, athenaConfig.get().getAthenaUrl());
  }

  public GetQueryResultsResult getQueryResults(String queryExecutionId, String token, int pageSize) throws AthenaQueryException {
    try {
      return athenaClient.fetchQueryResult(queryExecutionId, token, pageSize);
    } catch (AmazonClientException ace) {
      LOG.error("Error getting query results", ace);
      throw new AthenaQueryException(ace);
    }
  }

  public String runQuery(String sql) throws AthenaQueryException {
    try {
      return athenaClient.runQuery(sql, athenaConfig.getDefaultSchema());
    } catch (AmazonClientException ace) {
      LOG.error("Error starting query", ace);
      throw new AthenaQueryException(ace);
    }
  }

  public QueryExecutionStatus getQueryExecutionStatus(String queryExecutionId) throws AthenaQueryException {
    try {
      return athenaClient.getQueryExecutionStatus(queryExecutionId);
    } catch (AmazonClientException ace) {
      LOG.error("Error getting query status", ace);
      throw new AthenaQueryException(ace);
    }
  }

  public void cancelQuery(String queryExecutionId) throws AthenaQueryException {
    try {
      athenaClient.cancelQuery(queryExecutionId);
    } catch (AmazonClientException ace) {
      LOG.error("Error canceling query", ace);
      throw new AthenaQueryException(ace);
    }
  }
}
