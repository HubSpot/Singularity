package com.hubspot.singularity.resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.ClusterUtilization;
import com.hubspot.singularity.RequestUtilization;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularitySlaveUsage;
import com.hubspot.singularity.SingularitySlaveUsageWithId;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCurrentUsageWithId;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskUsage;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.UsageManager;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiParam;

@Path(UsageResource.PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api(description="Provides usage data about slaves and tasks", value=UsageResource.PATH)
public class UsageResource {

  public static final String PATH = SingularityService.API_BASE_PATH + "/usage";

  private final UsageManager usageManager;
  private final TaskManager taskManager;
  private final SlaveManager slaveManager;
  private final DeployManager deployManager;

  @Inject
  public UsageResource(UsageManager usageManager, TaskManager taskManager, SlaveManager slaveManager, DeployManager deployManager) {
    this.usageManager = usageManager;
    this.taskManager = taskManager;
    this.slaveManager = slaveManager;
    this.deployManager = deployManager;
  }

  @GET
  @Path("/slaves")
  public List<SingularitySlaveUsageWithId> getSlavesWithUsage() {
    return usageManager.getAllCurrentSlaveUsage();
  }

  @GET
  @Path("/slaves/{slaveId}/tasks/current")
  public List<SingularityTaskCurrentUsageWithId> getSlaveCurrentTaskUsage(@PathParam("slaveId") String slaveId) {
    Optional<SingularitySlave> slave = slaveManager.getObject(slaveId);

    WebExceptions.checkNotFound(slave.isPresent(), "No slave found with id %s", slaveId);

    List<SingularityTask> tasksOnSlave = taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), slave.get());

    List<SingularityTaskId> taskIds = new ArrayList<>(tasksOnSlave.size());
    for (SingularityTask task : tasksOnSlave) {
      taskIds.add(task.getTaskId());
    }

    return usageManager.getTaskCurrentUsages(taskIds);
  }

  @GET
  @Path("/slaves/{slaveId}/history")
  public List<SingularitySlaveUsage> getSlaveUsageHistory(@PathParam("slaveId") String slaveId) {
    return usageManager.getSlaveUsage(slaveId);
  }

  @GET
  @Path("/tasks/{taskId}/history")
  public List<SingularityTaskUsage> getTaskUsageHistory(@PathParam("taskId") String taskId) {
    return usageManager.getTaskUsage(taskId);
  }

  @GET
  @Path("/cluster/utilization")
  public ClusterUtilization getClusterUtilization(@ApiParam("For under-utilized requests, only consider those that are under-utilized greater than or equal to this percentage") @QueryParam("minUnderUtilizedPct") double minUnderUtilizedPct) {
    List<SingularityTaskId> tasks = usageManager.getTasksWithUsage().stream().map(SingularityTaskId::valueOf).collect(Collectors.toList());

    Map<String, RequestUtilization> utilizationPerRequestId = new HashMap<>();

    for (SingularityTaskId task : tasks) {
      RequestUtilization requestUtilization = utilizationPerRequestId.getOrDefault(task.getRequestId(), new RequestUtilization(task.getRequestId(), task.getDeployId()));

      usageManager.getTaskUsage(task.getId()).forEach(usage -> {
        requestUtilization.addCpu(usage.getCpuSeconds());
        requestUtilization.addMemBytes(usage.getMemoryRssBytes());
        requestUtilization.incrementTaskCount();
      });

      utilizationPerRequestId.put(task.getRequestId(), requestUtilization);
    }

    return getClusterUtilization(utilizationPerRequestId, minUnderUtilizedPct);
  }

  private ClusterUtilization getClusterUtilization(Map<String, RequestUtilization> utilizationPerRequestId, double minUnderUtilizedPct) {
    int numRequestsWithUnderUtilizedCpu = 0;
    int numRequestsWithOverUtilizedCpu = 0;
    int numRequestsWithUnderUtilizedMemBytes = 0;

    double totalUnderUtilizedCpu = 0;
    double totalOverUtilizedCpu = 0;
    long totalUnderUtilizedMemBytes = 0;

    double maxUnderUtilizedCpu = Double.MIN_VALUE;
    double maxOverUtilizedCpu = Double.MIN_VALUE;
    long maxUnderUtilizedMemBytes = Long.MIN_VALUE;

    double minUnderUtilizedCpu = Double.MAX_VALUE;
    double minOverUtilizedCpu = Double.MAX_VALUE;
    long minUnderUtilizedMemBytes = Long.MAX_VALUE;


    for (Iterator<Map.Entry<String, RequestUtilization>> it = utilizationPerRequestId.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<String, RequestUtilization> entry = it.next();
      RequestUtilization utilization = entry.getValue();
      Optional<SingularityDeploy> maybeDeploy = deployManager.getDeploy(utilization.getRequestId(), utilization.getDeployId());
      if (maybeDeploy.isPresent() && maybeDeploy.get().getResources().isPresent()) {
        boolean includeUtilization = true;
        long memoryBytesReserved = (long) (maybeDeploy.get().getResources().get().getMemoryMb() * SingularitySlaveUsage.BYTES_PER_MEGABYTE);
        double cpuReserved = maybeDeploy.get().getResources().get().getCpus();

        double unusedCpu = cpuReserved - utilization.getAvgCpuUsed();
        long unusedMemBytes = memoryBytesReserved - utilization.getMemBytesTotal();

        if (unusedCpu / cpuReserved >= minUnderUtilizedPct) {
          numRequestsWithUnderUtilizedCpu++;
          totalUnderUtilizedCpu += unusedCpu;

          if (unusedCpu > maxUnderUtilizedCpu) {
            maxUnderUtilizedCpu = unusedCpu;
          }
          if (unusedCpu < minUnderUtilizedCpu) {
            minUnderUtilizedCpu = unusedCpu;
          }
        } else if (unusedCpu < 0) {
          numRequestsWithOverUtilizedCpu++;
          double overusedCpu = Math.abs(unusedCpu);
          totalOverUtilizedCpu += overusedCpu;

          if (overusedCpu > maxOverUtilizedCpu) {
            maxOverUtilizedCpu = overusedCpu;
          }
          if (overusedCpu < minOverUtilizedCpu) {
            minOverUtilizedCpu = overusedCpu;
          }
        } else {
          includeUtilization = false;
        }

        if (unusedMemBytes / memoryBytesReserved >= minUnderUtilizedPct) {
          numRequestsWithUnderUtilizedMemBytes++;
          totalUnderUtilizedMemBytes += unusedMemBytes;

          if (unusedMemBytes > maxUnderUtilizedMemBytes) {
            maxUnderUtilizedMemBytes = unusedMemBytes;
          }
          if (unusedMemBytes < minUnderUtilizedMemBytes) {
            minUnderUtilizedMemBytes = unusedMemBytes;
          }
        } else if (!includeUtilization) {
          it.remove();
        }
      }
    }

    double avgUnderUtilizedCpu = totalUnderUtilizedCpu / numRequestsWithUnderUtilizedCpu;
    double avgOverUtilizedCpu = totalOverUtilizedCpu / numRequestsWithOverUtilizedCpu;
    double avgUnderUtilizedMemBytes = totalUnderUtilizedMemBytes / numRequestsWithUnderUtilizedMemBytes;

    return new ClusterUtilization(new ArrayList<>(utilizationPerRequestId.values()), numRequestsWithUnderUtilizedCpu, numRequestsWithOverUtilizedCpu,
        numRequestsWithUnderUtilizedMemBytes, totalUnderUtilizedCpu, totalOverUtilizedCpu, totalUnderUtilizedMemBytes, avgUnderUtilizedCpu,
        avgOverUtilizedCpu, avgUnderUtilizedMemBytes, maxUnderUtilizedCpu, maxOverUtilizedCpu, maxUnderUtilizedMemBytes, minUnderUtilizedCpu,
        minOverUtilizedCpu, minUnderUtilizedMemBytes);
  }
}
