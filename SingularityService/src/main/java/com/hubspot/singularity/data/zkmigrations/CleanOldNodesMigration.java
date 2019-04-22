package com.hubspot.singularity.data.zkmigrations;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.curator.framework.CuratorFramework;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityRequestGroup;
import com.hubspot.singularity.data.RequestGroupManager;
import com.hubspot.singularity.data.RequestManager;

public class CleanOldNodesMigration extends ZkDataMigration {
  private final CuratorFramework curatorFramework;
  private final RequestGroupManager requestGroupManager;
  private final RequestManager requestManager;

  @Inject
  public CleanOldNodesMigration(CuratorFramework curatorFramework, RequestGroupManager requestGroupManager, RequestManager requestManager) {
    super(12);
    this.curatorFramework = curatorFramework;
    this.requestGroupManager = requestGroupManager;
    this.requestManager = requestManager;
  }

  @Override
  public void applyMigration() {
    List<String> toClean = ImmutableList.of("/disasters/previous-stats", "/disasters/stats", "/disasters/task-credits", "/offer-state");
    try {
      for (String node : toClean) {
        if (curatorFramework.checkExists().forPath(node) != null) {
          curatorFramework.delete().deletingChildrenIfNeeded().forPath(node);
        }
      }
      List<String> allIds = requestManager.getAllRequestIds();
      for (SingularityRequestGroup requestGroup : requestGroupManager.getRequestGroups(false)) {
        List<String> ids = requestGroup.getRequestIds()
            .stream()
            .filter((id) -> allIds.contains(id))
            .collect(Collectors.toList());
        if (ids.isEmpty()) {
          requestGroupManager.deleteRequestGroup(requestGroup.getId());
        } else {
          if (!ids.equals(requestGroup.getRequestIds())) {
            requestGroupManager.saveRequestGroup(new SingularityRequestGroup(
                requestGroup.getId(),
                ids,
                requestGroup.getMetadata()
            ));
          }
        }
      }
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }
}
