package com.hubspot.singularity.api.request;

import java.util.Optional;

import org.immutables.value.Value.Immutable;

import com.google.common.base.Function;
import com.hubspot.singularity.annotations.SingularityStyle;

@Immutable
@SingularityStyle
public abstract class AbstractSingularityRequestWithState {
  public static Function<SingularityRequestWithState, String> REQUEST_STATE_TO_REQUEST_ID = (input) -> input.getRequest().getId();

  public static String getRequestState(Optional<SingularityRequestWithState> maybeRequestWithState) {
    if (maybeRequestWithState.isPresent()) {
      return maybeRequestWithState.get().getState().name();
    }
    return "MISSING";
  }

  public static boolean isActive(Optional<SingularityRequestWithState> maybeRequestWithState) {
    return maybeRequestWithState.isPresent() && maybeRequestWithState.get().getState().isRunnable();
  }


  public abstract SingularityRequest getRequest();

  public abstract RequestState getState();

  public abstract long getTimestamp();
}
