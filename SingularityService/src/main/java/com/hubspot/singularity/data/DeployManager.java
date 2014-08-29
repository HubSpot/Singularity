package com.hubspot.singularity.data;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityDeployResult;
import com.hubspot.singularity.SingularityDeployStatistics;
import com.hubspot.singularity.SingularityDeployWebhook;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityDeployWebhook.DeployEventType;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.SingularityDeployKeyTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityDeployMarkerTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityDeployStateTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityDeployStatisticsTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityDeployTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityPendingDeployTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityRequestDeployStateTranscoder;

public class DeployManager extends CuratorAsyncManager {

  private final static Logger LOG = LoggerFactory.getLogger(DeployManager.class);

  private final WebhookManager webhookManager;
  private final SingularityDeployTranscoder deployTranscoder;
  private final SingularityPendingDeployTranscoder pendingDeployTranscoder;
  private final SingularityDeployMarkerTranscoder deployMarkerTranscoder;
  private final SingularityRequestDeployStateTranscoder requestDeployStateTranscoder;
  private final SingularityDeployStatisticsTranscoder deployStatisticsTranscoder;
  private final SingularityDeployStateTranscoder deployStateTranscoder;
  private final SingularityDeployKeyTranscoder deployKeyTranscoder;

  private final static String DEPLOY_ROOT = "/deploys";

  private final static String PENDING_ROOT = DEPLOY_ROOT + "/pending";
  private final static String CANCEL_ROOT = DEPLOY_ROOT + "/cancel";

  private final static String BY_REQUEST_ROOT = DEPLOY_ROOT + "/requests";

  private final static String REQUEST_DEPLOY_STATE_KEY = "STATE";
  private final static String DEPLOY_LIST_KEY = "/ids";

  private final static String DEPLOY_DATA_KEY = "DEPLOY";
  private final static String DEPLOY_MARKER_KEY = "MARKER";
  private final static String DEPLOY_STATISTICS_KEY = "STATISTICS";
  private final static String DEPLOY_RESULT_KEY = "RESULT_STATE";

  @Inject
  public DeployManager(SingularityConfiguration configuration, CuratorFramework curator, WebhookManager webhookManager, SingularityDeployTranscoder deployTranscoder, SingularityRequestDeployStateTranscoder requestDeployStateTranscoder,
      SingularityPendingDeployTranscoder pendingDeployTranscoder, SingularityDeployMarkerTranscoder deployMarkerTranscoder, SingularityDeployStatisticsTranscoder deployStatisticsTranscoder, SingularityDeployStateTranscoder deployStateTranscoder,
      SingularityDeployKeyTranscoder deployKeyTranscoder) {
    super(curator, configuration.getZookeeperAsyncTimeout());

    this.webhookManager = webhookManager;
    this.pendingDeployTranscoder = pendingDeployTranscoder;
    this.deployTranscoder = deployTranscoder;
    this.deployStatisticsTranscoder = deployStatisticsTranscoder;
    this.deployMarkerTranscoder = deployMarkerTranscoder;
    this.requestDeployStateTranscoder = requestDeployStateTranscoder;
    this.deployStateTranscoder = deployStateTranscoder;
    this.deployKeyTranscoder = deployKeyTranscoder;
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

    return getChildrenAsIdsForParents(BY_REQUEST_ROOT, paths, deployKeyTranscoder);
  }

  public Map<String, SingularityRequestDeployState> getRequestDeployStatesByRequestIds(Collection<String> requestIds) {
    final List<String> paths = Lists.newArrayListWithCapacity(requestIds.size());

    for (String requestId : requestIds) {
      paths.add(getRequestDeployStatePath(requestId));
    }

    return Maps.uniqueIndex(getAsync("request_deploy_states", paths, requestDeployStateTranscoder), new Function<SingularityRequestDeployState, String>() {

      @Override
      public String apply(SingularityRequestDeployState input) {
        return input.getRequestId();
      }

    });
  }

  public Map<String, SingularityRequestDeployState> getAllRequestDeployStatesByRequestId() {
    final List<String> requestIds = getChildren(BY_REQUEST_ROOT);

    return getRequestDeployStatesByRequestIds(requestIds);
  }

  public List<SingularityDeployMarker> getCancelDeploys() {
    return getAsyncChildren(CANCEL_ROOT, deployMarkerTranscoder);
  }

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

    final List<SingularityDeploy> deploys = getAsync("deploys-by-key", paths, deployTranscoder);

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
      LOG.info(String.format("Deploy object for %s already existed (new marker: %s)", deploy, deployMarker));
    }

    webhookManager.enqueueDeployUpdate(new SingularityDeployWebhook(deployMarker, Optional.of(deploy), DeployEventType.STARTING, Optional.<SingularityDeployResult> absent()));
    
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
      return Optional.of(new SingularityDeployHistory(deployState, deployMarker.get(), Optional.<SingularityDeploy> absent(), Optional.<SingularityDeployStatistics >absent()));
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

    return getData(deployPath, deployTranscoder);
  }

  public Optional<String> getInUseDeployId(String requestId) {
    Optional<SingularityRequestDeployState> deployState = getRequestDeployState(requestId);

    if (!deployState.isPresent() || (!deployState.get().getActiveDeploy().isPresent() && !deployState.get().getPendingDeploy().isPresent())) {
      return Optional.absent();
    }

    return Optional.of(deployState.get().getActiveDeploy().or(deployState.get().getPendingDeploy()).get().getDeployId());
  }

  public Optional<SingularityRequestDeployState> getRequestDeployState(String requestId) {
    return getData(getRequestDeployStatePath(requestId), requestDeployStateTranscoder);
  }

  public SingularityCreateResult saveNewRequestDeployState(SingularityRequestDeployState newDeployState) {
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
    webhookManager.enqueueDeployUpdate(new SingularityDeployWebhook(deployMarker, deploy, DeployEventType.FINISHED, Optional.of(result)));
    
    return save(getDeployResultPath(deployMarker.getRequestId(), deployMarker.getDeployId()), result, deployStateTranscoder);
  }

  public Optional<SingularityDeployResult> getDeployResult(String requestId, String deployId) {
    return getData(getDeployResultPath(requestId, deployId), deployStateTranscoder);
  }

}
