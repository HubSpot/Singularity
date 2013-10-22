package com.hubspot.singularity;

import org.apache.curator.framework.CuratorFramework;

import com.codahale.dropwizard.lifecycle.Managed;
import com.google.common.io.Closeables;
import com.google.inject.Inject;
import com.hubspot.singularity.mesos.SingularityDriver;

public class SingularityManaged implements Managed {

  private final SingularityDriver driver;
  private final CuratorFramework curator;
  
  @Inject
  public SingularityManaged(SingularityDriver driver, CuratorFramework curator) {
    this.driver = driver;
    this.curator = curator;
  }
  
  @Override
  public void start() throws Exception {
    driver.start();
  }
  
  @Override
  public void stop() throws Exception {
    driver.stop();
    Closeables.close(curator, true);
  }

}
