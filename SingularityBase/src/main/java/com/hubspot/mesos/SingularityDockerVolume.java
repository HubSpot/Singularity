package com.hubspot.mesos;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.wordnik.swagger.annotations.ApiModelProperty;

public class SingularityDockerVolume {
    private final Optional<String> driver, name;
    private final Map<String, Object> driverOptions;

    @JsonCreator
    public SingularityDockerVolume(
            @JsonProperty("driver") Optional<String> driver,
            @JsonProperty("name") Optional<String> name,
            @JsonProperty("driver_options") Map<String, Object> driverOptions)
    {
        this.driver = driver;
        this.name = name;
        this.driverOptions = driverOptions;
    }

    @ApiModelProperty(required=false, value="Docker volume driver name")
    public Optional<String> getDriver() {
        return driver;
    }

    @ApiModelProperty(required=false, value="Volume name")
    public Optional<String> getName() {
        return name;
    }

    @JsonProperty("driver_options")
    @ApiModelProperty(required=false, value="Volume driver options")
    public Map<String, Object> getDriverOptions() {
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
          ", driver_options=" + driverOptions +
          '}';
    }
}
