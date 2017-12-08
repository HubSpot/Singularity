package com.hubspot.singularity.helpers;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.mesos.v1.Protos.AgentID;
import org.apache.mesos.v1.Protos.ExecutorID;
import org.apache.mesos.v1.Protos.FrameworkID;
import org.apache.mesos.v1.Protos.Offer;
import org.apache.mesos.v1.Protos.Resource;
import org.apache.mesos.v1.Protos.TaskID;
import org.apache.mesos.v1.Protos.TaskInfo;
import org.apache.mesos.v1.Protos.TaskState;
import org.apache.mesos.v1.Protos.TaskStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.mesos.protos.MesosOfferObject;
import com.hubspot.mesos.protos.MesosResourceObject;
import com.hubspot.mesos.protos.MesosStringValue;
import com.hubspot.mesos.protos.MesosTaskObject;
import com.hubspot.mesos.protos.MesosTaskStatusObject;
import com.hubspot.singularity.ExtendedTaskState;

public class MesosProtosUtils {
  private final ObjectMapper objectMapper;

  @Inject
  public MesosProtosUtils(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public MesosTaskObject taskFromProtos(TaskInfo taskInfo) {
    return objectMapper.convertValue(taskInfo, MesosTaskObject.class);
  }

  public MesosOfferObject offerFromProtos(Offer offer) {
    return objectMapper.convertValue(offer, MesosOfferObject.class);
  }

  public static AgentID toAgentId(MesosStringValue stringValue) {
    return AgentID.newBuilder().setValue(stringValue.getValue()).build();
  }

  public static TaskID toTaskId(MesosStringValue stringValue) {
    return TaskID.newBuilder().setValue(stringValue.getValue()).build();
  }

  public static ExecutorID toExecutorId(MesosStringValue stringValue) {
    return ExecutorID.newBuilder().setValue(stringValue.getValue()).build();
  }

  public static FrameworkID toFrameworkId(MesosStringValue stringValue) {
    return FrameworkID.newBuilder().setValue(stringValue.getValue()).build();
  }

  public List<Resource> toResourceList(List<MesosResourceObject> resourceObjects) {
    return resourceObjects.stream()
        .map((r) -> objectMapper.convertValue(r, Resource.class))
        .collect(Collectors.toList());
  }

  public MesosTaskStatusObject taskStatusFromProtos(TaskStatus status) {
    return objectMapper.convertValue(status, MesosTaskStatusObject.class);
  }

  public static TaskState toTaskState(ExtendedTaskState extendedTaskState) {
    return TaskState.valueOf(extendedTaskState.toTaskState().get().name());
  }
}
