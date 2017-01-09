package com.hubspot.singularity.api;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class SingularityS3SearchRequest {
  private final Optional<String> requestId;
  private final Optional<String> deployId;
  private final Optional<String> taskId;
  private final Optional<Long> start;
  private final Optional<Long> end;
  private final boolean excludeMetadata;
  private final boolean listOnly;
  private final Optional<Integer> maxPerPage;
  private final Map<String, ContinuationToken> continuationTokens;

  @JsonCreator

  public SingularityS3SearchRequest(@JsonProperty("requestId") Optional<String> requestId,
                                    @JsonProperty("deployId") Optional<String> deployId,
                                    @JsonProperty("taskId") Optional<String> taskId,
                                    @JsonProperty("start") Optional<Long> start,
                                    @JsonProperty("end") Optional<Long> end,
                                    @JsonProperty("excludeMetadata") boolean excludeMetadata,
                                    @JsonProperty("listOnly") boolean listOnly,
                                    @JsonProperty("maxPerPage") Optional<Integer> maxPerPage,
                                    @JsonProperty("continuationTokens") Map<String, ContinuationToken> continuationTokens) {
    this.requestId = requestId;
    this.deployId = deployId;
    this.taskId = taskId;
    this.start = start;
    this.end = end;
    this.excludeMetadata = excludeMetadata;
    this.listOnly = listOnly;
    this.maxPerPage = maxPerPage;
    this.continuationTokens = Objects.firstNonNull(continuationTokens, Collections.<String, ContinuationToken>emptyMap());
  }

  @ApiModelProperty(required=false, value="The request ID to search for")
  public Optional<String> getRequestId() {
    return requestId;
  }

  @ApiModelProperty(required=false, value="The deploy ID to search for")
  public Optional<String> getDeployId() {
    return deployId;
  }

  @ApiModelProperty(required=false, value="The task ID to search for")
  public Optional<String> getTaskId() {
    return taskId;
  }

  @ApiModelProperty(required=false, value="Start timestamp (millis, 13 digit)")
  public Optional<Long> getStart() {
    return start;
  }

  @ApiModelProperty(required=false, value="End timestamp (millis, 13 digit)")
  public Optional<Long> getEnd() {
    return end;
  }

  @ApiModelProperty(required=false, value="if true, do not query for custom start/end time metadata")
  public boolean isExcludeMetadata() {
    return excludeMetadata;
  }

  @ApiModelProperty(required=false, value="If true, do not generate download/get urls, only list objects")
  public boolean isListOnly() {
    return listOnly;
  }

  /**
   * NOTE: maxPerPage is not a guaranteed value. It is possible to get as many as (maxPerPage * 2 - 1) results
   * when using the paginated search endpoint
   */
  @ApiModelProperty(required=false, value="Target number of results to return")
  public Optional<Integer> getMaxPerPage() {
    return maxPerPage;
  }

  @ApiModelProperty(required=false, value="S3 continuation tokens, return these to Singularity to continue searching subsequent pages of results")
  public Map<String, ContinuationToken> getContinuationTokens() {
    return continuationTokens;
  }

  @Override
  public String toString() {
    return "SingularityS3SearchRequest{" +
        "requestId=" + requestId +
        ", deployId=" + deployId +
        ", taskId=" + taskId +
        ", start=" + start +
        ", end=" + end +
        ", excludeMetadata=" + excludeMetadata +
        ", listOnly=" + listOnly +
        ", maxPerPage=" + maxPerPage +
        ", continuationTokens=" + continuationTokens +
        '}';
  }
}
