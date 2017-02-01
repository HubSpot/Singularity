package com.hubspot.singularity;

public abstract class SingularityId {

  private final String id;

  public SingularityId(String id) {
    this.id = id;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  public String getId() {
    return id;
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
    SingularityId other = (SingularityId) obj;
    return id.equals(other.id);
  }


}
