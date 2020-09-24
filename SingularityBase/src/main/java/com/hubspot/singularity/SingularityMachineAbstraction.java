package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import java.util.Optional;

@Schema(
  title = "Describes a slave or rack",
  subTypes = { SingularityAgent.class, SingularityRack.class }
)
public abstract class SingularityMachineAbstraction<T extends SingularityMachineAbstraction<T>> {
  private final String id;
  private final long firstSeenAt;
  private final SingularityMachineStateHistoryUpdate currentState;

  public SingularityMachineAbstraction(String id) {
    this(
      id,
      System.currentTimeMillis(),
      new SingularityMachineStateHistoryUpdate(
        id,
        MachineState.ACTIVE,
        System.currentTimeMillis(),
        Optional.<String>empty(),
        Optional.<String>empty()
      )
    );
  }

  public SingularityMachineAbstraction(
    String id,
    long firstSeenAt,
    SingularityMachineStateHistoryUpdate currentState
  ) {
    this.id = id;
    this.currentState = currentState;
    this.firstSeenAt = firstSeenAt;
  }

  @Schema(description = "The machine id")
  public String getId() {
    return id;
  }

  @JsonIgnore
  public abstract String getName();

  @JsonIgnore
  public abstract String getTypeName();

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    @SuppressWarnings("unchecked")
    SingularityMachineAbstraction<T> other = (SingularityMachineAbstraction<T>) obj;
    if (id == null) {
      if (other.id != null) {
        return false;
      }
    } else if (!id.equals(other.id)) {
      return false;
    }
    return true;
  }

  @Schema(description = "Timestamp of the first time this machine was seen")
  public long getFirstSeenAt() {
    return firstSeenAt;
  }

  @Schema(description = "The current state of this machine")
  public SingularityMachineStateHistoryUpdate getCurrentState() {
    return currentState;
  }

  public abstract T changeState(SingularityMachineStateHistoryUpdate newState);
}
