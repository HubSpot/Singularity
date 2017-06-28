package com.hubspot.singularity;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Before;

import com.google.inject.Inject;
import com.hubspot.singularity.scheduler.SingularityTestModule;
import com.squarespace.jersey2.guice.JerseyGuiceUtils;

public class SingularityCuratorTestBase {

  @Inject
  protected CuratorFramework cf;
  @Inject
  protected TestingServer ts;

  private SingularityTestModule singularityTestModule;

  private final boolean useDBTests;

  @Before
  public final void curatorSetup() throws Exception {
    JerseyGuiceUtils.reset();
    singularityTestModule = new SingularityTestModule(useDBTests);

    singularityTestModule.getInjector().injectMembers(this);
    singularityTestModule.start();
  }

  public SingularityCuratorTestBase(boolean useDBTests) {
    this.useDBTests = useDBTests;
  }

  @After
  public final void curatorTeardown() throws Exception {
    singularityTestModule.stop();

    if (cf != null) {
      cf.close();
    }

    if (ts != null) {
      ts.close();
    }

  }


}
