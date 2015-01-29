package com.hubspot.singularity.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.mesos.CounterMap;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityHostState;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityScheduledTasksInfo;
import com.hubspot.singularity.SingularityState;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.Transcoder;

@Singleton
public class StateManager extends CuratorManager {

  private static final Logger LOG = LoggerFactory.getLogger(StateManager.class);

  private static final String ROOT_PATH = "/hosts";
  private static final String STATE_PATH = "STATE";

  private final RequestManager requestManager;
  private final TaskManager taskManager;
  private final DeployManager deployManager;
  private final SlaveManager slaveManager;
  private final RackManager rackManager;
  private final Transcoder<SingularityState> stateTranscoder;
  private final Transcoder<SingularityHostState> hostStateTranscoder;
  private final SingularityConfiguration singularityConfiguration;

  @Inject
  public StateManager(CuratorFramework curatorFramework, RequestManager requestManager, TaskManager taskManager, DeployManager deployManager, SlaveManager slaveManager, RackManager rackManager,
      Transcoder<SingularityState> stateTranscoder, Transcoder<SingularityHostState> hostStateTranscoder, SingularityConfiguration singularityConfiguration) {
    super(curatorFramework);

    this.requestManager = requestManager;
    this.taskManager = taskManager;
    this.stateTranscoder = stateTranscoder;
    this.hostStateTranscoder = hostStateTranscoder;
    this.slaveManager = slaveManager;
    this.rackManager = rackManager;
    this.deployManager = deployManager;
    this.singularityConfiguration = singularityConfiguration;
  }

  public void save(SingularityHostState hostState) throws InterruptedException {
    final String path = ZKPaths.makePath(ROOT_PATH, hostState.getHostname());
    final byte[] data = hostStateTranscoder.toBytes(hostState);

    if (curator.getState() == CuratorFrameworkState.STARTED) {
      try {
        if (exists(path)) {
          curator.setData().forPath(path, data);
        } else {
          curator.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path, data);
        }
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        throw ie;
      } catch (Throwable t) {
        throw Throwables.propagate(t);
      }
    }
  }

  public List<SingularityHostState> getHostStates() {
    List<String> children = getChildren(ROOT_PATH);
    List<SingularityHostState> states = Lists.newArrayListWithCapacity(children.size());

    for (String child : children) {

      try {
        byte[] bytes = curator.getData().forPath(ZKPaths.makePath(ROOT_PATH, child));

        states.add(hostStateTranscoder.fromBytes(bytes));
      } catch (NoNodeException nne) {
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }

    return states;
  }

  private Map<String, Long> getNumTasks(List<SingularityRequestWithState> requests) {
    final CounterMap<String> numTasks = new CounterMap<>(requests.size());

    for (SingularityTaskId taskId : taskManager.getActiveTaskIds()) {
      numTasks.incr(taskId.getRequestId());
    }

    for (SingularityPendingTaskId pendingTaskId : taskManager.getPendingTaskIds()) {
      numTasks.incr(pendingTaskId.getRequestId());
    }

    return numTasks.toCountMap();
  }

  public SingularityState getState(boolean skipCache, boolean includeRequestIds) {
    Optional<SingularityState> fromZk = Optional.absent();

    if (!skipCache) {
      fromZk = getData(STATE_PATH, stateTranscoder);
    }

    if (fromZk.isPresent()) {
      final long now = System.currentTimeMillis();
      final long delta = now - fromZk.get().getGeneratedAt();

      if (delta < singularityConfiguration.getCacheStateForMillis()) {
        return fromZk.get();
      }
    }

    final long start = System.currentTimeMillis();

    SingularityState newState = generateState(includeRequestIds);

    if (!skipCache) {
      final byte[] bytes = stateTranscoder.toBytes(newState);
      save(STATE_PATH, newState, stateTranscoder);

      LOG.info("Generated new state and saved {} bytes in {}", bytes.length, JavaUtils.duration(start));
    }

    return newState;
  }

  public SingularityState generateState(boolean includeRequestIds) {
    final int activeTasks = taskManager.getNumActiveTasks();
    final int scheduledTasks = taskManager.getNumScheduledTasks();
    final int cleaningTasks = taskManager.getNumCleanupTasks();
    final int lbCleanupTasks = taskManager.getNumLbCleanupTasks();

    final SingularityScheduledTasksInfo scheduledTasksInfo = SingularityScheduledTasksInfo.getInfo(taskManager.getPendingTasks(), singularityConfiguration.getDeltaAfterWhichTasksAreLateMillis());

    final List<String> overProvisionedRequestIds = new ArrayList<>();
    final List<String> possiblyUnderProvisionedRequestIds = new ArrayList<>();

    final List<SingularityRequestWithState> requests = requestManager.getRequests();

    final Map<String, Long> numInstances = getNumTasks(requests);

    int numActiveRequests = 0;
    int numPausedRequests = 0;
    int cooldownRequests = 0;
    int numFinishedRequests = 0;

    for (SingularityRequestWithState requestWithState : requests) {
      switch (requestWithState.getState()) {
        case DEPLOYING_TO_UNPAUSE:
        case ACTIVE:
          numActiveRequests++;
          break;
        case FINISHED:
          numFinishedRequests++;
          break;
        case PAUSED:
          numPausedRequests++;
          break;
        case SYSTEM_COOLDOWN:
          cooldownRequests++;
          break;
        case DELETED:
          break;
      }

      if (requestWithState.getState().isRunnable() && !requestWithState.getRequest().isOneOff()) {
        final int instances = requestWithState.getRequest().getInstancesSafe();

        final Long numActualInstances = numInstances.get(requestWithState.getRequest().getId());

        if (numActualInstances == null || numActualInstances.longValue() < instances) {
          possiblyUnderProvisionedRequestIds.add(requestWithState.getRequest().getId());
        } else if (numActualInstances.longValue() > instances) {
          overProvisionedRequestIds.add(requestWithState.getRequest().getId());
        }
      }
    }

    final List<String> underProvisionedRequestIds = new ArrayList<>(possiblyUnderProvisionedRequestIds.size());
    if (!possiblyUnderProvisionedRequestIds.isEmpty()) {
      Map<String, SingularityRequestDeployState> deployStates = deployManager.getRequestDeployStatesByRequestIds(possiblyUnderProvisionedRequestIds);

      for (SingularityRequestDeployState deployState : deployStates.values()) {
        if (deployState.getActiveDeploy().isPresent() || deployState.getPendingDeploy().isPresent()) {
          underProvisionedRequestIds.add(deployState.getRequestId());
        }
      }
    }

    final int pendingRequests = requestManager.getSizeOfPendingQueue();
    final int cleaningRequests = requestManager.getSizeOfCleanupQueue();

    final int activeRacks = rackManager.getNumActive();
    final int deadRacks = rackManager.getNumDead();
    final int decomissioningRacks = rackManager.getNumDecomissioning();

    final int activeSlaves = slaveManager.getNumActive();
    final int deadSlaves = slaveManager.getNumDead();
    final int decomissioningSlaves = slaveManager.getNumDecomissioning();

    final List<SingularityHostState> states = getHostStates();

    int numDeploys = 0;
    long oldestDeploy = 0;
    final long now = System.currentTimeMillis();

    for (SingularityPendingDeploy pendingDeploy : deployManager.getPendingDeploys()) {
      long delta = now - pendingDeploy.getDeployMarker().getTimestamp();
      if (delta > oldestDeploy) {
        oldestDeploy = delta;
      }
      numDeploys++;
    }

    return new SingularityState(activeTasks, numActiveRequests, cooldownRequests, numPausedRequests, scheduledTasks, pendingRequests, lbCleanupTasks, cleaningRequests, activeSlaves,
        deadSlaves, decomissioningSlaves, activeRacks, deadRacks, decomissioningRacks, cleaningTasks, states, oldestDeploy, numDeploys, scheduledTasksInfo.getNumLateTasks(),
        scheduledTasksInfo.getNumFutureTasks(), scheduledTasksInfo.getMaxTaskLag(), System.currentTimeMillis(), includeRequestIds ? overProvisionedRequestIds : null,
        includeRequestIds ? underProvisionedRequestIds : null, overProvisionedRequestIds.size(), underProvisionedRequestIds.size(), numFinishedRequests);
  }

}
