package com.hubspot.mesos;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Beta
public class SingularityDockerVolume {
    private final Optional<String> driver, name;
    private final Map<String, String> driverOptions;

    @JsonCreator
    public SingularityDockerVolume(
            @JsonProperty("driver") Optional<String> driver,
            @JsonProperty("name") Optional<String> name,
            @JsonProperty("driverOptions") Map<String, String> driverOptions)
    {
        this.driver = driver;
        this.name = name;
        this.driverOptions = MoreObjects.firstNonNull(driverOptions, Collections.emptyMap());
    }

    @ApiModelProperty(required=false, value="Docker volume driver name")
    public Optional<String> getDriver() {
        return driver;
    }

    @ApiModelProperty(required=false, value="Volume name '%i' will be replaced with the instance index")
    public Optional<String> getName() {
        return name;
    }

    @ApiModelProperty(required=false, value="Volume driver options")
    public Map<String, String> getDriverOptions() {
        return driverOptions;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      SingularityDockerVolume that = (SingularityDockerVolume) o;
      return Objects.equals(driver, that.driver) &&
          Objects.equals(name, that.name) &&
          Objects.equals(driverOptions, that.driverOptions);
    }

    @Override
    public int hashCode() {
      return Objects.hash(driver, name, driverOptions);
    }

    @Override
    public String toString() {
      return "SingularityDockerVolume{" +
          "driver='" + driver + '\'' +
          ", name=" + name +
          ", driverOptions=" + driverOptions +
          '}';
    }
}
