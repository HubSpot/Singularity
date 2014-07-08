package com.hubspot.singularity;

import com.google.common.base.Optional;


public abstract class SingularityMachineAbstraction extends SingularityJsonObject {
  
  private final String id;
  private final long firstSeenAt;
  
  private Optional<String> decomissioningBy;
  private Optional<Long> decomissioningAt;
  private Optional<Long> decomissionedAt;
  private Optional<Long> deadAt;
  
  private SingularityMachineState state;
  
  public enum SingularityMachineState {
    ACTIVE, DECOMISSIONING, DECOMISSIONED, DEAD, 
  }
  
  public SingularityMachineAbstraction(String id) {
    this(id, SingularityMachineState.ACTIVE, System.currentTimeMillis(), Optional.<String> absent(), Optional.<Long> absent(), Optional.<Long> absent(), Optional.<Long> absent());
  }
  
  public SingularityMachineAbstraction(String id, SingularityMachineState state, long firstSeenAt, Optional<String> decomissioningBy, Optional<Long> decomissioningAt, Optional<Long> decomissionedAt, Optional<Long> deadAt) {
    this.id = id;
    this.state = state;
    this.firstSeenAt = firstSeenAt;
    this.deadAt = deadAt;
    this.decomissioningBy = decomissioningBy;
    this.decomissioningAt = decomissioningAt;
    this.decomissionedAt = decomissionedAt;
  }
  
  public Optional<String> getDecomissioningBy() {
    return decomissioningBy;
  }

  public void setDecomissioningBy(Optional<String> decomissioningBy) {
    this.decomissioningBy = decomissioningBy;
  }

  public Optional<Long> getDecomissioningAt() {
    return decomissioningAt;
  }

  public void setDecomissioningAt(Optional<Long> decomissioningAt) {
    this.decomissioningAt = decomissioningAt;
  }

  public String getId() {
    return id;
  }
  
  public Optional<Long> getDeadAt() {
    return deadAt;
  }

  public void setDeadAt(Optional<Long> deadAt) {
    this.deadAt = deadAt;
  }

  public Optional<Long> getDecomissionedAt() {
    return decomissionedAt;
  }

  public void setDecomissionedAt(Optional<Long> decomissionedAt) {
    this.decomissionedAt = decomissionedAt;
  }

  public long getFirstSeenAt() {
    return firstSeenAt;
  }

  public SingularityMachineState getState() {
    return state;
  }
  
  public void setState(SingularityMachineState state) {
    this.state = state;
  }
  
  
}
