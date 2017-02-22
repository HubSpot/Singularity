package com.hubspot.singularity;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Function;
import com.google.common.base.Optional;

public class SingularityRequestWithState {

  private final SingularityRequest request;
  private final RequestState state;
  private final long timestamp;

  public static Function<SingularityRequestWithState, String> REQUEST_STATE_TO_REQUEST_ID = new Function<SingularityRequestWithState, String>() {

    @Override
    public String apply(@Nonnull SingularityRequestWithState input) {
      return input.getRequest().getId();
    }

  };

  public static String getRequestState(Optional<SingularityRequestWithState> maybeRequestWithState) {
    if (maybeRequestWithState.isPresent()) {
      return maybeRequestWithState.get().getState().name();
    }
    return "MISSING";
  }

  public static boolean isActive(Optional<SingularityRequestWithState> maybeRequestWithState) {
    return maybeRequestWithState.isPresent() && maybeRequestWithState.get().getState().isRunnable();
  }

  @JsonCreator
  public SingularityRequestWithState(@JsonProperty("request") SingularityRequest request, @JsonProperty("state") RequestState state, @JsonProperty("timestamp") long timestamp) {
    this.request = request;
    this.state = state;
    this.timestamp = timestamp;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public RequestState getState() {
    return state;
  }

  public SingularityRequest getRequest() {
    return request;
  }

  @Override
  public String toString() {
    return "SingularityRequestWithState{" +
        "request=" + request +
        ", state=" + state +
        ", timestamp=" + timestamp +
        '}';
  }
}
