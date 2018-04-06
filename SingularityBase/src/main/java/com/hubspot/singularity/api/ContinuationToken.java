package com.hubspot.singularity.api;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Describes parameters to fetch the next page of results from s3")
public class ContinuationToken {
  private final String value;
  private final boolean lastPage;

  @JsonCreator
  public ContinuationToken(@JsonProperty("value") String value, @JsonProperty("lastPage") boolean lastPage) {
    this.value = value;
    this.lastPage = lastPage;
  }

  @Schema(required = true, description = "S3 continuation token specific to a bucket + prefix being searched")
  public String getValue() {
    return value;
  }

  @Schema(required = true, description = "If true, there are no further results for this bucket + prefix")
  public boolean isLastPage() {
    return lastPage;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ContinuationToken that = (ContinuationToken) o;
    return lastPage == that.lastPage &&
        Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, lastPage);
  }

  @Override
  public String toString() {
    return "ContinuationToken{" +
        "value='" + value + '\'' +
        ", lastPage=" + lastPage +
        '}';
  }
}
