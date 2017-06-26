package com.hubspot.singularity.expiring;

import org.immutables.value.Value.Immutable;

import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;
import com.hubspot.singularity.api.SingularityBounceRequest;

@Immutable
@SingularityStyle
public abstract class AbstractSingularityExpiringBounce extends SingularityExpiringRequestActionParent<SingularityBounceRequest> {

  public abstract String getRequestId();

  public abstract String getDeployId();

  public abstract Optional<String> getUser();

  public abstract long getStartMillis();

  public abstract SingularityBounceRequest getExpiringAPIRequestObject();

  public abstract String getActionId();

}
