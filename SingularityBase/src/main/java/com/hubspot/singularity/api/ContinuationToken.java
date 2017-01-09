package com.hubspot.singularity.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class ContinuationToken {
  String bucket;
  String prefix;
  String value;
  boolean lastPage;

  @JsonCreator
  public ContinuationToken(@JsonProperty("bucket") String bucket,
                           @JsonProperty("prefix") String prefix,
                           @JsonProperty("value") String value,
                           @JsonProperty("lastPage") boolean lastPage) {
    this.bucket = bucket;
    this.prefix = prefix;
    this.value = value;
    this.lastPage = lastPage;
  }

  @ApiModelProperty(required=true, value="prefix associated with this continuation token")
  public String getBucket() {
    return bucket;
  }

  @ApiModelProperty(required=true, value="bucket associated with this continuation token")
  public String getPrefix() {
    return prefix;
  }

  @ApiModelProperty(required=true, value="S3 continuation token specific to a bucket + prefix being searched")
  public String getValue() {
    return value;
  }

  @ApiModelProperty(required=true, value="If true, there are no further results for this bucket + prefix")
  public boolean isLastPage() {
    return lastPage;
  }


}
