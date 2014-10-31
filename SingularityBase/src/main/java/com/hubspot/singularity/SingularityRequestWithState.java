package com.hubspot.singularity;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Optional;

public class SingularityRequestWithState extends SingularityJsonObject {

  private final SingularityRequest request;
  private final RequestState state;

  public static Function<SingularityRequestWithState, String> REQUEST_STATE_TO_REQUEST_ID = new Function<SingularityRequestWithState, String>() {

    @Override
    public String apply(SingularityRequestWithState input) {
      return input.getRequest().getId();
    }

  };

  public static SingularityRequestWithState fromBytes(byte[] bytes, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(bytes, SingularityRequestWithState.class);
    } catch (IOException e) {
      throw new SingularityJsonException(e);
    }
  }

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
  public SingularityRequestWithState(@JsonProperty("request") SingularityRequest request, @JsonProperty("state") RequestState state) {
    this.request = request;
    this.state = state;
  }

  public RequestState getState() {
    return state;
  }

  public SingularityRequest getRequest() {
    return request;
  }

  @Override
  public String toString() {
    return "SingularityRequestWithState [request=" + request + ", state=" + state + "]";
  }

}
