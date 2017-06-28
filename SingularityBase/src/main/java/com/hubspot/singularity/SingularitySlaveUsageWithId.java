package com.hubspot.singularity;

import com.wordnik.swagger.annotations.ApiModel;

@ApiModel(description = "Singularity's view of a Mesos slave")
public class SingularitySlaveUsageWithId extends SingularitySlaveUsage {

  private final String slaveId;

  public SingularitySlaveUsageWithId(SingularitySlaveUsage usage, String slaveId) {
    super(usage.getMemoryBytesUsed(), usage.getMemoryMbReserved(), usage.getTimestamp(), usage.getCpusUsed(), usage.getCpusReserved(), usage.getNumTasks(), usage.getMemoryMbTotal(), usage.getCpusTotal(), usage.getLongRunningTasksUsage());
    this.slaveId = slaveId;
  }

  public String getSlaveId() {
    return slaveId;
  }

  @Override
  public String toString() {
    return "SingularitySlaveUsageWithId [slaveId=" + slaveId + ", super=" + super.toString() + "]";
  }



}
