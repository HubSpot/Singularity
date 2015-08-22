package com.hubspot.singularity;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

public class SingularityRuncConfig {
  private final List<SingularityRuncMount> mounts;
  private final boolean terminal;

  @JsonCreator
  public SingularityRuncConfig(@JsonProperty("mounts") List<SingularityRuncMount> mounts,
                               @JsonProperty("terminal") boolean terminal) {
    this.mounts = Objects.firstNonNull(mounts, Collections.<SingularityRuncMount>emptyList());
    this.terminal = Objects.firstNonNull(terminal, true);
  }

  public static SingularityRuncConfig defaultConfig() {
    return new SingularityRuncConfig(Collections.<SingularityRuncMount>emptyList(), true);
  }

  public List<SingularityRuncMount> getMounts() {
    return mounts;
  }

  public boolean isTerminal() {
    return terminal;
  }

  @Override
  public String toString() {
    return "SingularityRuncConfig{" +
      ", mounts=" + mounts +
      ", terminal=" + terminal +
      '}';
  }
}
