package com.hubspot.singularity;


import com.codahale.dropwizard.Configuration;

public class SingularityConfiguration extends Configuration {
  private String master;

  public void setMaster(String master) {
    this.master = master;
  }

  public String getMaster() {
    return master;
  }
}
