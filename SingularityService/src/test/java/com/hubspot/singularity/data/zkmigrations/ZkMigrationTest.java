package com.hubspot.singularity.data.zkmigrations;


import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskBuilder;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskStatusHolder;
import com.hubspot.singularity.SingularityTestBaseNoDb;
import com.hubspot.singularity.data.MetadataManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;

public class ZkMigrationTest extends SingularityTestBaseNoDb {

  @Inject
  private ZkDataMigrationRunner migrationRunner;
  @Inject
  private MetadataManager metadataManager;
  @Inject
  private TaskManager taskManager;
  @Inject
  private RequestManager requestManager;
  @Inject
  private ObjectMapper objectMapper;
  @Inject
  private CuratorFramework curator;
  @Inject
  private List<ZkDataMigration> migrations;

  @Test
  public void testMigrationRunner() {
    int largestSeen = 0;

    for (ZkDataMigration migration : migrations) {
      Assert.assertTrue(migration.getMigrationNumber() > largestSeen);

      largestSeen = migration.getMigrationNumber();
    }

    Assert.assertTrue(migrationRunner.checkMigrations() == migrations.size());

    Assert.assertTrue(metadataManager.getZkDataVersion().isPresent() && metadataManager.getZkDataVersion().get().equals(Integer.toString(largestSeen)));

    Assert.assertTrue(migrationRunner.checkMigrations() == 0);
  }

  @Test
  public void testNamespaceTasksMigration() throws Exception {
    metadataManager.setZkDataVersion("11");
    long now = System.currentTimeMillis();
    SingularityPendingTaskId testPending = new SingularityPendingTaskId("test", "deploy", now, 1, PendingType.IMMEDIATE, now);
    SingularityPendingTask pendingTask = new SingularityPendingTaskBuilder().setPendingTaskId(testPending).build();
    curator.create().creatingParentsIfNeeded().forPath("/tasks/scheduled/" + testPending.getId(), objectMapper.writeValueAsBytes(pendingTask));

    SingularityTaskId taskId = new SingularityTaskId("test", "deploy", now, 1, "host", "rack");
    curator.create().creatingParentsIfNeeded().forPath("/tasks/active/" + taskId.getId());
    SingularityTaskStatusHolder statusHolder = new SingularityTaskStatusHolder(taskId, Optional.absent(), now, "1234", Optional.absent());
    curator.create().creatingParentsIfNeeded().forPath("/tasks/statuses/" + taskId.getId(), objectMapper.writeValueAsBytes(statusHolder));

    migrationRunner.checkMigrations();

    List<SingularityPendingTaskId> pendingTaskIds = taskManager.getPendingTaskIds(true); // cache hasn't been loaded yet for this test
    Assert.assertTrue(pendingTaskIds.contains(testPending));
    Assert.assertEquals(pendingTask, taskManager.getPendingTask(testPending, true).get());

    List<SingularityTaskId> active = taskManager.getActiveTaskIds();
    Assert.assertTrue(active.contains(taskId));
  }

  @Test
  public void testSingularityRequestTypeMigration() throws Exception {
    metadataManager.setZkDataVersion("8");

    final List<String> owners = ImmutableList.of("foo1@bar.com", "foo2@bar.com");

    final SingularityRequestTypeMigration.OldSingularityRequest oldOnDemandRequest = new SingularityRequestTypeMigration.OldSingularityRequest("old-on-demand", null, Optional.<String>absent(), Optional.of(false), Optional.<Boolean>absent());
    final SingularityRequestTypeMigration.OldSingularityRequest oldWorkerRequest = new SingularityRequestTypeMigration.OldSingularityRequest("old-worker", null, Optional.<String>absent(), Optional.of(true), Optional.<Boolean>absent());
    final SingularityRequestTypeMigration.OldSingularityRequest oldScheduledRequest = new SingularityRequestTypeMigration.OldSingularityRequest("old-scheduled", null, Optional.of("0 0 0 0 0"), Optional.<Boolean>absent(), Optional.<Boolean>absent());
    final SingularityRequestTypeMigration.OldSingularityRequest oldServiceRequest = new SingularityRequestTypeMigration.OldSingularityRequest("old-service", null, Optional.<String>absent(), Optional.of(true), Optional.of(true));

    oldOnDemandRequest.setUnknownField("owners", owners);

    // save old-style requests to ZK
    curator.create().creatingParentsIfNeeded().forPath("/requests/all/" + oldOnDemandRequest.getId(), objectMapper.writeValueAsBytes(new SingularityRequestTypeMigration.OldSingularityRequestWithState(oldOnDemandRequest, RequestState.ACTIVE, System.currentTimeMillis())));
    curator.create().creatingParentsIfNeeded().forPath("/requests/all/" + oldWorkerRequest.getId(), objectMapper.writeValueAsBytes(new SingularityRequestTypeMigration.OldSingularityRequestWithState(oldWorkerRequest, RequestState.ACTIVE, System.currentTimeMillis())));
    curator.create().creatingParentsIfNeeded().forPath("/requests/all/" + oldScheduledRequest.getId(), objectMapper.writeValueAsBytes(new SingularityRequestTypeMigration.OldSingularityRequestWithState(oldScheduledRequest, RequestState.ACTIVE, System.currentTimeMillis())));
    curator.create().creatingParentsIfNeeded().forPath("/requests/all/" + oldServiceRequest.getId(), objectMapper.writeValueAsBytes(new SingularityRequestTypeMigration.OldSingularityRequestWithState(oldServiceRequest, RequestState.ACTIVE, System.currentTimeMillis())));

    // run ZK migration
    migrationRunner.checkMigrations();

    // assert that the migration properly set the requestType field
    // cache isn't loaded for this, so skip cache on these calls
    Assert.assertEquals(RequestType.ON_DEMAND, requestManager.getRequest(oldOnDemandRequest.getId(), true).get().getRequest().getRequestType());
    Assert.assertEquals(RequestType.WORKER, requestManager.getRequest(oldWorkerRequest.getId(), true).get().getRequest().getRequestType());
    Assert.assertEquals(RequestType.SCHEDULED, requestManager.getRequest(oldScheduledRequest.getId(), true).get().getRequest().getRequestType());
    Assert.assertEquals(RequestType.SERVICE, requestManager.getRequest(oldServiceRequest.getId(), true).get().getRequest().getRequestType());

    // assert that the migration properly carried over any additional fields on the request
    Assert.assertEquals(Optional.of(owners), requestManager.getRequest(oldOnDemandRequest.getId(), true).get().getRequest().getOwners());
  }

  @Test
  public void testPendingRequestRewriteTest() throws Exception {
    metadataManager.setZkDataVersion("9");
    long now = System.currentTimeMillis();

    SingularityPendingRequest immediateRequest = new SingularityPendingRequest("immediateRequest", "immediateDeploy", now, Optional.absent(), PendingType.IMMEDIATE, Optional.absent(), Optional.absent());
    SingularityPendingRequest newDeploy = new SingularityPendingRequest("newDeployRequest", "newDeploy", now, Optional.absent(), PendingType.NEW_DEPLOY, Optional.absent(), Optional.absent());
    SingularityPendingRequest oneOffRequest = new SingularityPendingRequest("oneOffRequest", "oneOffDeploy", now, Optional.absent(), PendingType.ONEOFF, Optional.absent(), Optional.absent());
    curator.create().creatingParentsIfNeeded().forPath("/requests/pending/immediateRequest-immediateDeploy", objectMapper.writeValueAsBytes(immediateRequest));
    curator.create().creatingParentsIfNeeded().forPath("/requests/pending/newDeployRequest-newDeploy", objectMapper.writeValueAsBytes(newDeploy));
    curator.create().creatingParentsIfNeeded().forPath(String.format("%s%s", "/requests/pending/oneOffRequest-oneOffDeploy", now), objectMapper.writeValueAsBytes(oneOffRequest));

    Assert.assertEquals("3 existing requests under old paths", 3, requestManager.getPendingRequests().size());
    System.out.println(curator.getChildren().forPath("/requests/pending"));

    migrationRunner.checkMigrations();

    Assert.assertEquals("3 existing requests under new paths", 3, requestManager.getPendingRequests().size());
    System.out.println(curator.getChildren().forPath("/requests/pending"));

    requestManager.deletePendingRequest(newDeploy);
    Assertions.assertThat(requestManager.getPendingRequests())
        .as("Non-renamed, non-timestamped nodes can be deleted")
        .hasSize(2)
        // Shim for the fact that SinguarityPendingRequest does not implement `equals`/`hashCode`
        // Can be removed when immutables PR is merged
        .extracting(SingularityPendingRequest::toString)
        .doesNotContain(newDeploy.toString())
        .contains(oneOffRequest.toString(), immediateRequest.toString());

    requestManager.deletePendingRequest(oneOffRequest);
    Assertions.assertThat(requestManager.getPendingRequests())
        .as("Non-renamed timestamped nodes can be deleted")
        .hasSize(1)
        .extracting(SingularityPendingRequest::toString)
        .doesNotContain(oneOffRequest.toString())
        .contains(immediateRequest.toString());

    requestManager.deletePendingRequest(immediateRequest);
    Assertions.assertThat(requestManager.getPendingRequests())
        .as("Renamed nodes can be deleted")
        .hasSize(0);
  }

  @Test
  public void testPendingRequestWithRunIdRewrite() throws Exception {
    metadataManager.setZkDataVersion("10");
    long now = System.currentTimeMillis();

    SingularityPendingRequest immediateRequest = new SingularityPendingRequest("immediateRequest", "immediateDeploy", now, Optional.absent(), PendingType.IMMEDIATE, Optional.absent(), Optional.of("run1"), Optional.absent(), Optional.absent(), Optional.absent());
    SingularityPendingRequest newDeploy = new SingularityPendingRequest("newDeployRequest", "newDeploy", now, Optional.absent(), PendingType.NEW_DEPLOY, Optional.absent(), Optional.absent());
    SingularityPendingRequest oneOffRequest = new SingularityPendingRequest("oneOffRequest", "oneOffDeploy", now, Optional.absent(), PendingType.ONEOFF, Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent());
    curator.create().creatingParentsIfNeeded().forPath(String.format("%s%s", "/requests/pending/immediateRequest-immediateDeploy", now), objectMapper.writeValueAsBytes(immediateRequest));
    curator.create().creatingParentsIfNeeded().forPath("/requests/pending/newDeployRequest-newDeploy", objectMapper.writeValueAsBytes(newDeploy));
    curator.create().creatingParentsIfNeeded().forPath(String.format("%s%s", "/requests/pending/oneOffRequest-oneOffDeploy", now), objectMapper.writeValueAsBytes(oneOffRequest));

    Assert.assertEquals("3 existing requests under old paths", 3, requestManager.getPendingRequests().size());
    System.out.println(curator.getChildren().forPath("/requests/pending"));

    migrationRunner.checkMigrations();

    System.out.println(curator.getChildren().forPath("/requests/pending"));
    Assert.assertEquals("3 existing requests under new paths", 3, requestManager.getPendingRequests().size());
    System.out.println(curator.getChildren().forPath("/requests/pending"));

    requestManager.deletePendingRequest(newDeploy);
    Assertions.assertThat(requestManager.getPendingRequests())
        .as("Non-renamed, non-timestamped nodes can be deleted")
        .hasSize(2)
        // Shim for the fact that SinguarityPendingRequest does not implement `equals`/`hashCode`
        // Can be removed when immutables PR is merged
        .extracting(SingularityPendingRequest::toString)
        .doesNotContain(newDeploy.toString())
        .contains(oneOffRequest.toString(), immediateRequest.toString());

    requestManager.deletePendingRequest(oneOffRequest);
    Assertions.assertThat(requestManager.getPendingRequests())
        .as("Non-renamed immediate (i.e., run-now scheduled without a runId) nodes can be deleted")
        .hasSize(1)
        .extracting(SingularityPendingRequest::toString)
        .doesNotContain(oneOffRequest.toString())
        .contains(immediateRequest.toString());

    requestManager.deletePendingRequest(immediateRequest);
    Assertions.assertThat(requestManager.getPendingRequests())
        .as("Renamed one-off (i.e., run-now on-demand) nodes can be deleted")
        .hasSize(0);
  }


}
