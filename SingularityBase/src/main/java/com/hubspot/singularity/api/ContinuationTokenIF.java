package com.hubspot.singularity.api;

import org.immutables.value.Value.Immutable;

import com.hubspot.immutables.style.SingularityStyle;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Immutable
@SingularityStyle
public interface ContinuationTokenIF {
  @ApiModelProperty(required=true, value="S3 continuation token specific to a bucket + prefix being searched")
  String getValue();

  @ApiModelProperty(required=true, value="If true, there are no further results for this bucket + prefix")
  boolean isLastPage();
}
