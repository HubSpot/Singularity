package com.hubspot.singularity.data;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityDeployResult;
import com.hubspot.singularity.SingularityDeployStatistics;
import com.hubspot.singularity.SingularityDeployUpdate;
import com.hubspot.singularity.SingularityDeployUpdate.DeployEventType;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityUpdatePendingDeployRequest;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.IdTranscoder;
import com.hubspot.singularity.data.transcoders.Transcoder;
import com.hubspot.singularity.event.SingularityEventListener;
import com.hubspot.singularity.scheduler.SingularityLeaderCache;

@Singleton
public class DeployManager extends CuratorAsyncManager {

  private static final Logger LOG = LoggerFactory.getLogger(DeployManager.class);

  private final SingularityEventListener singularityEventListener;
  private final Transcoder<SingularityDeploy> deployTranscoder;
  private final Transcoder<SingularityPendingDeploy> pendingDeployTranscoder;
  private final Transcoder<SingularityDeployMarker> deployMarkerTranscoder;
  private final Transcoder<SingularityRequestDeployState> requestDeployStateTranscoder;
  private final Transcoder<SingularityDeployStatistics> deployStatisticsTranscoder;
  private final Transcoder<SingularityDeployResult> deployStateTranscoder;
  private final Transcoder<SingularityUpdatePendingDeployRequest> updateRequestTranscoder;

  private final IdTranscoder<SingularityDeployKey> deployKeyTranscoder;

  private final ZkCache<SingularityDeploy> deploysCache;
  private final SingularityLeaderCache leaderCache;

  private static final String DEPLOY_ROOT = "/deploys";

  private static final String PENDING_ROOT = DEPLOY_ROOT + "/pending";
  private static final String CANCEL_ROOT = DEPLOY_ROOT + "/cancel";
  private static final String UPDATE_ROOT = DEPLOY_ROOT + "/update";

  private static final String BY_REQUEST_ROOT = DEPLOY_ROOT + "/requests";

  private static final String REQUEST_DEPLOY_STATE_KEY = "STATE";
  private static final String DEPLOY_LIST_KEY = "/ids";

  private static final String DEPLOY_DATA_KEY = "DEPLOY";
  private static final String DEPLOY_MARKER_KEY = "MARKER";
  private static final String DEPLOY_STATISTICS_KEY = "STATISTICS";
  private static final String DEPLOY_RESULT_KEY = "RESULT_STATE";

  @Inject
  public DeployManager(CuratorFramework curator, SingularityConfiguration configuration, MetricRegistry metricRegistry, SingularityEventListener singularityEventListener, Transcoder<SingularityDeploy> deployTranscoder,
                       Transcoder<SingularityRequestDeployState> requestDeployStateTranscoder, Transcoder<SingularityPendingDeploy> pendingDeployTranscoder, Transcoder<SingularityDeployMarker> deployMarkerTranscoder,
                       Transcoder<SingularityDeployStatistics> deployStatisticsTranscoder, Transcoder<SingularityDeployResult> deployStateTranscoder, IdTranscoder<SingularityDeployKey> deployKeyTranscoder,
                       Transcoder<SingularityUpdatePendingDeployRequest> updateRequestTranscoder, ZkCache<SingularityDeploy> deploysCache, SingularityLeaderCache leaderCache) {
    super(curator, configuration, metricRegistry);

    this.singularityEventListener = singularityEventListener;
    this.pendingDeployTranscoder = pendingDeployTranscoder;
    this.deployTranscoder = deployTranscoder;
    this.deployStatisticsTranscoder = deployStatisticsTranscoder;
    this.deployMarkerTranscoder = deployMarkerTranscoder;
    this.requestDeployStateTranscoder = requestDeployStateTranscoder;
    this.deployStateTranscoder = deployStateTranscoder;
    this.deployKeyTranscoder = deployKeyTranscoder;
    this.updateRequestTranscoder = updateRequestTranscoder;
    this.deploysCache = deploysCache;
    this.leaderCache = leaderCache;
  }

  public List<SingularityDeployKey> getDeployIdsFor(String requestId) {
    return getChildrenAsIds(getDeployIdPath(requestId), deployKeyTranscoder);
  }

  public List<SingularityDeployKey> getAllDeployIds() {
    final List<String> requestIds = getChildren(BY_REQUEST_ROOT);
    final List<String> paths = Lists.newArrayListWithCapacity(requestIds.size());

    for (String requestId : requestIds) {
      paths.add(getDeployIdPath(requestId));
    }

    return getChildrenAsIdsForParents("getAllDeployIds", paths, deployKeyTranscoder);
  }

  @Timed
  public Map<String, SingularityRequestDeployState> getRequestDeployStatesByRequestIds(Collection<String> requestIds) {
    if (leaderCache.active()) {
      return leaderCache.getRequestDeployStateByRequestId(requestIds);
    }

    return fetchDeployStatesByRequestIds(requestIds);
  }

  public Map<String, SingularityRequestDeployState> fetchDeployStatesByRequestIds(Collection<String> requestIds) {
    final List<String> paths = Lists.newArrayListWithCapacity(requestIds.size());

    for (String requestId : requestIds) {
      paths.add(getRequestDeployStatePath(requestId));
    }

    return Maps.uniqueIndex(getAsync("getRequestDeployStatesByRequestIds", paths, requestDeployStateTranscoder), new Function<SingularityRequestDeployState, String>() {

      @Override
      public String apply(SingularityRequestDeployState input) {
        return input.getRequestId();
      }

    });
  }

  @Timed
  public Map<String, SingularityRequestDeployState> getAllRequestDeployStatesByRequestId() {
    if (leaderCache.active()) {
      return leaderCache.getRequestDeployStateByRequestId();
    }
    final List<String> requestIds = getChildren(BY_REQUEST_ROOT);
    return fetchDeployStatesByRequestIds(requestIds);
  }

  public List<SingularityDeployMarker> getCancelDeploys() {
    return getAsyncChildren(CANCEL_ROOT, deployMarkerTranscoder);
  }

  @Timed
  public List<SingularityPendingDeploy> getPendingDeploys() {
    return getAsyncChildren(PENDING_ROOT, pendingDeployTranscoder);
  }

  private String getRequestDeployPath(String requestId) {
    return ZKPaths.makePath(BY_REQUEST_ROOT, requestId);
  }

  private String getDeployStatisticsPath(String requestId, String deployId) {
    return ZKPaths.makePath(getDeployParentPath(requestId, deployId), DEPLOY_STATISTICS_KEY);
  }

  private String getDeployResultPath(String requestId, String deployId) {
    return ZKPaths.makePath(getDeployParentPath(requestId, deployId), DEPLOY_RESULT_KEY);
  }

  private String getDeployIdPath(String requestId) {
    return ZKPaths.makePath(getRequestDeployPath(requestId), DEPLOY_LIST_KEY);
  }

  private String getDeployParentPath(String requestId, String deployId) {
    return ZKPaths.makePath(getDeployIdPath(requestId), new SingularityDeployKey(requestId, deployId).getId());
  }

  private String getDeployDataPath(String requestId, String deployId) {
    return ZKPaths.makePath(getDeployParentPath(requestId, deployId), DEPLOY_DATA_KEY);
  }

  private String getDeployMarkerPath(String requestId, String deployId) {
    return ZKPaths.makePath(getDeployParentPath(requestId, deployId), DEPLOY_MARKER_KEY);
  }

  private String getRequestDeployStatePath(String requestId) {
    return ZKPaths.makePath(getRequestDeployPath(requestId), REQUEST_DEPLOY_STATE_KEY);
  }

  public Map<SingularityDeployKey, SingularityDeploy> getDeploysForKeys(Collection<SingularityDeployKey> deployKeys) {
    final List<String> paths = Lists.newArrayListWithCapacity(deployKeys.size());

    for (SingularityDeployKey deployKey : deployKeys) {
      paths.add(getDeployDataPath(deployKey.getRequestId(), deployKey.getDeployId()));
    }

    final List<SingularityDeploy> deploys = getAsync("getDeploysForKeys", paths, deployTranscoder, deploysCache);

    final Map<SingularityDeployKey, SingularityDeploy> deployKeyToDeploy = Maps.uniqueIndex(deploys, new Function<SingularityDeploy, SingularityDeployKey>() {
      @Override
      public SingularityDeployKey apply(SingularityDeploy input) {
        return SingularityDeployKey.fromDeploy(input);
      }
    });

    return deployKeyToDeploy;
  }

  public SingularityCreateResult saveDeploy(SingularityRequest request, SingularityDeployMarker deployMarker, SingularityDeploy deploy) {
    final SingularityCreateResult deploySaveResult = create(getDeployDataPath(deploy.getRequestId(), deploy.getId()), deploy, deployTranscoder);

    if (deploySaveResult == SingularityCreateResult.EXISTED) {
      LOG.info("Deploy object for {} already existed (new marker: {})", deploy, deployMarker);
    }

    singularityEventListener.deployHistoryEvent(new SingularityDeployUpdate(deployMarker, Optional.of(deploy), DeployEventType.STARTING, Optional.<SingularityDeployResult>absent()));

    create(getDeployMarkerPath(deploy.getRequestId(), deploy.getId()), deployMarker, deployMarkerTranscoder);

    final Optional<SingularityRequestDeployState> currentState = getRequestDeployState(deploy.getRequestId());

    Optional<SingularityDeployMarker> activeDeploy = Optional.absent();
    Optional<SingularityDeployMarker> pendingDeploy = Optional.absent();

    if (request.isDeployable()) {
      if (currentState.isPresent()) {
        activeDeploy = currentState.get().getActiveDeploy();
      }
      pendingDeploy = Optional.of(deployMarker);
    } else {
      activeDeploy = Optional.of(deployMarker);
    }

    final SingularityRequestDeployState newState = new SingularityRequestDeployState(deploy.getRequestId(), activeDeploy, pendingDeploy);

    return saveNewRequestDeployState(newState);
  }

  public Optional<SingularityDeployHistory> getDeployHistory(String requestId, String deployId, boolean loadEntireHistory) {
    Optional<SingularityDeployMarker> deployMarker = getData(getDeployMarkerPath(requestId, deployId), deployMarkerTranscoder);

    if (!deployMarker.isPresent()) {
      return Optional.absent();
    }

    Optional<SingularityDeployResult> deployState = getDeployResult(requestId, deployId);

    if (!loadEntireHistory) {
      return Optional.of(new SingularityDeployHistory(deployState, deployMarker.get(), Optional.<SingularityDeploy> absent(), Optional.<SingularityDeployStatistics>absent()));
    }

    Optional<SingularityDeploy> deploy = getDeploy(requestId, deployId);

    if (!deploy.isPresent()) {
      return Optional.absent();
    }

    Optional<SingularityDeployStatistics> deployStatistics = getDeployStatistics(requestId, deployId);

    return Optional.of(new SingularityDeployHistory(deployState, deployMarker.get(), deploy, deployStatistics));
  }

  public Optional<SingularityDeploy> getDeploy(String requestId, String deployId) {
    final String deployPath = getDeployDataPath(requestId, deployId);

    return getData(deployPath, deployTranscoder, deploysCache, true);
  }

  public Optional<String> getInUseDeployId(String requestId) {
    Optional<SingularityRequestDeployState> deployState = getRequestDeployState(requestId);

    if (!deployState.isPresent() || !deployState.get().getActiveDeploy().isPresent() && !deployState.get().getPendingDeploy().isPresent()) {
      return Optional.absent();
    }

    return Optional.of(deployState.get().getActiveDeploy().or(deployState.get().getPendingDeploy()).get().getDeployId());
  }

  public Optional<SingularityRequestDeployState> getRequestDeployState(String requestId) {
    if (leaderCache.active()) {
      return leaderCache.getRequestDeployState(requestId);
    }
    return getData(getRequestDeployStatePath(requestId), requestDeployStateTranscoder);
  }

  public SingularityCreateResult saveNewRequestDeployState(SingularityRequestDeployState newDeployState) {
    if (leaderCache.active()) {
      leaderCache.putRequestDeployState(newDeployState);
    }

    return save(getRequestDeployStatePath(newDeployState.getRequestId()), newDeployState, requestDeployStateTranscoder);
  }

  public Optional<SingularityDeployStatistics> getDeployStatistics(String requestId, String deployId) {
    return getData(getDeployStatisticsPath(requestId, deployId), deployStatisticsTranscoder);
  }

  public SingularityCreateResult saveDeployStatistics(SingularityDeployStatistics newDeployStatistics) {
    return save(getDeployStatisticsPath(newDeployStatistics.getRequestId(), newDeployStatistics.getDeployId()), newDeployStatistics, deployStatisticsTranscoder);
  }

  private String getPendingDeployPath(String requestId) {
    return ZKPaths.makePath(PENDING_ROOT, requestId);
  }

  private String getCancelDeployPath(SingularityDeployMarker deployMarker) {
    return ZKPaths.makePath(CANCEL_ROOT, String.format("%s-%s", deployMarker.getRequestId(), deployMarker.getDeployId()));
  }

  public SingularityCreateResult createCancelDeployRequest(SingularityDeployMarker deployMarker) {
    return create(getCancelDeployPath(deployMarker), deployMarker, deployMarkerTranscoder);
  }

  public SingularityDeleteResult deleteRequestDeployState(String requestId) {
    if (leaderCache.active()) {
      leaderCache.deleteRequestDeployState(requestId);
    }
    return delete(getRequestDeployStatePath(requestId));
  }

  public SingularityDeleteResult deleteDeployHistory(SingularityDeployKey deployKey) {
    return delete(getDeployParentPath(deployKey.getRequestId(), deployKey.getDeployId()));
  }

  public SingularityDeleteResult deletePendingDeploy(String requestId) {
    return delete(getPendingDeployPath(requestId));
  }

  public SingularityDeleteResult deleteCancelDeployRequest(SingularityDeployMarker deployMarker) {
    return delete(getCancelDeployPath(deployMarker));
  }

  public SingularityCreateResult createPendingDeploy(SingularityPendingDeploy pendingDeploy) {
    return create(getPendingDeployPath(pendingDeploy.getDeployMarker().getRequestId()), pendingDeploy, pendingDeployTranscoder);
  }

  public SingularityCreateResult savePendingDeploy(SingularityPendingDeploy pendingDeploy) {
    return save(getPendingDeployPath(pendingDeploy.getDeployMarker().getRequestId()), pendingDeploy, pendingDeployTranscoder);
  }

  public Optional<SingularityPendingDeploy> getPendingDeploy(String requestId) {
    return getData(getPendingDeployPath(requestId), pendingDeployTranscoder);
  }

  public SingularityCreateResult saveDeployResult(SingularityDeployMarker deployMarker, Optional<SingularityDeploy> deploy, SingularityDeployResult result) {
    singularityEventListener.deployHistoryEvent(new SingularityDeployUpdate(deployMarker, deploy, DeployEventType.FINISHED, Optional.of(result)));

    return save(getDeployResultPath(deployMarker.getRequestId(), deployMarker.getDeployId()), result, deployStateTranscoder);
  }

  public Optional<SingularityDeployResult> getDeployResult(String requestId, String deployId) {
    return getData(getDeployResultPath(requestId, deployId), deployStateTranscoder);
  }

  private String getUpdatePendingDeployPath(SingularityUpdatePendingDeployRequest updateRequest) {
    return ZKPaths.makePath(UPDATE_ROOT, String.format("%s-%s", updateRequest.getRequestId(), updateRequest.getDeployId()));
  }

  public SingularityCreateResult createUpdatePendingDeployRequest(SingularityUpdatePendingDeployRequest updateRequest) {
    return create(getUpdatePendingDeployPath(updateRequest), updateRequest, updateRequestTranscoder);
  }

  public SingularityDeleteResult deleteUpdatePendingDeployRequest(SingularityUpdatePendingDeployRequest updateRequest) {
    return delete(getUpdatePendingDeployPath(updateRequest));
  }

  public List<SingularityUpdatePendingDeployRequest> getPendingDeployUpdates() {
    return getAsyncChildren(UPDATE_ROOT, updateRequestTranscoder);
  }

  public void purgeStaleRequests(List<String> activeRequestIds, long deleteBeforeTime) {
    final List<String> requestIds = getChildren(BY_REQUEST_ROOT);
    for (String requestId : requestIds) {
      if (!activeRequestIds.contains(requestId)) {
        String path = getRequestDeployPath(requestId);
        Optional<Stat> maybeStat = checkExists(path);
        if (maybeStat.isPresent() && maybeStat.get().getMtime() < deleteBeforeTime && !getChildren(path).contains(REQUEST_DEPLOY_STATE_KEY)) {
          delete(path);
        }
      }
    }
  }

  public SingularityDeleteResult deleteRequestId(String requestId) {
    return delete(getRequestDeployPath(requestId));
  }

  public void activateLeaderCache() {
    final List<String> requestIds = getChildren(BY_REQUEST_ROOT);
    leaderCache.cacheRequestDeployStates(fetchDeployStatesByRequestIds(requestIds));
  }
}
