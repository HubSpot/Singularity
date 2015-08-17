package com.hubspot.singularity;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Before;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.horizon.HttpClient;
import com.hubspot.mesos.client.MesosClient;
import com.hubspot.singularity.scheduler.SingularityTestModule;

public class SingularityCuratorTestBase {

  @Inject
  protected CuratorFramework cf;
  @Inject
  protected TestingServer ts;
  @Inject
  @Named(MesosClient.HTTP_CLIENT_NAME)
  private HttpClient httpClient;

  private SingularityTestModule singularityTestModule;

  private final boolean useDBTests;

  @Before
  public final void curatorSetup() throws Exception {
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

    if (httpClient != null) {
      httpClient.close();
    }
  }


}
