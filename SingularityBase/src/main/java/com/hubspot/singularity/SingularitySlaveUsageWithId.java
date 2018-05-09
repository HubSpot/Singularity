package com.hubspot.singularity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A description of resources used on a mesos slave")
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

  @Schema(description = "The id as assigned by mesos for this particualr slave")
  public String getSlaveId() {
    return slaveId;
  }

  @Override
  public String toString() {
    return "SingularitySlaveUsageWithId [slaveId=" + slaveId + ", super=" + super.toString() + "]";
  }



}
