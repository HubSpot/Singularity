package com.hubspot.singularity.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties( ignoreUnknown = true )
public class NetworkConfiguration {
  private boolean defaultPortMapping;

  public void setDefaultPortMapping(boolean defaultPortMapping)
  {
    this.defaultPortMapping = defaultPortMapping;
  }

  public boolean isDefaultPortMapping()
  {
    return defaultPortMapping;
  }
}
