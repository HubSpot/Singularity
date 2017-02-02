package com.hubspot.singularity.athena;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AthenaField {
  private final String name;
  private final AthenaFieldType type; // TODO enum this
  private final String friendlyName;

  @JsonCreator
  public AthenaField(@JsonProperty("name") String name, @JsonProperty("type") AthenaFieldType type, @JsonProperty("friendlyName") String friendlyName) {
    this.name = name;
    this.type = type;
    this.friendlyName = friendlyName;
  }

  public String getName() {
    return name;
  }

  public AthenaFieldType getType() {
    return type;
  }

  public String getFriendlyName() {
    return friendlyName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    AthenaField field = (AthenaField) o;

    if (name != null ? !name.equals(field.name) : field.name != null) {
      return false;
    }
    if (type != null ? !type.equals(field.type) : field.type != null) {
      return false;
    }
    return friendlyName != null ? friendlyName.equals(field.friendlyName) : field.friendlyName == null;
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (type != null ? type.hashCode() : 0);
    result = 31 * result + (friendlyName != null ? friendlyName.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "AthenaField{" +
        "name='" + name + '\'' +
        ", type='" + type + '\'' +
        ", friendlyName='" + friendlyName + '\'' +
        '}';
  }
}
