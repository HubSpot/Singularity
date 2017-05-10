package com.hubspot.singularity;

import java.util.List;

import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;

import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;

@Immutable
@SingularityStyle
public abstract class AbstractSingularityState {

  public abstract int getActiveTasks();

  public abstract int getLaunchingTasks();

  public abstract int getActiveRequests();

  public abstract int getCooldownRequests();

  public abstract int getPausedRequests();

  public abstract int getScheduledTasks();

  public abstract int getPendingRequests();

  public abstract int getLbCleanupTasks();

  public abstract int getLbCleanupRequests();

  public abstract int getCleaningRequests();

  public abstract int getActiveSlaves();

  public abstract int getDeadSlaves();

  public abstract int getDecommissioningSlaves();

  public abstract int getActiveRacks();

  public abstract int getDeadRacks();

  public abstract int getDecommissioningRacks();

  public abstract int getCleaningTasks();

  public abstract List<SingularityHostState> getHostStates();

  public abstract long getOldestDeploy();

  public abstract int getNumDeploys();

  public abstract long getOldestDeployStep();

  public abstract List<SingularityDeployMarker> getActiveDeploys();

  public abstract int getLateTasks();

  public abstract int getFutureTasks();

  public abstract long getMaxTaskLag();

  public abstract long getGeneratedAt();

  public abstract List<String> getOverProvisionedRequestIds();

  public abstract List<String> getUnderProvisionedRequestIds();

  public abstract int getOverProvisionedRequests();

  public abstract int getUnderProvisionedRequests();

  public abstract int getFinishedRequests();

  public abstract int getUnknownRacks();

  public abstract int getUnknownSlaves();

  public abstract Optional<Boolean> getAuthDatastoreHealthy();

  public abstract Optional<Double> getMinimumPriorityLevel();

  public abstract long getAvgStatusUpdateDelayMs();

  @Derived
  public int getAllRequests() {
    return getActiveRequests() + getCooldownRequests() + getPausedRequests();
  }

}
