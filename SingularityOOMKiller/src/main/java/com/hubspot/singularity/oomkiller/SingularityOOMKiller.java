package com.hubspot.singularity.oomkiller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.hubspot.mesos.client.MesosClient;
import com.hubspot.mesos.json.MesosExecutorObject;
import com.hubspot.mesos.json.MesosSlaveFrameworkObject;
import com.hubspot.mesos.json.MesosSlaveStateObject;
import com.hubspot.mesos.json.MesosTaskMonitorObject;
import com.hubspot.mesos.json.MesosTaskObject;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityTaskCleanupResult;
import com.hubspot.singularity.api.SingularityKillTaskRequest;
import com.hubspot.singularity.client.SingularityClient;
import com.hubspot.singularity.oomkiller.config.SingularityOOMKillerConfiguration;
import com.hubspot.singularity.runner.base.shared.ProcessFailedException;

public class SingularityOOMKiller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityOOMKiller.class);

  private final SingularityClient singularity;
  private final MesosClient mesos;
  private final SingularityOOMKillerConfiguration oomKillerConfiguration;
  private final SingularityCGroupProcLocator cgroupProcLocator;
  private final SingularityProcessKiller processKiller;
  private final SingularityOOMKillerMetrics metrics;

  @Inject
  public SingularityOOMKiller(MesosClient mesos, SingularityOOMKillerConfiguration oomKillerConfiguration, SingularityClient singularity, SingularityCGroupProcLocator cgroupProcLocator,
      SingularityProcessKiller processKiller, SingularityOOMKillerMetrics metrics) {
    this.mesos = mesos;
    this.oomKillerConfiguration = oomKillerConfiguration;
    this.singularity = singularity;
    this.processKiller = processKiller;
    this.cgroupProcLocator = cgroupProcLocator;
    this.metrics = metrics;
  }

  private double getOverageRatio(MesosTaskMonitorObject taskMonitor) {
    return (double) taskMonitor.getStatistics().getMemRssBytes() / (double) taskMonitor.getStatistics().getMemLimitBytes();
  }

  public void checkForOOMS() {
    List<MesosTaskMonitorObject> taskMonitors = mesos.getSlaveResourceUsage(oomKillerConfiguration.getSlaveHostname());
    List<MesosTaskMonitorObject> oomTaskMonitors = new ArrayList<>();

    for (MesosTaskMonitorObject taskMonitor : taskMonitors) {
      double useRatio = getOverageRatio(taskMonitor);

      if (useRatio > oomKillerConfiguration.getRequestKillThresholdRatio()) {
        LOG.info("Memory usage for {} is over limit {}/{} ({})", taskMonitor.getExecutorId(), taskMonitor.getStatistics().getMemRssBytes(), taskMonitor.getStatistics().getMemLimitBytes(), useRatio);
        oomTaskMonitors.add(taskMonitor);
      }
    }

    if (oomTaskMonitors.isEmpty()) {
      return;
    }

    metrics.getEligibleForKillMeter().mark(oomTaskMonitors.size());

    MesosSlaveStateObject slaveState = mesos.getSlaveState(oomKillerConfiguration.getSlaveHostname());

    Map<String, Map<String, MesosExecutorObject>> frameworksById = Maps.newHashMapWithExpectedSize(slaveState.getFrameworks().size());

    for (MesosSlaveFrameworkObject framework : slaveState.getFrameworks()) {
      Map<String, MesosExecutorObject> subMap = Maps.newHashMapWithExpectedSize(framework.getExecutors().size());
      for (MesosExecutorObject executor : framework.getExecutors()) {
        subMap.put(executor.getId(), executor);
      }
      frameworksById.put(framework.getId(), subMap);
    }

    for (MesosTaskMonitorObject oomTaskMonitor : oomTaskMonitors) {
      Map<String, MesosExecutorObject> executorsForFramework = frameworksById.get(oomTaskMonitor.getFrameworkId());

      if (executorsForFramework == null || !executorsForFramework.containsKey(oomTaskMonitor.getExecutorId())) {
        LOG.warn("Couldn't find a matching executor for oom task ({}-{})", oomTaskMonitor.getFrameworkId(), oomTaskMonitor.getExecutorId());
        metrics.getUnknownExecutorsMeter().mark();
        continue;
      }

      MesosExecutorObject executor = executorsForFramework.get(oomTaskMonitor.getExecutorId());

      double useRatio = getOverageRatio(oomTaskMonitor);

      if (useRatio > oomKillerConfiguration.getKillProcessDirectlyThresholdRatio()) {
        try {
          for (String pid : cgroupProcLocator.getPids(executor.getContainer())) {
            processKiller.killNow(pid);
            metrics.getOomHardKillsMeter().mark();
          }
        } catch (InterruptedException | ProcessFailedException e) {
          LOG.warn("Couldn't find pids for {}", executor.getContainer(), e);
          continue;
        }
      } else {
        for (MesosTaskObject task : executor.getCompletedTasks()) {
          Optional<SingularityTaskCleanupResult> taskCleanupResult = singularity.killTask(task.getId(), Optional.<SingularityKillTaskRequest> absent());

          if (taskCleanupResult.isPresent()) {
            LOG.info("Kill result {} for {}", taskCleanupResult.get().getResult(), task.getId());

            if (taskCleanupResult.get().getResult() == SingularityCreateResult.CREATED) {
              metrics.getOomSoftKillsMeter().mark();
            } else {
              metrics.getSingularityAlreadyKillingMeter().mark();
            }
          } else {
            metrics.getSingularityFailuresMeter().mark();

            LOG.info("Unable to request kill for ({})", task.getId());
          }
        }
      }
    }


  }


}
