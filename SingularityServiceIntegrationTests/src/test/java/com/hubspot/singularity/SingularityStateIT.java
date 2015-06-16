package com.hubspot.singularity;

import static org.junit.Assert.assertEquals;

import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.base.Optional;
import com.hubspot.singularity.client.SingularityClient;

@RunWith(JukitoRunner.class)
public class SingularityStateIT {
  public static class Module extends JukitoModule {
    @Override
    protected void configureTest() {
      install(new DockerTestModule());
    }
  }

  @Test
  public void testStateEndpoint(SingularityClient singularityClient) {
    final SingularityState state = singularityClient.getState(Optional.<Boolean>absent(), Optional.<Boolean>absent());

    assertEquals(3, state.getActiveSlaves());
  }
}
