package com.hubspot.singularity.api.logs;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Describes a request to search for task logs in s3")
public class SingularityS3SearchRequest {
  private final Map<String, List<String>> requestsAndDeploys;
  private final List<String> taskIds;
  private final Optional<Long> start;
  private final Optional<Long> end;
  private final boolean excludeMetadata;
  private final boolean listOnly;
  private final Optional<Integer> maxPerPage;
  private final Map<String, ContinuationToken> continuationTokens;

  @JsonCreator

  public SingularityS3SearchRequest(@JsonProperty("requestsAndDeploys") Map<String, List<String>> requestsAndDeploys,
                                    @JsonProperty("taskIds") List<String> taskIds,
                                    @JsonProperty("start") Optional<Long> start,
                                    @JsonProperty("end") Optional<Long> end,
                                    @JsonProperty("excludeMetadata") boolean excludeMetadata,
                                    @JsonProperty("listOnly") boolean listOnly,
                                    @JsonProperty("maxPerPage") Optional<Integer> maxPerPage,
                                    @JsonProperty("continuationTokens") Map<String, ContinuationToken> continuationTokens) {
    this.requestsAndDeploys = requestsAndDeploys != null ? requestsAndDeploys : Collections.<String, List<String>>emptyMap();
    this.taskIds = taskIds != null ? taskIds : Collections.<String>emptyList();
    this.start = start;
    this.end = end;
    this.excludeMetadata = excludeMetadata;
    this.listOnly = listOnly;
    this.maxPerPage = maxPerPage;
    this.continuationTokens = continuationTokens != null ? continuationTokens : Collections.<String, ContinuationToken>emptyMap();
  }

  @Schema(description = "A map of request IDs to a list of deploy ids to search")
  public Map<String, List<String>> getRequestsAndDeploys() {
    return requestsAndDeploys;
  }

  @Schema(description = "A list of task IDs to search for")
  public List<String> getTaskIds() {
    return taskIds;
  }

  @Schema(description = "Start timestamp (millis, 13 digit)", nullable = true)
  public Optional<Long> getStart() {
    return start;
  }

  @Schema(description = "End timestamp (millis, 13 digit)", nullable = true)
  public Optional<Long> getEnd() {
    return end;
  }

  @Schema(description = "if true, do not query for custom start/end time metadata", defaultValue = "false")
  public boolean isExcludeMetadata() {
    return excludeMetadata;
  }

  @Schema(description = "If true, do not generate download/get urls, only list objects", defaultValue = "false")
  public boolean isListOnly() {
    return listOnly;
  }

  /**
   * NOTE: maxPerPage is not a guaranteed value. It is possible to get as many as (maxPerPage * 2 - 1) results
   * when using the paginated search endpoint
   */
  @Schema(description = "Target number of results to return", nullable = true)
  public Optional<Integer> getMaxPerPage() {
    return maxPerPage;
  }

  @Schema(description = "S3 continuation tokens, return these to Singularity to continue searching subsequent pages of results")
  public Map<String, ContinuationToken> getContinuationTokens() {
    return continuationTokens;
  }

  @Override
  public String toString() {
    return "SingularityS3SearchRequest{" +
        "requestsAndDeploys=" + requestsAndDeploys +
        ", taskIds=" + taskIds +
        ", start=" + start +
        ", end=" + end +
        ", excludeMetadata=" + excludeMetadata +
        ", listOnly=" + listOnly +
        ", maxPerPage=" + maxPerPage +
        ", continuationTokens=" + continuationTokens +
        '}';
  }
}
