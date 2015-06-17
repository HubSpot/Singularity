package com.hubspot.singularity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
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

    singularityClient.createOrUpdateSingularityRequest(request, Optional.<String>absent());

    final Optional<SingularityRequestParent> requestParent = singularityClient.getSingularityRequest(REQUEST_ID);
    assertTrue(requestParent.isPresent());
    assertEquals(request, requestParent.get().getRequest());

    final SingularityDeploy deploy = new SingularityDeployBuilder(REQUEST_ID, deployId)
            .setCommand(Optional.of("(echo hello && sleep 10) > test.txt"))
            .build();

    singularityClient.createDeployForSingularityRequest(REQUEST_ID, deploy, Optional.<Boolean>absent(), Optional.<String>absent());

    Optional<DeployState> deployState = Optional.absent();
    for (int i = 0; i < 10; i++) {
      final Optional<SingularityDeployHistory> deployHistory = singularityClient.getHistoryForRequestDeploy(REQUEST_ID, deployId);
      if (deployHistory.isPresent() && deployHistory.get().getDeployResult().isPresent()) {
        deployState = Optional.fromNullable(deployHistory.get().getDeployResult().get().getDeployState());

        if (deployState.get().isDeployFinished()) {
          break;
        }
      }

      Thread.sleep(2000);  // ghetto, i know.
    }

    assertEquals(Optional.of(DeployState.SUCCEEDED), deployState);

    final List<SingularityTaskIdHistory> tasks = ImmutableList.copyOf(Iterables.concat(singularityClient.getActiveTaskHistoryForRequest(REQUEST_ID), singularityClient.getInactiveTaskHistoryForRequest(REQUEST_ID)));

    Optional<SingularityTaskId> taskId = Optional.absent();

    for (SingularityTaskIdHistory taskIdHistory : tasks) {
      if (taskIdHistory.getTaskId().getRequestId().equals(REQUEST_ID) && taskIdHistory.getTaskId().getDeployId().equals(deployId)) {
        taskId = Optional.of(taskIdHistory.getTaskId());
      }
    }

    assertTrue(taskId.isPresent());

    System.out.println(singularityClient.readSandBoxFile(taskId.get().getId(), "test.txt", Optional.<String>absent(), Optional.of(0L), Optional.of(100L)));
  }
}
