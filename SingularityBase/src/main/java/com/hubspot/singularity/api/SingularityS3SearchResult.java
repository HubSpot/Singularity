package com.hubspot.singularity.api;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.singularity.SingularityS3LogMetadata;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class SingularityS3SearchResult {
  private final Map<String, ContinuationToken> continuationTokens;
  private final boolean lastPage;
  private final List<SingularityS3LogMetadata> results;

  public static SingularityS3SearchResult empty() {
    return new SingularityS3SearchResult(Collections.<String, ContinuationToken>emptyMap(), false, Collections.<SingularityS3LogMetadata>emptyList());
  }

  @JsonCreator
  public SingularityS3SearchResult(@JsonProperty("continuationTokens") Map<String, ContinuationToken> continuationTokens,
                                   @JsonProperty("lastPage") boolean lastPage,
                                   @JsonProperty("results") List<SingularityS3LogMetadata> results) {
    this.continuationTokens = continuationTokens;
    this.lastPage = lastPage;
    this.results = results;
  }

  @ApiModelProperty(required=false, value="S3 continuation tokens, return these to Singularity to continue searching subsequent pages of results")
  public Map<String, ContinuationToken> getContinuationTokens() {
    return continuationTokens;
  }

  @ApiModelProperty(required=true, value="If true, there are no further results for any bucket + prefix being searched")
  public boolean isLastPage() {
    return lastPage;
  }

  @ApiModelProperty("List of S3 log metadata")
  public List<SingularityS3LogMetadata> getResults() {
    return results;
  }

  @Override
  public String toString() {
    return "SingularityS3SearchResult{" +
        "continuationTokens=" + continuationTokens +
        ", lastPage=" + lastPage +
        ", results=" + results +
        '}';
  }
}
