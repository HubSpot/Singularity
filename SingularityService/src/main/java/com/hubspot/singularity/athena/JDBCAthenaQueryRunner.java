package com.hubspot.singularity.athena;

import java.sql.SQLException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.HttpMethod;
import com.amazonaws.athena.jdbc.AthenaServiceClient;
import com.amazonaws.athena.jdbc.AthenaServiceClientConfig;
import com.amazonaws.athena.jdbc.shaded.com.amazonaws.services.athena.model.GetQueryResultsResult;
import com.amazonaws.athena.jdbc.shaded.com.amazonaws.services.athena.model.QueryExecutionStatus;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.config.AthenaConfig;

public class JDBCAthenaQueryRunner implements AthenaQueryRunner {
  private static final Logger LOG = LoggerFactory.getLogger(JDBCAthenaQueryRunner.class);

  private final AthenaConfig athenaConfig;
  private final AthenaServiceClient athenaClient;
  private final AmazonS3 s3Client;

  @Inject
  public JDBCAthenaQueryRunner(Optional<AthenaConfig> athenaConfig) throws SQLException {
    this.athenaConfig = athenaConfig.get();
    AthenaServiceClientConfig clientConfig = new AthenaServiceClientConfig();
    clientConfig.setAwsAccessId(athenaConfig.get().getS3AccessKey());
    clientConfig.setAwsSecretKey(athenaConfig.get().getS3SecretKey());
    clientConfig.setS3StagingDir(String.format("s3://%s/%s", athenaConfig.get().getS3StagingBucket(), athenaConfig.get().getS3StagingPrefix()));
    this.athenaClient = new AthenaServiceClient(clientConfig, athenaConfig.get().getAthenaUrl());
    this.s3Client = new AmazonS3Client(new BasicAWSCredentials(athenaConfig.get().getS3AccessKey(), athenaConfig.get().getS3SecretKey()));
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

  public boolean isPartitionPathValid(String bucket, String prefix) throws AthenaQueryException {
    return s3Client.listObjectsV2(new ListObjectsV2Request().withBucketName(bucket).withPrefix(prefix).withMaxKeys(1)).getKeyCount() > 0;
  }

  public String generateDownloadLink(String bucket, String key) throws AthenaQueryException {
    final Date expireAt = new Date(System.currentTimeMillis() + athenaConfig.getExpireS3LinksAfterMillis());
    GeneratePresignedUrlRequest getUrlRequest = new GeneratePresignedUrlRequest(bucket, key)
        .withMethod(HttpMethod.GET)
        .withExpiration(expireAt);
    return s3Client.generatePresignedUrl(getUrlRequest).toString();
  }
}
