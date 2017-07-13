package com.hubspot.mesos.json;

import java.util.Collections;
import java.util.List;

import org.apache.mesos.v1.Protos.AgentID;
import org.apache.mesos.v1.Protos.CommandInfo;
import org.apache.mesos.v1.Protos.ContainerInfo;
import org.apache.mesos.v1.Protos.DiscoveryInfo;
import org.apache.mesos.v1.Protos.ExecutorInfo;
import org.apache.mesos.v1.Protos.HealthCheck;
import org.apache.mesos.v1.Protos.KillPolicy;
import org.apache.mesos.v1.Protos.Labels;
import org.apache.mesos.v1.Protos.Resource;
import org.apache.mesos.v1.Protos.TaskID;
import org.apache.mesos.v1.Protos.TaskInfo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

/*
 * Mimics the TaskInfo object from mesos, with the addition that we can read
 * AgentID from either a field named slaveId or a field named agentId for
 * better backwards compatibility
 */
public class SingularityMesosTaskObject {
  private final TaskID taskId;
  private final Optional<ExecutorInfo> executor;
  private final Optional<Labels> labels;
  private final AgentID agentId;
  private final AgentID slaveId;
  private final List<Resource> resources;
  private final Optional<CommandInfo> command;
  private final Optional<ContainerInfo> container;
  private final Optional<DiscoveryInfo> discovery;
  private final Optional<HealthCheck> healthCheck;
  private final Optional<KillPolicy> killPolicy;
  private final String name;

  public static SingularityMesosTaskObject fromProtos(TaskInfo taskInfo) {
    return new SingularityMesosTaskObject(
        taskInfo.getTaskId(),
        taskInfo.hasExecutor() ? Optional.of(taskInfo.getExecutor()) : Optional.absent(),
        taskInfo.hasLabels() ? Optional.of(taskInfo.getLabels()) : Optional.absent(),
        taskInfo.getAgentId(),
        null,
        taskInfo.getResourcesList(),
        taskInfo.hasCommand() ? Optional.of(taskInfo.getCommand()) : Optional.absent(),
        taskInfo.hasContainer() ? Optional.of(taskInfo.getContainer()) : Optional.absent(),
        taskInfo.hasDiscovery() ? Optional.of(taskInfo.getDiscovery()) : Optional.absent(),
        taskInfo.hasHealthCheck() ? Optional.of(taskInfo.getHealthCheck()) : Optional.absent(),
        taskInfo.hasKillPolicy() ? Optional.of(taskInfo.getKillPolicy()) : Optional.absent(),
        taskInfo.getName()
    );
  }

  @JsonCreator
  public SingularityMesosTaskObject(@JsonProperty("taskId") TaskID taskId,
                                    @JsonProperty("executor") Optional<ExecutorInfo> executor,
                                    @JsonProperty("labels") Optional<Labels> labels,
                                    @JsonProperty("agentId") AgentID agentId,
                                    @JsonProperty("slaveId") AgentID slaveId,
                                    @JsonProperty("resources") List<Resource> resources,
                                    @JsonProperty("command") Optional<CommandInfo> command,
                                    @JsonProperty("container") Optional<ContainerInfo> container,
                                    @JsonProperty("discovery") Optional<DiscoveryInfo> discovery,
                                    @JsonProperty("healthCheck") Optional<HealthCheck> healthCheck,
                                    @JsonProperty("killPolicy") Optional<KillPolicy> killPolicy,
                                    @JsonProperty("name") String name) {
    this.taskId = taskId;
    this.executor = executor;
    this.labels = labels;
    this.agentId = agentId != null ? agentId : slaveId;
    this.slaveId = agentId != null ? agentId : slaveId;
    this.resources = resources != null ? resources : Collections.emptyList();
    this.command = command;
    this.container = container;
    this.discovery = discovery;
    this.healthCheck = healthCheck;
    this.killPolicy = killPolicy;
    this.name = name;
  }

  public TaskID getTaskId() {
    return taskId;
  }

  public ExecutorInfo getExecutor() {
    return executor.orNull();
  }

  public boolean hasExecutor() {
    return executor.isPresent();
  }

  public Labels getLabels() {
    return labels.orNull();
  }

  public boolean hasLabels() {
    return labels.isPresent();
  }

  public AgentID getAgentId() {
    return agentId;
  }

  public AgentID getSlaveId() {
    return slaveId;
  }

  public List<Resource> getResources() {
    return resources;
  }

  public CommandInfo getCommand() {
    return command.orNull();
  }

  public boolean hasCommand() {
    return command.isPresent();
  }

  public ContainerInfo getContainer() {
    return container.orNull();
  }

  public boolean hasContainer() {
    return container.isPresent();
  }

  public DiscoveryInfo getDiscovery() {
    return discovery.orNull();
  }

  public boolean hasDiscovery() {
    return discovery.isPresent();
  }

  public HealthCheck getHealthCheck() {
    return healthCheck.orNull();
  }

  public boolean hasHealthCheck() {
    return healthCheck.isPresent();
  }

  public KillPolicy getKillPolicy() {
    return killPolicy.orNull();
  }

  public boolean hasKillPolicy() {
    return killPolicy.isPresent();
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SingularityMesosTaskObject that = (SingularityMesosTaskObject) o;

    if (taskId != null ? !taskId.equals(that.taskId) : that.taskId != null) {
      return false;
    }
    if (executor != null ? !executor.equals(that.executor) : that.executor != null) {
      return false;
    }
    if (labels != null ? !labels.equals(that.labels) : that.labels != null) {
      return false;
    }
    if (agentId != null ? !agentId.equals(that.agentId) : that.agentId != null) {
      return false;
    }
    if (resources != null ? !resources.equals(that.resources) : that.resources != null) {
      return false;
    }
    if (command != null ? !command.equals(that.command) : that.command != null) {
      return false;
    }
    if (container != null ? !container.equals(that.container) : that.container != null) {
      return false;
    }
    if (discovery != null ? !discovery.equals(that.discovery) : that.discovery != null) {
      return false;
    }
    if (healthCheck != null ? !healthCheck.equals(that.healthCheck) : that.healthCheck != null) {
      return false;
    }
    if (killPolicy != null ? !killPolicy.equals(that.killPolicy) : that.killPolicy != null) {
      return false;
    }
    return name != null ? name.equals(that.name) : that.name == null;
  }

  @Override
  public int hashCode() {
    int result = taskId != null ? taskId.hashCode() : 0;
    result = 31 * result + (executor != null ? executor.hashCode() : 0);
    result = 31 * result + (labels != null ? labels.hashCode() : 0);
    result = 31 * result + (agentId != null ? agentId.hashCode() : 0);
    result = 31 * result + (resources != null ? resources.hashCode() : 0);
    result = 31 * result + (command != null ? command.hashCode() : 0);
    result = 31 * result + (container != null ? container.hashCode() : 0);
    result = 31 * result + (discovery != null ? discovery.hashCode() : 0);
    result = 31 * result + (healthCheck != null ? healthCheck.hashCode() : 0);
    result = 31 * result + (killPolicy != null ? killPolicy.hashCode() : 0);
    result = 31 * result + (name != null ? name.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "SingularityMesosTaskObject{" +
        "taskId=" + taskId +
        ", executor=" + executor +
        ", labels=" + labels +
        ", agentId=" + agentId +
        ", resources=" + resources +
        ", command=" + command +
        ", container=" + container +
        ", discovery=" + discovery +
        ", healthCheck=" + healthCheck +
        ", killPolicy=" + killPolicy +
        ", name='" + name + '\'' +
        '}';
  }
}
