package com.hubspot.singularity.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class ContinuationToken {
  private final String value;
  private final boolean lastPage;

  @JsonCreator
  public ContinuationToken(@JsonProperty("value") String value, @JsonProperty("lastPage") boolean lastPage) {
    this.value = value;
    this.lastPage = lastPage;
  }

  @ApiModelProperty(required=true, value="S3 continuation token specific to a bucket + prefix being searched")
  public String getValue() {
    return value;
  }

  @ApiModelProperty(required=true, value="If true, there are no further results for this bucket + prefix")
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

    ContinuationToken token = (ContinuationToken) o;

    if (lastPage != token.lastPage) {
      return false;
    }
    return value != null ? value.equals(token.value) : token.value == null;
  }

  @Override
  public int hashCode() {
    int result = value != null ? value.hashCode() : 0;
    result = 31 * result + (lastPage ? 1 : 0);
    return result;
  }

  @Override
  public String toString() {
    return "ContinuationToken{" +
        "value='" + value + '\'' +
        ", lastPage=" + lastPage +
        '}';
  }
}
