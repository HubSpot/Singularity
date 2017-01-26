package com.hubspot.singularity.athena;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class AthenaQueryResult {
  private final String id;
  private final Optional<String> athenaQueryId;
  private final String sql;
  private final Optional<String> s3ResultBucket;
  private final Optional<String> s3ResultPrefix;
  private final AthenaQueryStatus status;
  private final Optional<String> exceptionMessage;

  @JsonCreator
  public AthenaQueryResult(@JsonProperty("id") String id,
                           @JsonProperty("athenaQueryId") Optional<String> athenaQueryId,
                           @JsonProperty("sql") String sql,
                           @JsonProperty("s3ResultBucket") Optional<String> s3ResultBucket,
                           @JsonProperty("s3ResultPrefix") Optional<String> s3ResultPrefix,
                           @JsonProperty("status") AthenaQueryStatus status,
                           @JsonProperty("exceptionMessage") Optional<String> exceptionMessage) {
    this.id = id;
    this.athenaQueryId = athenaQueryId;
    this.sql = sql;
    this.s3ResultBucket = s3ResultBucket;
    this.s3ResultPrefix = s3ResultPrefix;
    this.status = status;
    this.exceptionMessage = exceptionMessage;
  }

  public String getId() {
    return id;
  }

  public Optional<String> getAthenaQueryId() {
    return athenaQueryId;
  }


  public String getSql() {
    return sql;
  }

  public Optional<String> getS3ResultBucket() {
    return s3ResultBucket;
  }

  public Optional<String> getS3ResultPrefix() {
    return s3ResultPrefix;
  }

  public AthenaQueryStatus getStatus() {
    return status;
  }

  public Optional<String> getExceptionMessage() {
    return exceptionMessage;
  }
}
