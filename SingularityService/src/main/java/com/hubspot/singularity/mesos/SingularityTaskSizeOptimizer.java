package com.hubspot.singularity.mesos;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.mesos.v1.Protos.TaskInfo;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.mesos.json.SingularityMesosOfferObject;
import com.hubspot.mesos.json.SingularityMesosTaskObject;
import com.hubspot.singularity.SingularityDeployBuilder;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.config.SingularityConfiguration;

@Singleton
public class SingularityTaskSizeOptimizer {

  private final SingularityConfiguration configuration;

  @Inject
  public SingularityTaskSizeOptimizer(SingularityConfiguration configuration) {
    this.configuration = configuration;
  }

  SingularityTask getSizeOptimizedTask(SingularityTask task) {
    if (configuration.isStoreAllMesosTaskInfoForDebugging()) {
      return task;
    }

    TaskInfo.Builder mesosTask = task.getMesosTaskProtos().toBuilder();

    mesosTask.clearData();

    List<SingularityMesosOfferObject> offers = task.getOffers()
        .stream()
        .map(SingularityMesosOfferObject::sizeOptimized)
        .collect(Collectors.toList());

    SingularityTaskRequest taskRequest = task.getTaskRequest();

    if (task.getTaskRequest().getDeploy().getExecutorData().isPresent()) {

      SingularityDeployBuilder deploy = task.getTaskRequest().getDeploy().toBuilder();

      deploy.setExecutorData(Optional.absent());

      taskRequest = new SingularityTaskRequest(task.getTaskRequest().getRequest(),
          deploy.build(), task.getTaskRequest().getPendingTask());
    }

    return new SingularityTask(taskRequest, task.getTaskId(), offers, SingularityMesosTaskObject.fromProtos(mesosTask.build()), task.getRackId());
  }

}
