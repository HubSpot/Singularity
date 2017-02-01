package com.hubspot.singularity.athena;

import com.amazonaws.athena.jdbc.shaded.com.amazonaws.services.athena.model.GetQueryResultsResult;
import com.amazonaws.athena.jdbc.shaded.com.amazonaws.services.athena.model.QueryExecutionStatus;
import com.google.inject.Inject;

public class NoopAthenaQueryRunner implements AthenaQueryRunner {

  @Inject
  public NoopAthenaQueryRunner() {}

  public GetQueryResultsResult getQueryResults(String queryExecutionId, String token, int pageSize) throws AthenaQueryException {
    throw new AthenaQueryException("No Athena Credentials Provided");
  }

  public String runQuery(String sql) throws AthenaQueryException {
    throw new AthenaQueryException("No Athena Credentials Provided");
  }

  public QueryExecutionStatus getQueryExecutionStatus(String queryExecutionId) throws AthenaQueryException {
    throw new AthenaQueryException("No Athena Credentials Provided");
  }

  public void cancelQuery(String queryExecutionId) throws AthenaQueryException {
    throw new AthenaQueryException("No Athena Credentials Provided");
  }
}
