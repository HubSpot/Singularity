package com.hubspot.mesos;

import java.util.Objects;

import org.apache.mesos.Protos.Image;
import org.apache.mesos.Protos.Image.Type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class SingularityMesosInfo {
  private final String image;
  private final Image.Type type;

  @JsonCreator
  public SingularityMesosInfo(@JsonProperty("image") String image, // TODO should this be more like the Mesos image protobuf?
                              @JsonProperty("type") Image.Type type) {
    this.image = image;
    this.type = type;
  }

  @ApiModelProperty(required=true, value="Image name")
  public String getImage() {
    return image;
  }

  @ApiModelProperty(required=true, value="Image type")
  public Type getType()
  {
    return type;
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
    return Objects.equals(image, that.image) &&
        Objects.equals(type, that.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(image, type);
  }

  @Override
  public String toString() {
    return "SingularityMesosInfo{" +
        "image='" + image + '\'' +
        ", type=" + type +
        '}';
  }
}
