package com.hubspot.singularity.api.common;

import java.util.List;
import java.util.Optional;

import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyle;
import com.hubspot.singularity.api.deploy.SingularityDeployMarker;
import com.hubspot.singularity.api.machines.SingularityHostState;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "Describes the current state of singularity")
public abstract class AbstractSingularityState {
  @Schema(description = "The count of active tasks")
  public abstract int getActiveTasks();

  @Schema(description = "The count of tasks in launching state")
  public abstract int getLaunchingTasks();

  @Schema(description = "The count of requests in active state")
  public abstract int getActiveRequests();

  @Schema(description = "The count of requests in cooldown state")
  public abstract int getCooldownRequests();

  @Schema(description = "Count of requests in paused state")
  public abstract int getPausedRequests();

  @Schema(description = "The count of tasks waiting to be launched")
  public abstract int getScheduledTasks();

  @Schema(
      title = "The count of pending requests",
      description = "A pending request is a trigger for the scheduler to perform an action for a request (create a cleanup, launch a task, etc)"
  )
  public abstract int getPendingRequests();

  @Schema(description = "The count of tasks with associated load balancer cleanups")
  public abstract int getLbCleanupTasks();

  @Schema(description = "The count of requests with associated load balancer cleanups")
  public abstract int getLbCleanupRequests();

  @Schema(description = "The count of requests with associated cleanups (e.g. due to a bounce)")
  public abstract int getCleaningRequests();

  @Schema(description = "The count of active slaves")
  public abstract int getActiveSlaves();

  @Schema(description = "The count of dead slaves (no longer reachable or considered lost by mesos)")
  public abstract int getDeadSlaves();

  @Schema(description = "The count of slaves currently decommissioning")
  public abstract int getDecommissioningSlaves();

  @Schema(description = "The count of active racks")
  public abstract int getActiveRacks();

  @Schema(description = "The count of racks considered dead")
  public abstract int getDeadRacks();

  @Schema(description = "The count of racks that are currently decommissioning")
  public abstract int getDecommissioningRacks();

  @Schema(description = "The count of cleaning tasks")
  public abstract int getCleaningTasks();

  @Schema(description = "Describes the state of all Singularity scheduler instances")
  public abstract List<SingularityHostState> getHostStates();

  @Schema(description = "Timestamp of the oldest running deploy")
  public abstract long getOldestDeploy();

  @Schema(description = "Number of active/in-progress deploys")
  public abstract int getNumDeploys();

  @Schema(
      title = "Timestamp of the oldest single step for a deploy",
      description = "For incremental deploys this is a measure of the time taken for a single step of the deploy"
  )
  public abstract long getOldestDeployStep();

  @Schema(description = "List of active deploy identifiers")
  public abstract List<SingularityDeployMarker> getActiveDeploys();

  @Schema(description = "The count of tasks that have not been launched in time")
  public abstract int getLateTasks();

  @Schema(description = "The count of pending tasks that will be launched at a future time")
  public abstract int getFutureTasks();

  @Schema(description = "The maximum delay in launching any pending task")
  public abstract long getMaxTaskLag();

  @Schema(description = "Time this state was generated")
  public abstract long getGeneratedAt();

  @Schema(description = "The count of requests running too many instances")
  public abstract List<String> getOverProvisionedRequestIds();

  @Schema(description = "The ids of requests running too many instances")
  public abstract List<String> getUnderProvisionedRequestIds();

  @Schema(description = "The count of requests running too few instances")
  public abstract int getOverProvisionedRequests();

  @Schema(description = "The ids of requests running too few instances")
  public abstract int getUnderProvisionedRequests();

  @Schema(description = "Count of requests in finished state")
  public abstract int getFinishedRequests();

  @Schema(description = "The count of racks in an unknown state")
  public abstract int getUnknownRacks();

  @Schema(description = "The count of slaves in an unknown state")
  public abstract int getUnknownSlaves();

  @Schema(description = "`true` if the auth datastore is reachable (when auth is configured)", nullable = true)
  public abstract Optional<Boolean> getAuthDatastoreHealthy();

  @Schema(description = "The minimum priority level for launching tasks if a priority freeze is active, empty otherwise", nullable = true)
  public abstract Optional<Double> getMinimumPriorityLevel();

  @Schema(description = "The average delay (in millis) for processing status updates from mesos")
  public abstract long getAvgStatusUpdateDelayMs();

  @Derived
  @Schema(description = "The count of all requests in all states")
  public int getAllRequests() {
    return getActiveRequests() + getCooldownRequests() + getPausedRequests();
  }

}
