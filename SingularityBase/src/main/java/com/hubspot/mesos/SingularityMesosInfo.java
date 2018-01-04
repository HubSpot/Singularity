package com.hubspot.mesos;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Beta
public class SingularityMesosInfo {
  private final Optional<SingularityMesosImage> image;

  @JsonCreator
  public SingularityMesosInfo(@JsonProperty("image") Optional<SingularityMesosImage> image) {
    this.image = image;
  }

  @ApiModelProperty(required=false, value="Mesos image descriptor")
  public Optional<SingularityMesosImage> getImage() {
    return image;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityMesosInfo that = (SingularityMesosInfo) o;
    return Objects.equals(image, that.image);
  }

  @Override
  public int hashCode() {
    return Objects.hash(image);
  }

  @Override
  public String toString() {
    return "SingularityMesosInfo{" +
        "image='" + image + '\'' +
        '}';
  }
}
