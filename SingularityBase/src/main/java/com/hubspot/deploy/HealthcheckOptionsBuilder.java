package com.hubspot.deploy;

import java.util.List;
import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.google.common.base.Optional;
import com.hubspot.singularity.HealthcheckProtocol;

public class HealthcheckOptionsBuilder {
  @NotNull
  private String uri;
  private Optional<Integer> portIndex;
  private Optional<Long> portNumber;
  private Optional<HealthcheckProtocol> protocol;
  private Optional<Integer> startupTimeoutSeconds;
  private Optional<Integer> startupDelaySeconds;
  private Optional<Integer> startupIntervalSeconds;
  private Optional<Integer> intervalSeconds;
  private Optional<Integer> responseTimeoutSeconds;
  private Optional<Integer> maxRetries;
  private Optional<List<Integer>> failureStatusCodes;

  public HealthcheckOptionsBuilder(String uri) {
    this.uri = uri;
    this.portIndex = Optional.absent();
    this.portNumber = Optional.absent();
    this.protocol = Optional.absent();
    this.startupTimeoutSeconds = Optional.absent();
    this.startupDelaySeconds = Optional.absent();
    this.startupIntervalSeconds = Optional.absent();
    this.intervalSeconds = Optional.absent();
    this.responseTimeoutSeconds = Optional.absent();
    this.maxRetries = Optional.absent();
    this.failureStatusCodes = Optional.absent();
  }

  public String getUri() {
    return uri;
  }

  public HealthcheckOptionsBuilder setUri(String uri) {
    this.uri = uri;
    return this;
  }

  public Optional<Integer> getPortIndex() {
    return portIndex;
  }

  public HealthcheckOptionsBuilder setPortIndex(Optional<Integer> portIndex) {
    this.portIndex = portIndex;
    return this;
  }

  public Optional<Long> getPortNumber() {
    return portNumber;
  }

  public HealthcheckOptionsBuilder setPortNumber(Optional<Long> portNumber) {
    this.portNumber = portNumber;
    return this;
  }

  public Optional<HealthcheckProtocol> getProtocol() {
    return protocol;
  }

  public HealthcheckOptionsBuilder setProtocol(Optional<HealthcheckProtocol> protocol) {
    this.protocol = protocol;
    return this;
  }

  public Optional<Integer> getStartupTimeoutSeconds() {
    return startupTimeoutSeconds;
  }

  public HealthcheckOptionsBuilder setStartupTimeoutSeconds(Optional<Integer> startupTimeoutSeconds) {
    this.startupTimeoutSeconds = startupTimeoutSeconds;
    return this;
  }

  public Optional<Integer> getStartupDelaySeconds() {
    return startupDelaySeconds;
  }

  public HealthcheckOptionsBuilder setStartupDelaySeconds(Optional<Integer> startupDelaySeconds) {
    this.startupDelaySeconds = startupDelaySeconds;
    return this;
  }

  public Optional<Integer> getStartupIntervalSeconds() {
    return startupIntervalSeconds;
  }

  public HealthcheckOptionsBuilder setStartupIntervalSeconds(Optional<Integer> startupIntervalSeconds) {
    this.startupIntervalSeconds = startupIntervalSeconds;
    return this;
  }

  public Optional<Integer> getIntervalSeconds() {
    return intervalSeconds;
  }

  public HealthcheckOptionsBuilder setIntervalSeconds(Optional<Integer> intervalSeconds) {
    this.intervalSeconds = intervalSeconds;
    return this;
  }

  public Optional<Integer> getResponseTimeoutSeconds() {
    return responseTimeoutSeconds;
  }

  public HealthcheckOptionsBuilder setResponseTimeoutSeconds(Optional<Integer> responseTimeoutSeconds) {
    this.responseTimeoutSeconds = responseTimeoutSeconds;
    return this;
  }

  public Optional<Integer> getMaxRetries() {
    return maxRetries;
  }

  public HealthcheckOptionsBuilder setMaxRetries(Optional<Integer> maxRetries) {
    this.maxRetries = maxRetries;
    return this;
  }

  public Optional<List<Integer>> getFailureStatusCodes() {
    return failureStatusCodes;
  }

  public HealthcheckOptionsBuilder setFailureStatusCodes(Optional<List<Integer>> failureStatusCodes) {
    this.failureStatusCodes = failureStatusCodes;
    return this;
  }

  public HealthcheckOptions build() {
    return new HealthcheckOptions(uri, portIndex, portNumber, protocol, startupTimeoutSeconds, startupDelaySeconds, startupIntervalSeconds, intervalSeconds, responseTimeoutSeconds, maxRetries, failureStatusCodes);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HealthcheckOptionsBuilder that = (HealthcheckOptionsBuilder) o;
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
    return "HealthcheckOptionsBuilder{" +
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
