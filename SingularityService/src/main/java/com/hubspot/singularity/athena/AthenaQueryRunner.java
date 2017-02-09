package com.hubspot.singularity.athena;

import com.amazonaws.athena.jdbc.shaded.com.amazonaws.services.athena.model.GetQueryResultsResult;
import com.amazonaws.athena.jdbc.shaded.com.amazonaws.services.athena.model.QueryExecutionStatus;

public interface AthenaQueryRunner {
  GetQueryResultsResult getQueryResults(String queryExecutionId, String token, int pageSize) throws AthenaQueryException;
  String runQuery(String sql) throws AthenaQueryException;
  QueryExecutionStatus getQueryExecutionStatus(String queryExecutionId) throws AthenaQueryException;
  void cancelQuery(String queryExecutionId) throws AthenaQueryException;
  boolean isPartitionPathValid(String bucket, String prefix) throws AthenaQueryException;
  String generateDownloadLink(String bucket, String key) throws AthenaQueryException;
}
