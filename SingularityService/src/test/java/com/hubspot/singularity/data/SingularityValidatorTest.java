package com.hubspot.singularity.data;

import static org.mockito.Mockito.*;

import javax.ws.rs.WebApplicationException;

import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;

@RunWith(JukitoRunner.class)
public class SingularityValidatorTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  public static class TestModule extends JukitoModule {
    @Override
    protected void configureTest() {
      forceMock(MesosConfiguration.class);
      forceMock(SingularityConfiguration.class);
    }
  }

  @Before
  public void configureMocks(MesosConfiguration mesosConfiguration,
                             SingularityConfiguration singularityConfiguration) {
    // Needed to get the constructor injection working
    when(singularityConfiguration.getMesosConfiguration()).thenReturn(mesosConfiguration);
    when(singularityConfiguration.getHealthcheckMaxRetries()).thenReturn(Optional.<Integer>absent());
    when(singularityConfiguration.getMaxDeployIdSize()).thenReturn(15);
  }

  @Test
  public void itForbidsBracketCharactersInDeployIds(SingularityRequest singularityRequest,
                                                    SingularityDeploy singularityDeploy,
                                                    SingularityValidator singularityValidator) throws Exception {
    when(singularityDeploy.getId()).thenReturn("[[");

    expectedException.expect(WebApplicationException.class);
    singularityValidator.checkDeploy(singularityRequest, singularityDeploy);
  }

  @Test
  public void itForbidsQuotesInDeployIds(SingularityRequest singularityRequest,
                                         SingularityDeploy singularityDeploy,
                                         SingularityValidator singularityValidator) throws Exception {
    when(singularityDeploy.getId()).thenReturn("deployKey'");

    expectedException.expect(WebApplicationException.class);
    singularityValidator.checkDeploy(singularityRequest, singularityDeploy);
  }
}
