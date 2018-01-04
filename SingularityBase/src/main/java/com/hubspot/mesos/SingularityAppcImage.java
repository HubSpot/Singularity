package com.hubspot.mesos;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Beta
public class SingularityAppcImage {
  private final String name;
  private final Optional<String> id;

  @JsonCreator
  public SingularityAppcImage(@JsonProperty("name") String name, @JsonProperty("id") Optional<String> id)
  {
    this.name = name;
    this.id = id;
  }

  @ApiModelProperty(required=true, value="Appc image name")
  public String getName()
  {
    return name;
  }

  @ApiModelProperty(required=false, value="")
  public Optional<String> getId()
  {
    return id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityAppcImage that = (SingularityAppcImage) o;
    return Objects.equals(name, that.name) &&
        Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, id);
  }

  @Override
  public String toString() {
    return "SingularityAppcImage{" +
        "name='" + name + '\'' +
        "id='" + id + '\'' +
        '}';
  }
}
