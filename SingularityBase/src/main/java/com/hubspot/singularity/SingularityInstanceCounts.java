package com.hubspot.singularity;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;

public class SingularityInstanceCounts {
  private int activeDeployHealthy;
  private int activeDeployNotYetHealthy;
  private int activeDeployPending;
  private int activeDeployCleaning;

  private int pendingDeployHealthy;
  private int pendingDeployNotYetHealthy;
  private int pendingDeployPending;
  private int pendingDeployCleaning;

  @JsonCreator

  public SingularityInstanceCounts(int activeDeployHealthy,
                                   int activeDeployNotYetHealthy,
                                   int activeDeployPending,
                                   int activeDeployCleaning,
                                   int pendingDeployHealthy,
                                   int pendingDeployNotYetHealthy,
                                   int pendingDeployPending, int pendingDeployCleaning) {
    this.activeDeployHealthy = activeDeployHealthy;
    this.activeDeployNotYetHealthy = activeDeployNotYetHealthy;
    this.activeDeployPending = activeDeployPending;
    this.activeDeployCleaning = activeDeployCleaning;
    this.pendingDeployHealthy = pendingDeployHealthy;
    this.pendingDeployNotYetHealthy = pendingDeployNotYetHealthy;
    this.pendingDeployPending = pendingDeployPending;
    this.pendingDeployCleaning = pendingDeployCleaning;
  }

  public int getActiveDeployHealthy() {
    return activeDeployHealthy;
  }

  public int getActiveDeployNotYetHealthy() {
    return activeDeployNotYetHealthy;
  }

  public int getActiveDeployPending() {
    return activeDeployPending;
  }

  public int getActiveDeployCleaning() {
    return activeDeployCleaning;
  }

  public int getPendingDeployHealthy() {
    return pendingDeployHealthy;
  }

  public int getPendingDeployNotYetHealthy() {
    return pendingDeployNotYetHealthy;
  }

  public int getPendingDeployPending() {
    return pendingDeployPending;
  }

  public int getPendingDeployCleaning() {
    return pendingDeployCleaning;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof SingularityInstanceCounts) {
      final SingularityInstanceCounts that = (SingularityInstanceCounts) obj;
      return Objects.equals(this.activeDeployHealthy, that.activeDeployHealthy) &&
          Objects.equals(this.activeDeployNotYetHealthy, that.activeDeployNotYetHealthy) &&
          Objects.equals(this.activeDeployPending, that.activeDeployPending) &&
          Objects.equals(this.activeDeployCleaning, that.activeDeployCleaning) &&
          Objects.equals(this.pendingDeployHealthy, that.pendingDeployHealthy) &&
          Objects.equals(this.pendingDeployNotYetHealthy, that.pendingDeployNotYetHealthy) &&
          Objects.equals(this.pendingDeployPending, that.pendingDeployPending) &&
          Objects.equals(this.pendingDeployCleaning, that.pendingDeployCleaning);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(activeDeployHealthy, activeDeployNotYetHealthy, activeDeployPending, activeDeployCleaning, pendingDeployHealthy, pendingDeployNotYetHealthy, pendingDeployPending, pendingDeployCleaning);
  }

  @Override
  public String toString() {
    return "SingularityInstanceCounts{" +
        "activeDeployHealthy=" + activeDeployHealthy +
        ", activeDeployNotYetHealthy=" + activeDeployNotYetHealthy +
        ", activeDeployPending=" + activeDeployPending +
        ", activeDeployCleaning=" + activeDeployCleaning +
        ", pendingDeployHealthy=" + pendingDeployHealthy +
        ", pendingDeployNotYetHealthy=" + pendingDeployNotYetHealthy +
        ", pendingDeployPending=" + pendingDeployPending +
        ", pendingDeployCleaning=" + pendingDeployCleaning +
        '}';
  }
}
