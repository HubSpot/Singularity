package com.hubspot.mesos;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.mesos.v1.Protos.AgentID;
import org.apache.mesos.v1.Protos.ExecutorID;
import org.apache.mesos.v1.Protos.FrameworkID;
import org.apache.mesos.v1.Protos.Offer;
import org.apache.mesos.v1.Protos.Resource;
import org.apache.mesos.v1.Protos.TaskInfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.mesos.protos.MesosOfferObject;
import com.hubspot.mesos.protos.MesosResourceObject;
import com.hubspot.mesos.protos.MesosStringValue;
import com.hubspot.mesos.protos.MesosTaskObject;

public class MesosProtosUtils {
  private final ObjectMapper objectMapper;

  @Inject
  public MesosProtosUtils(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }
  public MesosTaskObject taskFromProtos(TaskInfo taskInfo) {
    try {
      return objectMapper.readValue(objectMapper.writeValueAsString(taskInfo), MesosTaskObject.class);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  public MesosOfferObject offerFromProtos(Offer offer) {
    try {
      return objectMapper.readValue(objectMapper.writeValueAsString(offer), MesosOfferObject.class);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  public AgentID toAgentId(MesosStringValue stringValue) {
    return AgentID.newBuilder().setValue(stringValue.getValue()).build();
  }

  public ExecutorID toExecutorId(MesosStringValue stringValue) {
    return ExecutorID.newBuilder().setValue(stringValue.getValue()).build();
  }

  public FrameworkID toFrameworkId(MesosStringValue stringValue) {
    return FrameworkID.newBuilder().setValue(stringValue.getValue()).build();
  }

  public List<Resource> toResourceList(List<MesosResourceObject> resourceObjects) {
    return resourceObjects.stream()
        .map((r) -> {
          try {
            return objectMapper.readValue(objectMapper.writeValueAsString(r), Resource.class);
          } catch (IOException ioe) {
            throw new RuntimeException(ioe);
          }
        })
        .collect(Collectors.toList());
  }
}
