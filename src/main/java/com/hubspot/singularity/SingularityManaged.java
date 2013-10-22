package com.hubspot.singularity;

import com.codahale.dropwizard.lifecycle.Managed;
import com.google.inject.Inject;
import com.hubspot.singularity.mesos.SingularityDriver;

public class SingularityManaged implements Managed {

  private final SingularityDriver driver;
  
  @Inject
  public SingularityManaged(SingularityDriver driver) {
    this.driver = driver;
  }
  
  @Override
  public void start() throws Exception {
    driver.start();
  }
  
  @Override
  public void stop() throws Exception {
  
  }

}
