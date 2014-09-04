package com.hubspot.singularity;

import java.util.Objects;

public abstract class SingularityId {

  private String id;

  public String getId() {
    if (this.id == null) {
      this.id = toString();
    }
    return this.id;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getId());
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
    return Objects.equals(getId(), other.getId());
  }

}
