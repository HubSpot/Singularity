package com.hubspot.singularity;

import com.wordnik.swagger.annotations.ApiModel;

@ApiModel(description = "Singularity's view of a Mesos slave")
public class SingularitySlaveUsageWithId extends SingularitySlaveUsage {

  private final String slaveId;

  public SingularitySlaveUsageWithId(SingularitySlaveUsage usage, String slaveId) {
    super(
        usage.getCpusUsed(),
        usage.getCpusReserved(),
        usage.getCpusTotal(),
        usage.getMemoryBytesUsed(),
        usage.getMemoryMbReserved(),
        usage.getMemoryMbTotal(),
        usage.getDiskBytesUsed(),
        usage.getDiskMbReserved(),
        usage.getDiskMbTotal(),
        usage.getLongRunningTasksUsage(),
        usage.getNumTasks(),
        usage.getTimestamp(),
        usage.getSystemMemTotalBytes(),
        usage.getSystemMemFreeBytes(),
        usage.getSystemCpusTotal(),
        usage.getSystemLoad1Min(),
        usage.getSystemLoad5Min(),
        usage.getSystemLoad15Min(),
        usage.getSlaveDiskUsed(),
        usage.getSlaveDiskTotal()
    );
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
