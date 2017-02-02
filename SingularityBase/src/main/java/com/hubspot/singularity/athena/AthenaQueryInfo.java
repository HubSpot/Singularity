package com.hubspot.singularity.athena;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

public class AthenaQueryInfo {
  private final String id;
  private final String queryExecutionId;
  private final String sql;
  private final AthenaQueryStatus status;
  private final List<AthenaField> fields;
  private final Optional<String> exceptionMessage;

  @JsonCreator
  public AthenaQueryInfo(@JsonProperty("id") String id,
                         @JsonProperty("queryExecutionId") String queryExecutionId,
                         @JsonProperty("sql") String sql,
                         @JsonProperty("status") AthenaQueryStatus status,
                         @JsonProperty("fields") List<AthenaField> fields,
                         @JsonProperty("exceptionMessage") Optional<String> exceptionMessage) {
    this.id = id;
    this.queryExecutionId = queryExecutionId;
    this.sql = sql;
    this.status = status;
    this.fields = Objects.firstNonNull(fields, Collections.<AthenaField>emptyList());
    this.exceptionMessage = exceptionMessage;
  }

  public String getId() {
    return id;
  }

  public String getQueryExecutionId() {
    return queryExecutionId;
  }

  public String getSql() {
    return sql;
  }

  public AthenaQueryStatus getStatus() {
    return status;
  }

  public List<AthenaField> getFields() {
    return fields;
  }

  public Optional<String> getExceptionMessage() {
    return exceptionMessage;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    AthenaQueryInfo queryInfo = (AthenaQueryInfo) o;

    if (id != null ? !id.equals(queryInfo.id) : queryInfo.id != null) {
      return false;
    }
    if (queryExecutionId != null ? !queryExecutionId.equals(queryInfo.queryExecutionId) : queryInfo.queryExecutionId != null) {
      return false;
    }
    if (sql != null ? !sql.equals(queryInfo.sql) : queryInfo.sql != null) {
      return false;
    }
    if (status != queryInfo.status) {
      return false;
    }
    if (fields != null ? !fields.equals(queryInfo.fields) : queryInfo.fields != null) {
      return false;
    }
    return exceptionMessage != null ? exceptionMessage.equals(queryInfo.exceptionMessage) : queryInfo.exceptionMessage == null;
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (queryExecutionId != null ? queryExecutionId.hashCode() : 0);
    result = 31 * result + (sql != null ? sql.hashCode() : 0);
    result = 31 * result + (status != null ? status.hashCode() : 0);
    result = 31 * result + (fields != null ? fields.hashCode() : 0);
    result = 31 * result + (exceptionMessage != null ? exceptionMessage.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "AthenaQueryInfo{" +
        "id='" + id + '\'' +
        ", queryExecutionId='" + queryExecutionId + '\'' +
        ", sql='" + sql + '\'' +
        ", status=" + status +
        ", fields=" + fields +
        ", exceptionMessage=" + exceptionMessage +
        '}';
  }
}
