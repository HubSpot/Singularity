package com.hubspot.singularity.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Describes the new scale for a request")
public class SingularityScaleRequest extends SingularityExpiringRequestParent {

  private final Optional<Integer> instances;
  private final Optional<Boolean> skipHealthchecks;
  private final Optional<Boolean> bounce;
  private final Optional<Boolean> incremental;
  private final Optional<Boolean> skipEmailNotification;

  @Deprecated
  public SingularityScaleRequest(Optional<Integer> instances,
                                 Optional<Long> durationMillis,
                                 Optional<Boolean> skipHealthchecks,
                                 Optional<String> actionId,
                                 Optional<String> message,
                                 Optional<Boolean> bounce,
                                 Optional<Boolean> incremental) {
    this(instances, durationMillis, skipHealthchecks, actionId, message, bounce, incremental, Optional.absent());
  }

  @JsonCreator
  public SingularityScaleRequest(@JsonProperty("instances") Optional<Integer> instances,
                                 @JsonProperty("durationMillis") Optional<Long> durationMillis,
                                 @JsonProperty("skipHealthchecks") Optional<Boolean> skipHealthchecks,
                                 @JsonProperty("actionId") Optional<String> actionId,
                                 @JsonProperty("message") Optional<String> message,
                                 @JsonProperty("bounce") Optional<Boolean> bounce,
                                 @JsonProperty("incremental") Optional<Boolean> incremental,
                                 @JsonProperty("skipEmailNotification") Optional<Boolean> skipEmailNotification) {
    super(durationMillis, actionId, message);
    this.instances = instances;
    this.skipHealthchecks = skipHealthchecks;
    this.bounce = bounce;
    this.incremental = incremental;
    this.skipEmailNotification = skipEmailNotification;
  }

  @Schema(description = "If set to true, healthchecks will be skipped while scaling this request (only)", nullable = true)
  public Optional<Boolean> getSkipHealthchecks() {
    return skipHealthchecks;
  }

  @Schema(description = "The number of instances to scale to", nullable = true)
  public Optional<Integer> getInstances() {
    return instances;
  }

  @Schema(description = "Bounce the request to get to the new scale", nullable = true)
  public Optional<Boolean> getBounce() {
    return bounce;
  }

  @Schema(description = "If present and set to true, old tasks will be killed as soon as replacement tasks are available, instead of waiting for all replacement tasks to be healthy", nullable = true)
  public Optional<Boolean> getIncremental() {
    return incremental;
  }

  @Schema(description = "If set to true, no email notification will be sent out for this specific scale event", nullable = true)
  public Optional<Boolean> getSkipEmailNotification() {
    return skipEmailNotification;
  }

  @Override
  public String toString() {
    return "SingularityScaleRequest{" +
        "instances=" + instances +
        ", skipHealthchecks=" + skipHealthchecks +
        ", bounce=" + bounce +
        ", incremental=" + incremental +
        ", skipEmailNotification=" + skipEmailNotification +
        "} " + super.toString();
  }
}
