package com.hubspot.singularity.config;

import java.util.List;

import javax.validation.Valid;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;

public class ClusterCoordinatorConfiguration extends Configuration {
  /*
   * List of possible Singularity clusters. The first in the list will be considered the default
   */
  @NotEmpty
  private List<DataCenter> dataCenters;

  @JsonProperty("ui")
  @Valid
  private UIConfiguration uiConfiguration = new UIConfiguration();

  public List<DataCenter> getDataCenters() {
    return dataCenters;
  }

  public void setDataCenters(List<DataCenter> dataCenters) {
    this.dataCenters = dataCenters;
  }

  public UIConfiguration getUiConfiguration() {
    return uiConfiguration;
  }

  public void setUiConfiguration(UIConfiguration uiConfiguration) {
    this.uiConfiguration = uiConfiguration;
  }
}
