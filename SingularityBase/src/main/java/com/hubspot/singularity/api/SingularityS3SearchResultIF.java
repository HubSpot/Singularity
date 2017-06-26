package com.hubspot.singularity.api;

import java.util.List;
import java.util.Map;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import com.hubspot.immutables.style.SingularityStyle;
import com.hubspot.singularity.SingularityS3LogMetadata;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Immutable
@SingularityStyle
public interface SingularityS3SearchResultIF {

  @ApiModelProperty(required=false, value="S3 continuation tokens, return these to Singularity to continue searching subsequent pages of results")
  Map<String, ContinuationToken> getContinuationTokens();

  @Default
  @ApiModelProperty(required=true, value="If true, there are no further results for any bucket + prefix being searched")
  default boolean isLastPage() {
    return false;
  }

  @ApiModelProperty("List of S3 log metadata")
  List<SingularityS3LogMetadata> getResults();
}
