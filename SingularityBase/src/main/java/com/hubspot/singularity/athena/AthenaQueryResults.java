package com.hubspot.singularity.athena;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AthenaQueryResults {
  private final AthenaQueryInfo queryInfo;
  private final List<Map<String, String>> results;
  private final int requestedPageSize;
  private final String requestToken;
  private final String nextToken;

  @JsonCreator
  public AthenaQueryResults(@JsonProperty("queryInfo") AthenaQueryInfo queryInfo,
                            @JsonProperty("results") List<Map<String, String>> results,
                            @JsonProperty("requestedPageSize") int requestedPageSize,
                            @JsonProperty("requestToken") String requestToken,
                            @JsonProperty("nextToken") String nextToken) {
    this.queryInfo = queryInfo;
    this.results = results;
    this.requestedPageSize = requestedPageSize;
    this.requestToken = requestToken;
    this.nextToken = nextToken;
  }

  public AthenaQueryInfo getQueryInfo() {
    return queryInfo;
  }

  public List<Map<String, String>> getResults() {
    return results;
  }

  public int getRequestedPageSize() {
    return requestedPageSize;
  }

  public String getRequestToken() {
    return requestToken;
  }

  public String getNextToken() {
    return nextToken;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    AthenaQueryResults that = (AthenaQueryResults) o;

    if (requestedPageSize != that.requestedPageSize) {
      return false;
    }
    if (queryInfo != null ? !queryInfo.equals(that.queryInfo) : that.queryInfo != null) {
      return false;
    }
    if (results != null ? !results.equals(that.results) : that.results != null) {
      return false;
    }
    if (requestToken != null ? !requestToken.equals(that.requestToken) : that.requestToken != null) {
      return false;
    }
    return nextToken != null ? nextToken.equals(that.nextToken) : that.nextToken == null;
  }

  @Override
  public int hashCode() {
    int result = queryInfo != null ? queryInfo.hashCode() : 0;
    result = 31 * result + (results != null ? results.hashCode() : 0);
    result = 31 * result + requestedPageSize;
    result = 31 * result + (requestToken != null ? requestToken.hashCode() : 0);
    result = 31 * result + (nextToken != null ? nextToken.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "AthenaQueryResults{" +
        "queryInfo=" + queryInfo +
        ", results=" + results +
        ", requestedPageSize=" + requestedPageSize +
        ", requestToken='" + requestToken + '\'' +
        ", nextToken='" + nextToken + '\'' +
        '}';
  }
}
