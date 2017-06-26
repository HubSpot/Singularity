package com.hubspot.singularity.expiring;

import org.immutables.value.Value.Immutable;

import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;
import com.hubspot.singularity.api.SingularityPauseRequest;

@Immutable
@SingularityStyle
public abstract class AbstractSingularityExpiringPause extends SingularityExpiringRequestActionParent<SingularityPauseRequest> {
  public abstract String getRequestId();
  public abstract SingularityPauseRequest getExpiringAPIRequestObject();
  public abstract Optional<String> getUser();
  public abstract long getStartMillis();
  public abstract String getActionId();
}
