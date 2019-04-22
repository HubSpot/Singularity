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
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.Transcoder;

@Singleton
public class RequestGroupManager extends CuratorAsyncManager {
  private static final String REQUEST_GROUP_ROOT = "/groups";

  private final Transcoder<SingularityRequestGroup> requestGroupTranscoder;
  private final SingularityWebCache webCache;

  @Inject
  public RequestGroupManager(CuratorFramework curator, SingularityConfiguration configuration,
                             MetricRegistry metricRegistry, Transcoder<SingularityRequestGroup> requestGroupTranscoder, SingularityWebCache webCache) {
    super(curator, configuration, metricRegistry);
    this.requestGroupTranscoder = requestGroupTranscoder;
    this.webCache = webCache;
  }

  private String getRequestGroupPath(String requestGroupId) {
    return ZKPaths.makePath(REQUEST_GROUP_ROOT, requestGroupId);
  }

  public List<SingularityRequestGroup> getRequestGroups(boolean useWebCache) {
    if (useWebCache && webCache.useCachedRequestGroups()) {
      return webCache.getRequestGroups();
    }
    List<SingularityRequestGroup> requestGroups = getAsyncChildren(REQUEST_GROUP_ROOT, requestGroupTranscoder);
    if (useWebCache) {
      webCache.cacheRequestGroups(requestGroups);
    }
    return requestGroups;
  }

  public Optional<SingularityRequestGroup> getRequestGroup(String requestGroupId) {
    return getData(getRequestGroupPath(requestGroupId), requestGroupTranscoder);
  }

  public SingularityCreateResult saveRequestGroup(SingularityRequestGroup requestGroup) {
    return save(getRequestGroupPath(requestGroup.getId()), requestGroup, requestGroupTranscoder);
  }

  public SingularityDeleteResult deleteRequestGroup(String requestGroupId) {
    return delete(getRequestGroupPath(requestGroupId));
  }

  public void removeFromAllGroups(String requestId) {
    getRequestGroups(false).stream()
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
