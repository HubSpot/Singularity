package com.hubspot.singularity.api.common;

public abstract class SingularityId {
  @Override
  public int hashCode() {
    return getId().hashCode();
  }

  public abstract String getId();

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
    return getId().equals(other.getId());
  }


}
