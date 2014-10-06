package com.hubspot.singularity;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Before;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.hubspot.singularity.scheduler.SingularityTestModule;

public class SingularityCuratorTestBase {

  @Inject
  protected CuratorFramework cf;
  @Inject
  protected TestingServer ts;

  @Before
  public void setup() {
    Injector i = Guice.createInjector(new SingularityTestModule());

    i.injectMembers(this);
  }

  @After
  public void teardown() throws Exception {
    cf.close();
    ts.close();
  }

}
