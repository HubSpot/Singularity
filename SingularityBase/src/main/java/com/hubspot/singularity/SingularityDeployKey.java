package com.hubspot.singularity;

import java.util.Collection;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.hubspot.mesos.JavaUtils;

public class SingularityDeployKey extends SingularityId {

  private final String requestId;
  private final String deployId;

  public static SingularityDeployKey fromDeploy(SingularityDeploy deploy) {
    return new SingularityDeployKey(deploy.getRequestId(), deploy.getId());
  }

  public static SingularityDeployKey fromPendingTask(SingularityPendingTask pendingTask) {
    return new SingularityDeployKey(pendingTask.getPendingTaskId().getRequestId(), pendingTask.getPendingTaskId().getDeployId());
  }

  public static SingularityDeployKey fromDeployMarker(SingularityDeployMarker deployMarker) {
    return new SingularityDeployKey(deployMarker.getRequestId(), deployMarker.getDeployId());
  }

  public static Map<SingularityDeployKey, SingularityDeploy> fromDeploys(Collection<SingularityDeploy> deploys) {
    return Maps.uniqueIndex(deploys, new Function<SingularityDeploy, SingularityDeployKey>() {
      @Override
      public SingularityDeployKey apply(SingularityDeploy input) {
        return SingularityDeployKey.fromDeploy(input);
      }
    });
  }

  public static Map<SingularityPendingTask, SingularityDeployKey> fromPendingTasks(Collection<SingularityPendingTask> pendingTasks) {
    return Maps.toMap(pendingTasks, new Function<SingularityPendingTask, SingularityDeployKey>() {
      @Override
      public SingularityDeployKey apply(SingularityPendingTask input) {
        return SingularityDeployKey.fromPendingTask(input);
      }
    });
  }

  public static Map<SingularityPendingDeploy, SingularityDeployKey> fromPendingDeploys(Collection<SingularityPendingDeploy> pendingDeploys) {
    return Maps.toMap(pendingDeploys, fromPendingDeployToDeployKey);
  }

  public static Function<SingularityPendingDeploy, SingularityDeployKey> fromPendingDeployToDeployKey = new Function<SingularityPendingDeploy, SingularityDeployKey>() {
    @Override
    public SingularityDeployKey apply(SingularityPendingDeploy input) {
      return SingularityDeployKey.fromDeployMarker(input.getDeployMarker());
    }
  };

  public SingularityDeployKey(String requestId, String deployId) {
    this.requestId = requestId;
    this.deployId = deployId;
  }

  public String getRequestId() {
    return requestId;
  }

  public String getDeployId() {
    return deployId;
  }

  public static SingularityDeployKey fromString(String string) {
    final String[] splits = JavaUtils.reverseSplit(string, 2, "-");

    final String requestId = splits[0];
    final String deployId = splits[1];

    return new SingularityDeployKey(requestId, deployId);
  }

  @Override
  public String toString() {
    return String.format("%s-%s", getRequestId(), getDeployId());
  }

}
