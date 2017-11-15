package com.hubspot.singularity.data.zkmigrations;

import java.util.Collections;
import java.util.List;

import javax.inject.Singleton;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingTaskBuilder;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.transcoders.StringTranscoder;
import com.hubspot.singularity.data.transcoders.Transcoder;

@Singleton
public class SingularityCmdLineArgsMigration extends ZkDataMigration {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityCmdLineArgsMigration.class);

  private final CuratorFramework curator;
  private final TaskManager taskManager;
  private final ObjectMapper objectMapper;
  private final Transcoder<SingularityPendingRequest> pendingRequestTranscoder;

  @Inject
  public SingularityCmdLineArgsMigration(CuratorFramework curator, TaskManager taskManager, ObjectMapper objectMapper, Transcoder<SingularityPendingRequest> pendingRequestTranscoder) {
    super(4);
    this.curator = curator;
    this.taskManager = taskManager;
    this.objectMapper = objectMapper;
    this.pendingRequestTranscoder = pendingRequestTranscoder;
  }

  static class SingularityPendingRequestPrevious {

    private final String requestId;
    private final String deployId;
    private final long timestamp;
    private final PendingType pendingType;
    private final Optional<String> user;
    private final Optional<String> cmdLineArgs;

    @JsonCreator
    public SingularityPendingRequestPrevious(@JsonProperty("requestId") String requestId, @JsonProperty("deployId") String deployId, @JsonProperty("timestamp") long timestamp,
        @JsonProperty("user") Optional<String> user, @JsonProperty("pendingType") PendingType pendingType, @JsonProperty("cmdLineArgs") Optional<String> cmdLineArgs) {
      this.requestId = requestId;
      this.deployId = deployId;
      this.timestamp = timestamp;
      this.user = user;
      this.cmdLineArgs = cmdLineArgs;
      this.pendingType = pendingType;
    }

    public String getRequestId() {
      return requestId;
    }

    public String getDeployId() {
      return deployId;
    }

    public long getTimestamp() {
      return timestamp;
    }

    public PendingType getPendingType() {
      return pendingType;
    }

    public Optional<String> getUser() {
      return user;
    }

    public Optional<String> getCmdLineArgs() {
      return cmdLineArgs;
    }



  }

  static final String TASK_PENDING_PATH = "/tasks/scheduled";
  static final String REQUEST_PENDING_PATH = "/requests/pending";

  @Override
  public void applyMigration() {
    checkPendingTasks();
    checkPendingRequests();
  }

  private void checkPendingRequests() {

    try {
      if (curator.checkExists().forPath(REQUEST_PENDING_PATH) == null) {
        return;
      }
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }

    try {
      for (String pendingRequest : curator.getChildren().forPath(REQUEST_PENDING_PATH)) {
        SingularityPendingRequestPrevious previous = objectMapper.readValue(curator.getData().forPath(ZKPaths.makePath(REQUEST_PENDING_PATH, pendingRequest)), SingularityPendingRequestPrevious.class);
        SingularityPendingRequest newRequest = new SingularityPendingRequest(previous.requestId, previous.deployId, previous.timestamp, previous.user, previous.pendingType,
            getCmdLineArgs(previous.cmdLineArgs), Optional.<String> absent(), Optional.<Boolean> absent(), Optional.<String> absent(), Optional.<String> absent());

        LOG.info("Re-saving {}", newRequest);

        curator.setData().forPath(ZKPaths.makePath(REQUEST_PENDING_PATH, pendingRequest), pendingRequestTranscoder.toBytes(newRequest));
      }
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  private void checkPendingTasks() {

    try {
      if (curator.checkExists().forPath(TASK_PENDING_PATH) == null) {
        return;
      }
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }

    try {

      for (SingularityPendingTaskId pendingTaskId : taskManager.getPendingTaskIds()) {
        Optional<String> cmdLineArgs = getCmdLineArgs(pendingTaskId);

        SingularityCreateResult result = taskManager.savePendingTask(
            new SingularityPendingTaskBuilder()
                .setPendingTaskId(pendingTaskId)
                .setCmdLineArgsList(getCmdLineArgs(cmdLineArgs))
                .build()
        );

        LOG.info("Saving {} ({}) {}", pendingTaskId, cmdLineArgs, result);
      }
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  private Optional<List<String>> getCmdLineArgs(Optional<String> cmdLineArgs) {
    return cmdLineArgs.isPresent() ? Optional.of(Collections.singletonList(cmdLineArgs.get())) : Optional.<List<String>> absent();
  }

  private Optional<String> getCmdLineArgs(SingularityPendingTaskId pendingTaskId) throws Exception {
    byte[] data = curator.getData().forPath(ZKPaths.makePath(TASK_PENDING_PATH, pendingTaskId.getId()));

    if (data != null && data.length > 0) {
      return Optional.of(StringTranscoder.INSTANCE.fromBytes(data));
    }

    return Optional.absent();
  }


}
