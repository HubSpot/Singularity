package com.hubspot.singularity.data.zkmigrations;


import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityTestBaseNoDb;
import com.hubspot.singularity.data.MetadataManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.transcoders.StringTranscoder;
import com.hubspot.singularity.data.zkmigrations.SingularityCmdLineArgsMigration.SingularityPendingRequestPrevious;

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

  private String getPendingPath(String requestId, String deployId) {
    return ZKPaths.makePath(SingularityCmdLineArgsMigration.REQUEST_PENDING_PATH, new SingularityDeployKey(requestId, deployId).getId());
  }

  private String getPendingPath(SingularityPendingTaskId pendingTaskId) {
    return ZKPaths.makePath(SingularityCmdLineArgsMigration.TASK_PENDING_PATH, pendingTaskId.getId());
  }

  @Test
  public void testCmdLineArgsMigration() throws Exception {
    metadataManager.setZkDataVersion("2");

    // save some old stuff
    SingularityPendingRequestPrevious p1 = new SingularityPendingRequestPrevious("r1", "d1", 23L, Optional.<String> absent(), PendingType.BOUNCE, Optional.<String> absent());
    SingularityPendingRequestPrevious p2 = new SingularityPendingRequestPrevious("r2", "d3", 123L, Optional.of("user1"), PendingType.BOUNCE, Optional.of("cmd line args"));

    byte[] p1b = objectMapper.writeValueAsBytes(p1);
    byte[] p2b = objectMapper.writeValueAsBytes(p2);

    curator.create().creatingParentsIfNeeded().forPath(getPendingPath("r1",  "d1"), p1b);
    curator.create().creatingParentsIfNeeded().forPath(getPendingPath("r2",  "de"), p2b);

    SingularityPendingTaskId pt1 = new SingularityPendingTaskId("r1", "d1", 23L, 3, PendingType.BOUNCE, 1L);
    SingularityPendingTaskId pt2 = new SingularityPendingTaskId("r2", "d3", 231L, 1, PendingType.UNPAUSED, 23L);

    curator.create().creatingParentsIfNeeded().forPath(getPendingPath(pt1));
    curator.create().creatingParentsIfNeeded().forPath(getPendingPath(pt2), StringTranscoder.INSTANCE.toBytes("cmd line args"));

    migrationRunner.checkMigrations();

    Assert.assertTrue(!taskManager.getPendingTask(pt1).get().getCmdLineArgsList().isPresent());
    Assert.assertTrue(taskManager.getPendingTask(pt2).get().getCmdLineArgsList().get().get(0).equals("cmd line args"));
    Assert.assertTrue(taskManager.getPendingTask(pt2).get().getCmdLineArgsList().get().size() == 1);

    Assert.assertTrue(taskManager.getPendingTaskIds().contains(pt1));
    Assert.assertTrue(taskManager.getPendingTaskIds().contains(pt2));

    Assert.assertTrue(requestManager.getPendingRequests().size() == 2);

    for (SingularityPendingRequest r : requestManager.getPendingRequests()) {
      if (r.getRequestId().equals("r1")) {
        Assert.assertEquals(r.getDeployId(), p1.getDeployId());
        Assert.assertEquals(r.getTimestamp(), p1.getTimestamp());
        Assert.assertEquals(r.getPendingType(), p1.getPendingType());
        Assert.assertTrue(!r.getCmdLineArgsList().isPresent());
        Assert.assertEquals(r.getUser(), p1.getUser());
      } else {
        Assert.assertEquals(r.getDeployId(), p2.getDeployId());
        Assert.assertEquals(r.getTimestamp(), p2.getTimestamp());
        Assert.assertEquals(r.getPendingType(), p2.getPendingType());
        Assert.assertTrue(r.getCmdLineArgsList().get().size() == 1);
        Assert.assertTrue(r.getCmdLineArgsList().get().get(0).equals("cmd line args"));
        Assert.assertEquals(r.getUser(), p2.getUser());
      }
    }
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
    Assert.assertEquals(RequestType.ON_DEMAND, requestManager.getRequest(oldOnDemandRequest.getId()).get().getRequest().getRequestType());
    Assert.assertEquals(RequestType.WORKER, requestManager.getRequest(oldWorkerRequest.getId()).get().getRequest().getRequestType());
    Assert.assertEquals(RequestType.SCHEDULED, requestManager.getRequest(oldScheduledRequest.getId()).get().getRequest().getRequestType());
    Assert.assertEquals(RequestType.SERVICE, requestManager.getRequest(oldServiceRequest.getId()).get().getRequest().getRequestType());

    // assert that the migration properly carried over any additional fields on the request
    Assert.assertEquals(Optional.of(owners), requestManager.getRequest(oldOnDemandRequest.getId()).get().getRequest().getOwners());
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


}
