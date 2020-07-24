package com.hubspot.singularity.mesos;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.mesos.protos.MesosOfferObject;
import com.hubspot.singularity.SingularityDeployBuilder;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.helpers.MesosProtosUtils;
import com.hubspot.singularity.helpers.SingularityMesosTaskHolder;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.mesos.v1.Protos.TaskInfo;

@Singleton
public class SingularityTaskSizeOptimizer {
  private final SingularityConfiguration configuration;
  private final MesosProtosUtils mesosProtosUtils;

  @Inject
  public SingularityTaskSizeOptimizer(
    SingularityConfiguration configuration,
    MesosProtosUtils mesosProtosUtils
  ) {
    this.configuration = configuration;
    this.mesosProtosUtils = mesosProtosUtils;
  }

  SingularityTask getSizeOptimizedTask(SingularityMesosTaskHolder taskHolder) {
    if (configuration.isStoreAllMesosTaskInfoForDebugging()) {
      return taskHolder.getTask();
    }

    SingularityTask task = taskHolder.getTask();

    TaskInfo.Builder mesosTask = taskHolder.getMesosTask().toBuilder();

    mesosTask.clearData();

    List<MesosOfferObject> offers = task
      .getOffers()
      .stream()
      .map(MesosOfferObject::sizeOptimized)
      .collect(Collectors.toList());

    SingularityTaskRequest taskRequest = task.getTaskRequest();

    if (task.getTaskRequest().getDeploy().getExecutorData().isPresent()) {
      SingularityDeployBuilder deploy = task.getTaskRequest().getDeploy().toBuilder();

      deploy.setExecutorData(Optional.empty());

      taskRequest =
        new SingularityTaskRequest(
          task.getTaskRequest().getRequest(),
          deploy.build(),
          task.getTaskRequest().getPendingTask()
        );
    }

    return new SingularityTask(
      taskRequest,
      task.getTaskId(),
      offers,
      mesosProtosUtils.taskFromProtos(mesosTask.build()),
      task.getRackId()
    );
  }
}
