package com.hubspot.mesos;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.hubspot.immutables.style.SingularityStyle;

@Immutable
@SingularityStyle
@JsonDeserialize(as = Resources.class)
public abstract class AbstractResources {
  @Default
  public double getCpus() {
    return 0;
  }

  @Default
  public double getMemoryMb() {
    return 0;
  }

  @Default
  public int getNumPorts() {
    return 0;
  }

  @Default
  public double getDiskMb() {
    return 0;
  }

  public static Resources add(Resources a, Resources b) {
    Preconditions.checkNotNull(a, "first argument of Resources.add() is null");
    Preconditions.checkNotNull(b, "second argument of Resources.add() is null");

    return Resources.builder()
        .setCpus(a.getCpus() + b.getCpus())
        .setMemoryMb(a.getMemoryMb() + b.getMemoryMb())
        .setNumPorts(a.getNumPorts() + b.getNumPorts())
        .setDiskMb(a.getDiskMb() + b.getDiskMb())
        .build();
  }

  public static Resources of(int cpus, int memoryMb, int numPorts) {
    return Resources.builder()
        .setCpus(cpus)
        .setMemoryMb(memoryMb)
        .setNumPorts(numPorts)
        .build();
  }
}
