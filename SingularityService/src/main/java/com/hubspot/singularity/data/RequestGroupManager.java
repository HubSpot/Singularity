package com.hubspot.singularity.data;

import java.util.ArrayList;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityRequestGroup;
import com.hubspot.singularity.cache.SingularityCache;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.Transcoder;

@Singleton
public class RequestGroupManager extends CuratorAsyncManager {
  private static final String REQUEST_GROUP_ROOT = "/groups";

  private final SingularityCache cache;
  private final Transcoder<SingularityRequestGroup> requestGroupTranscoder;

  @Inject
  public RequestGroupManager(CuratorFramework curator,
                             SingularityConfiguration configuration,
                             MetricRegistry metricRegistry,
                             SingularityCache cache,
                             Transcoder<SingularityRequestGroup> requestGroupTranscoder) {
    super(curator, configuration, metricRegistry);
    this.cache = cache;
    this.requestGroupTranscoder = requestGroupTranscoder;
  }

  private String getRequestGroupPath(String requestGroupId) {
    return ZKPaths.makePath(REQUEST_GROUP_ROOT, requestGroupId);
  }

  public void activateLeaderCache() {
    cache.cacheRequestGroups(getRequestGroups(true));
  }

  public List<SingularityRequestGroup> getRequestGroups(boolean skipCache) {
    if (!skipCache) {
      return cache.getRequestGroups();
    }
    return getAsyncChildren(REQUEST_GROUP_ROOT, requestGroupTranscoder);
  }

  public Optional<SingularityRequestGroup> getRequestGroup(String requestGroupId) {
    return cache.getRequestGroup(requestGroupId);
  }

  public SingularityCreateResult saveRequestGroup(SingularityRequestGroup requestGroup) {
    SingularityCreateResult result = save(getRequestGroupPath(requestGroup.getId()), requestGroup, requestGroupTranscoder);
    cache.putRequestGroup(requestGroup);
    return result;
  }

  public SingularityDeleteResult deleteRequestGroup(String requestGroupId) {
    SingularityDeleteResult result = delete(getRequestGroupPath(requestGroupId));
    cache.deleteRequestGroup(requestGroupId);
    return result;
  }

  public void removeFromAllGroups(String requestId) {
    getRequestGroups(true).stream()
        .filter((g) -> g.getRequestIds().contains(requestId))
        .forEach((g) -> {
          List<String> ids = new ArrayList<>();
          ids.addAll(g.getRequestIds());
          ids.remove(requestId);
          if (ids.isEmpty()) {
            deleteRequestGroup(g.getId());
          } else {
            saveRequestGroup(new SingularityRequestGroup(
                g.getId(),
                ids,
                g.getMetadata()
            ));
          }
        });
  }
}
