package com.hubspot.singularity.api;

import java.util.List;
import java.util.Map;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Immutable
@SingularityStyle
@JsonDeserialize(as = SingularityS3SearchRequest.class)
public interface SingularityS3SearchRequestIF {

  @ApiModelProperty(required=false, value="A map of request IDs to a list of deploy ids to search")
  Map<String, List<String>> getRequestsAndDeploys();

  @ApiModelProperty(required=false, value="A list of task IDs to search for")
  List<String> getTaskIds();

  @ApiModelProperty(required=false, value="Start timestamp (millis, 13 digit)")
  Optional<Long> getStart();

  @ApiModelProperty(required=false, value="End timestamp (millis, 13 digit)")
  Optional<Long> getEnd();

  @ApiModelProperty(required=false, value="if true, do not query for custom start/end time metadata")
  boolean isExcludeMetadata();

  @ApiModelProperty(required=false, value="If true, do not generate download/get urls, only list objects")
  boolean isListOnly();

  /**
   * NOTE: maxPerPage is not a guaranteed value. It is possible to get as many as (maxPerPage * 2 - 1) results
   * when using the paginated search endpoint
   */
  @ApiModelProperty(required=false, value="Target number of results to return")
  Optional<Integer> getMaxPerPage();

  @ApiModelProperty(required=false, value="S3 continuation tokens, return these to Singularity to continue searching subsequent pages of results")
  Map<String, ContinuationToken> getContinuationTokens();
}
