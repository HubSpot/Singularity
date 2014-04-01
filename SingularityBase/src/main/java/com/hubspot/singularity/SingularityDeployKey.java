package com.hubspot.singularity;

import java.util.Collection;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

public class SingularityDeployKey {

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
    return Maps.toMap(pendingDeploys, new Function<SingularityPendingDeploy, SingularityDeployKey>() {
      @Override
      public SingularityDeployKey apply(SingularityPendingDeploy input) {
        return SingularityDeployKey.fromDeployMarker(input.getDeployMarker());
      }
    });
  }
 
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

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((deployId == null) ? 0 : deployId.hashCode());
    result = prime * result + ((requestId == null) ? 0 : requestId.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SingularityDeployKey other = (SingularityDeployKey) obj;
    if (deployId == null) {
      if (other.deployId != null)
        return false;
    } else if (!deployId.equals(other.deployId))
      return false;
    if (requestId == null) {
      if (other.requestId != null)
        return false;
    } else if (!requestId.equals(other.requestId))
      return false;
    return true;
  }

}
