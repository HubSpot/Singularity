package com.hubspot.singularity;

import static org.junit.Assert.assertEquals;

import java.util.Optional;

import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.hubspot.singularity.api.common.SingularityState;
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
    final SingularityState state = singularityClient.getState(Optional.empty(), Optional.empty());

    assertEquals(3, state.getActiveSlaves());
  }
}
