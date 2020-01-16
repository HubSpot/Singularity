package com.hubspot.singularity.api;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Settings related to shuffle configuration for a request.")
public class SingularityPerRequestShuffleConfiguration {
  public static final SingularityPerRequestShuffleConfiguration DEFAULT = new SingularityPerRequestShuffleConfiguration(
      Optional.of(false)
  );

  private final Optional<Boolean> avoidShuffle;

  @JsonCreator
  public SingularityPerRequestShuffleConfiguration(
      @JsonProperty("avoidShuffle") Optional<Boolean> avoidShuffle
  ) {
    this.avoidShuffle = avoidShuffle;
  }

  @Schema(description = "If true, Singularity will attempt to avoid shuffling tasks from this request, if possible.", nullable = true)
  public boolean getAvoidShuffle() {
    return avoidShuffle.orElse(false);
  }

  @Override
  public String toString() {
    return "SingularitySkipHealthchecksRequest{" +
        "avoidShuffle=" + avoidShuffle +
        "} " + super.toString();
  }
}
