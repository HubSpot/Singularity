package com.hubspot.mesos.protos;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class MesosDockerVolume {
  private final Optional<String> driver;
  private final Optional<String> name;
  private final Optional<List<MesosParameter>> driverOptions;

  @JsonCreator
  public MesosDockerVolume(@JsonProperty("driver") Optional<String> driver,
                           @JsonProperty("name") Optional<String> name,
                           @JsonProperty("driverOptions") Optional<List<MesosParameter>> driverOptions) {
    this.driver = driver;
    this.name = name;
    this.driverOptions = driverOptions;
  }

  public String getDriver() {
    return driver.orNull();
  }

  public boolean hasDriver() {
    return driver.isPresent();
  }

  public String getName() {
    return name.orNull();
  }

  public boolean hasName() {
    return name.isPresent();
  }

  public List<MesosParameter> getDriverOptions() {
    return driverOptions.orNull();
  }

  public boolean hasDriverOptions() {
    return driverOptions.isPresent();
  }
}
