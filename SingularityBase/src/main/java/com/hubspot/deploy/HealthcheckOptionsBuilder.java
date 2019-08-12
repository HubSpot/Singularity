package com.hubspot.deploy;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.hubspot.singularity.HealthcheckMethod;
import com.hubspot.singularity.HealthcheckProtocol;

public class HealthcheckOptionsBuilder {
  private Optional<String> uri;
  private Optional<Integer> portIndex;
  private Optional<Long> portNumber;
  private Optional<HealthcheckProtocol> protocol;
  private Optional<HealthcheckMethod> method;
  private Optional<Integer> startupTimeoutSeconds;
  private Optional<Integer> startupDelaySeconds;
  private Optional<Integer> startupIntervalSeconds;
  private Optional<Integer> intervalSeconds;
  private Optional<Integer> responseTimeoutSeconds;
  private Optional<Integer> maxRetries;
  private Optional<List<Integer>> failureStatusCodes;
  private Optional<String> healthcheckResultFilePath;

  public HealthcheckOptionsBuilder(String uri) {
    this(Optional.of(uri));
  }

  public HealthcheckOptionsBuilder(Optional<String> uri) {
    this.uri = uri;
    this.portIndex = Optional.empty();
    this.portNumber = Optional.empty();
    this.protocol = Optional.empty();
    this.method = Optional.empty();
    this.startupTimeoutSeconds = Optional.empty();
    this.startupDelaySeconds = Optional.empty();
    this.startupIntervalSeconds = Optional.empty();
    this.intervalSeconds = Optional.empty();
    this.responseTimeoutSeconds = Optional.empty();
    this.maxRetries = Optional.empty();
    this.failureStatusCodes = Optional.empty();
    this.healthcheckResultFilePath = Optional.empty();
  }

  public Optional<String> getUri() {
    return uri;
  }

  public HealthcheckOptionsBuilder setUri(String uri) {
    this.uri = Optional.of(uri);
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

  public Optional<HealthcheckMethod> getMethod() {
    return method;
  }

  public HealthcheckOptionsBuilder setMethod(Optional<HealthcheckMethod> method) {
    this.method = method;
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

  public Optional<String> getHealthcheckResultFilePath() {
    return healthcheckResultFilePath;
  }

  public HealthcheckOptionsBuilder setHealthcheckResultFilePath(Optional<String> healthcheckResultFilePath) {
    this.healthcheckResultFilePath= healthcheckResultFilePath;
    return this;
  }

  public HealthcheckOptions build() {
    return new HealthcheckOptions(uri, portIndex, portNumber, protocol, method, startupTimeoutSeconds, startupDelaySeconds, startupIntervalSeconds, intervalSeconds, responseTimeoutSeconds, maxRetries, failureStatusCodes, healthcheckResultFilePath);
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
        Objects.equals(method, that.method) &&
        Objects.equals(startupTimeoutSeconds, that.startupTimeoutSeconds) &&
        Objects.equals(startupDelaySeconds, that.startupDelaySeconds) &&
        Objects.equals(startupIntervalSeconds, that.startupIntervalSeconds) &&
        Objects.equals(intervalSeconds, that.intervalSeconds) &&
        Objects.equals(responseTimeoutSeconds, that.responseTimeoutSeconds) &&
        Objects.equals(maxRetries, that.maxRetries) &&
        Objects.equals(failureStatusCodes, that.failureStatusCodes) &&
        Objects.equals(healthcheckResultFilePath, that.healthcheckResultFilePath);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uri, portIndex, portNumber, protocol, method, startupTimeoutSeconds, startupDelaySeconds, startupIntervalSeconds, intervalSeconds, responseTimeoutSeconds, maxRetries, failureStatusCodes, healthcheckResultFilePath);
  }

  @Override
  public String toString() {
    return "HealthcheckOptionsBuilder{" +
        "uri='" + uri + '\'' +
        ", portIndex=" + portIndex +
        ", portNumber=" + portNumber +
        ", protocol=" + protocol +
        ", method=" + method +
        ", startupTimeoutSeconds=" + startupTimeoutSeconds +
        ", startupDelaySeconds=" + startupDelaySeconds +
        ", startupIntervalSeconds=" + startupIntervalSeconds +
        ", intervalSeconds=" + intervalSeconds +
        ", responseTimeoutSeconds=" + responseTimeoutSeconds +
        ", maxRetries=" + maxRetries +
        ", failureStatusCodes=" + failureStatusCodes +
        ", healthcheckResultFilePath=" + healthcheckResultFilePath +
        '}';
  }
}
