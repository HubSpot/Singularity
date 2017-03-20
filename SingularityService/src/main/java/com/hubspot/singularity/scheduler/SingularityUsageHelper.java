package com.hubspot.singularity.scheduler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.data.SlaveManager;

@Singleton
public class SingularityUsageHelper {

  private final SlaveManager slaveManager;

  @Inject
  public SingularityUsageHelper(SlaveManager slaveManager) {
    this.slaveManager = slaveManager;
  }

  public Set<String> getSlaveIdsToTrackUsageFor() {
    List<SingularitySlave> slaves = getSlavesToTrackUsageFor();
    Set<String> slaveIds = new HashSet<>(slaves.size());
    for (SingularitySlave slave : slaves) {
      slaveIds.add(slave.getId());
    }
    return slaveIds;
  }

  public List<SingularitySlave> getSlavesToTrackUsageFor() {
    List<SingularitySlave> slaves = slaveManager.getObjects();
    List<SingularitySlave> slavesToTrack = new ArrayList<>(slaves.size());

    for (SingularitySlave slave : slaves) {
      if (slave.getCurrentState().getState().isInactive() || slave.getCurrentState().getState() == MachineState.DECOMMISSIONED) {
        continue;
      }

      slavesToTrack.add(slave);
    }

    return slavesToTrack;
  }

}
