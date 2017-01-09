package com.hubspot.singularity.api;

import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class SingularityS3SearchRequest {
  private final Optional<Long> start;
  private final Optional<Long> end;
  private final boolean excludeMetadata;
  private final boolean listOnly;
  private final Optional<Integer> maxPerPage;
  private final Set<ContinuationToken> continuationTokens;

  @JsonCreator

  public SingularityS3SearchRequest(@JsonProperty("start") Optional<Long> start,
                                    @JsonProperty("end") Optional<Long> end,
                                    @JsonProperty("excludeMetadata") Optional<Boolean> excludeMetadata,
                                    @JsonProperty("listOnly") Optional<Boolean> listOnly,
                                    @JsonProperty("maxPerPage") Optional<Integer> maxPerPage,
                                    @JsonProperty("continuationTokens") Set<ContinuationToken> continuationTokens) {
    this.start = start;
    this.end = end;
    this.excludeMetadata = excludeMetadata.or(false);
    this.listOnly = listOnly.or(false);
    this.maxPerPage = maxPerPage;
    this.continuationTokens = Objects.firstNonNull(continuationTokens, Collections.<ContinuationToken>emptySet());
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

  @ApiModelProperty(required=false, value="Maximum number of results to return per prefix + bucket searched")
  public Optional<Integer> getMaxPerPage() {
    return maxPerPage;
  }

  @ApiModelProperty(required=false, value="S3 continuation tokens, return these to Singularity to continue searching subsequent pages of results")
  public Set<ContinuationToken> getContinuationTokens() {
    return continuationTokens;
  }

  @Override
  public String toString() {
    return "SingularityS3SearchRequest{" +
        "start=" + start +
        ", end=" + end +
        ", excludeMetadata=" + excludeMetadata +
        ", listOnly=" + listOnly +
        ", maxPerPage=" + maxPerPage +
        ", continuationTokens=" + continuationTokens +
        '}';
  }
}
