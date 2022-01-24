package com.hubspot.singularity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.hubspot.singularity.client.SingularityClient;
import java.util.Optional;
import name.falgout.jeffrey.testing.junit.guice.GuiceExtension;
import name.falgout.jeffrey.testing.junit.guice.IncludeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GuiceExtension.class)
@IncludeModule(DockerTestModule.class)
public class SingularityStateIT {

  @Test
  public void testStateEndpoint(SingularityClient singularityClient) {
    final SingularityState state = singularityClient.getState(
      Optional.<Boolean>empty(),
      Optional.<Boolean>empty()
    );

    assertEquals(3, state.getActiveAgents());
  }
}
