package com.hubspot.singularity.helpers;

import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityDeploy;

public class SingularityRequestDeployHolder {

  private final Optional<SingularityDeploy> activeDeploy;
  private final Optional<SingularityDeploy> pendingDeploy;

  public SingularityRequestDeployHolder(Optional<SingularityDeploy> activeDeploy, Optional<SingularityDeploy> pendingDeploy) {
    this.activeDeploy = activeDeploy;
    this.pendingDeploy = pendingDeploy;
  }

  public Optional<SingularityDeploy> getActiveDeploy() {
    return activeDeploy;
  }

  public Optional<SingularityDeploy> getPendingDeploy() {
    return pendingDeploy;
  }

}
