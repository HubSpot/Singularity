package com.hubspot.singularity.api.deploy;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Describes health check behavior for tasks associated with a deploy")
public class HealthcheckOptions {
  @NotNull
  private final String uri;
  private final Optional<Integer> portIndex;
  private final Optional<Long> portNumber;
  private final Optional<HealthcheckProtocol> protocol;
  private final Optional<Integer> startupTimeoutSeconds;
  private final Optional<Integer> startupDelaySeconds;
  private final Optional<Integer> startupIntervalSeconds;
  private final Optional<Integer> intervalSeconds;
  private final Optional<Integer> responseTimeoutSeconds;
  private final Optional<Integer> maxRetries;
  private final Optional<List<Integer>> failureStatusCodes;

  @JsonCreator
  public HealthcheckOptions(@JsonProperty("uri") String uri,
                            @JsonProperty("portIndex") Optional<Integer> portIndex,
                            @JsonProperty("portNumber") Optional<Long> portNumber,
                            @JsonProperty("protocol") Optional<HealthcheckProtocol> protocol,
                            @JsonProperty("startupTimeoutSeconds") Optional<Integer> startupTimeoutSeconds,
                            @JsonProperty("startupDelaySeconds") Optional<Integer> startupDelaySeconds,
                            @JsonProperty("startupIntervalSeconds") Optional<Integer> startupIntervalSeconds,
                            @JsonProperty("intervalSeconds") Optional<Integer> intervalSeconds,
                            @JsonProperty("responseTimeoutSeconds") Optional<Integer> responseTimeoutSeconds,
                            @JsonProperty("maxRetries") Optional<Integer> maxRetries,
                            @JsonProperty("failureStatusCodes") Optional<List<Integer>> failureStatusCodes) {
    this.uri = uri;
    this.portIndex = portIndex;
    this.portNumber = portNumber;
    this.protocol = protocol;
    this.startupTimeoutSeconds = startupTimeoutSeconds;
    this.startupDelaySeconds = startupDelaySeconds;
    this.startupIntervalSeconds = startupIntervalSeconds;
    this.intervalSeconds = intervalSeconds;
    this.responseTimeoutSeconds = responseTimeoutSeconds;
    this.maxRetries = maxRetries;
    this.failureStatusCodes = failureStatusCodes;
  }

  @JsonIgnore
  public HealthcheckOptionsBuilder toBuilder() {
    return new HealthcheckOptionsBuilder(uri)
      .setPortIndex(portIndex)
      .setPortNumber(portNumber)
      .setProtocol(protocol)
      .setStartupTimeoutSeconds(startupTimeoutSeconds)
      .setStartupDelaySeconds(startupDelaySeconds)
      .setStartupIntervalSeconds(startupIntervalSeconds)
      .setIntervalSeconds(intervalSeconds)
      .setResponseTimeoutSeconds(responseTimeoutSeconds)
      .setMaxRetries(maxRetries)
      .setFailureStatusCodes(failureStatusCodes);
  }

  @Schema(required = true, description = "Healthcheck uri to hit")
  public String getUri() {
    return uri;
  }

  @Schema(description = "Perform healthcheck on this dynamically allocated port (e.g. 0 for first port), defaults to first port")
  public Optional<Integer> getPortIndex() {
    return portIndex;
  }

  @Schema(description = "Perform healthcheck on this port (portIndex cannot also be used when using this setting)")
  public Optional<Long> getPortNumber() {
    return portNumber;
  }

  @Schema(description = "Healthcheck protocol - HTTP or HTTPS")
  public Optional<HealthcheckProtocol> getProtocol() {
    return protocol;
  }

  @Schema(description = "Consider the task unhealthy/failed if the app has not started responding to healthchecks in this amount of time")
  public Optional<Integer> getStartupTimeoutSeconds() {
    return startupTimeoutSeconds;
  }

  @Schema(description = "Wait this long before issuing the first healthcheck")
  public Optional<Integer> getStartupDelaySeconds() {
    return startupDelaySeconds;
  }

  @Schema(description = "Time to wait after a failed healthcheck to try again in seconds.")
  public Optional<Integer> getStartupIntervalSeconds() {
    return startupIntervalSeconds;
  }

  @Schema(description = "Time to wait after a valid but failed healthcheck response to try again in seconds.")
  public Optional<Integer> getIntervalSeconds() {
    return intervalSeconds;
  }

  @Schema(description = "Single healthcheck HTTP timeout in seconds.")
  public Optional<Integer> getResponseTimeoutSeconds() {
    return responseTimeoutSeconds;
  }

  @Schema(description = "Maximum number of times to retry an individual healthcheck before failing the deploy.")
  public Optional<Integer> getMaxRetries() {
    return maxRetries;
  }

  @Schema(description = "Fail the healthcheck with no further retries if one of these status codes is returned")
  public Optional<List<Integer>> getFailureStatusCodes() {
    return failureStatusCodes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HealthcheckOptions that = (HealthcheckOptions) o;
    return Objects.equals(uri, that.uri) &&
        Objects.equals(portIndex, that.portIndex) &&
        Objects.equals(portNumber, that.portNumber) &&
        Objects.equals(protocol, that.protocol) &&
        Objects.equals(startupTimeoutSeconds, that.startupTimeoutSeconds) &&
        Objects.equals(startupDelaySeconds, that.startupDelaySeconds) &&
        Objects.equals(startupIntervalSeconds, that.startupIntervalSeconds) &&
        Objects.equals(intervalSeconds, that.intervalSeconds) &&
        Objects.equals(responseTimeoutSeconds, that.responseTimeoutSeconds) &&
        Objects.equals(maxRetries, that.maxRetries) &&
        Objects.equals(failureStatusCodes, that.failureStatusCodes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uri, portIndex, portNumber, protocol, startupTimeoutSeconds, startupDelaySeconds, startupIntervalSeconds, intervalSeconds, responseTimeoutSeconds, maxRetries, failureStatusCodes);
  }

  @Override
  public String toString() {
    return "HealthcheckOptions{" +
        "uri='" + uri + '\'' +
        ", portIndex=" + portIndex +
        ", portNumber=" + portNumber +
        ", protocol=" + protocol +
        ", startupTimeoutSeconds=" + startupTimeoutSeconds +
        ", startupDelaySeconds=" + startupDelaySeconds +
        ", startupIntervalSeconds=" + startupIntervalSeconds +
        ", intervalSeconds=" + intervalSeconds +
        ", responseTimeoutSeconds=" + responseTimeoutSeconds +
        ", maxRetries=" + maxRetries +
        ", failureStatusCodes=" + failureStatusCodes +
        '}';
  }
}
