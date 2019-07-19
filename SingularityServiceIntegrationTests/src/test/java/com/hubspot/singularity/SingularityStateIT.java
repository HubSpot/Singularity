package com.hubspot.singularity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Optional;
import com.hubspot.singularity.client.SingularityClient;

import name.falgout.jeffrey.testing.junit.guice.GuiceExtension;
import name.falgout.jeffrey.testing.junit.guice.IncludeModule;

@ExtendWith(GuiceExtension.class)
@IncludeModule(DockerTestModule.class)
public class SingularityStateIT {

  @Test
  public void testStateEndpoint(SingularityClient singularityClient) {
    final SingularityState state = singularityClient.getState(Optional.<Boolean>empty(), Optional.<Boolean>empty());

    assertEquals(3, state.getActiveSlaves());
  }
}
