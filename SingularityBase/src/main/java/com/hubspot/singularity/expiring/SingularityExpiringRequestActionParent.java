package com.hubspot.singularity.expiring;

import com.hubspot.singularity.api.SingularityExpiringRequestParent;

public abstract class SingularityExpiringRequestActionParent<T extends SingularityExpiringRequestParent> extends SingularityExpiringParent<T> {
  public abstract String getRequestId();
}
