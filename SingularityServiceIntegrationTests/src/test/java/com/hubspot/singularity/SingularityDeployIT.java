package com.hubspot.singularity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.hubspot.singularity.api.deploy.DeployState;
import com.hubspot.singularity.api.deploy.SingularityDeploy;
import com.hubspot.singularity.api.deploy.SingularityDeployBuilder;
import com.hubspot.singularity.api.deploy.SingularityDeployHistory;
import com.hubspot.singularity.api.request.RequestType;
import com.hubspot.singularity.api.request.SingularityRequest;
import com.hubspot.singularity.api.request.SingularityRequestBuilder;
import com.hubspot.singularity.api.request.SingularityRequestParent;
import com.hubspot.singularity.client.SingularityClient;

@RunWith(JukitoRunner.class)
public class SingularityDeployIT {
  private static final String REQUEST_ID = "deploy-it-request-run-once";

  public static class Module extends JukitoModule {
    @Override
    protected void configureTest() {
      install(new DockerTestModule());
    }
  }

  @Test
  public void testDeploy(SingularityClient singularityClient) throws Exception {
    final SingularityRequest request = new SingularityRequestBuilder(REQUEST_ID, RequestType.RUN_ONCE)
        .setInstances(Optional.of(2))
        .build();

    final String deployId = Long.toString(System.currentTimeMillis());

    singularityClient.createOrUpdateSingularityRequest(request);

    final Optional<SingularityRequestParent> requestParent = singularityClient.getSingularityRequest(REQUEST_ID);
    assertTrue(requestParent.isPresent());
    assertEquals(request, requestParent.get().getRequest());

    final SingularityDeploy deploy = new SingularityDeployBuilder(REQUEST_ID, deployId)
        .setCommand(Optional.of("sleep 10"))
        .build();

    singularityClient.createDeployForSingularityRequest(REQUEST_ID, deploy, Optional.empty(), Optional.empty());

    Optional<DeployState> deployState = Optional.empty();
    for (int i = 0; i < 10; i++) {
      final Optional<SingularityDeployHistory> deployHistory = singularityClient.getHistoryForRequestDeploy(REQUEST_ID, deployId);
      if (deployHistory.isPresent() && deployHistory.get().getDeployResult().isPresent()) {
        deployState = Optional.ofNullable(deployHistory.get().getDeployResult().get().getDeployState());

        if (deployState.get().isDeployFinished()) {
          break;
        }
      }

      Thread.sleep(2000);  // ghetto, i know.
    }

    assertEquals(Optional.of(DeployState.SUCCEEDED), deployState);
  }
}
