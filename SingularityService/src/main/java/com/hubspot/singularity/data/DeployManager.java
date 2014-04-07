package com.hubspot.singularity.data;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.SetDataBuilder;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.KeeperException.BadVersionException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.hubspot.singularity.DeployState;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityDeployStatistics;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.DeployStateTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityDeployKeyTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityDeployMarkerTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityDeployStatisticsTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityDeployTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityPendingDeployTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityRequestDeployStateTranscoder;

public class DeployManager extends CuratorAsyncManager {
  
  private final static Logger LOG = LoggerFactory.getLogger(DeployManager.class);
  
  private final ObjectMapper objectMapper;
  
  private final SingularityDeployTranscoder deployTranscoder;
  private final SingularityPendingDeployTranscoder pendingDeployTranscoder;
  private final SingularityDeployMarkerTranscoder deployMarkerTranscoder;
  private final SingularityRequestDeployStateTranscoder requestDeployStateTranscoder;
  private final SingularityDeployStatisticsTranscoder deployStatisticsTranscoder;
  private final DeployStateTranscoder deployStateTranscoder;
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
  public DeployManager(SingularityConfiguration configuration, CuratorFramework curator, SingularityDeployTranscoder deployTranscoder, SingularityRequestDeployStateTranscoder requestDeployStateTranscoder, 
      SingularityPendingDeployTranscoder pendingDeployTranscoder, SingularityDeployMarkerTranscoder deployMarkerTranscoder, SingularityDeployStatisticsTranscoder deployStatisticsTranscoder, DeployStateTranscoder deployStateTranscoder, 
      SingularityDeployKeyTranscoder deployKeyTranscoder, ObjectMapper objectMapper) {
    super(curator, configuration.getZookeeperAsyncTimeout());
    
    this.objectMapper = objectMapper;
      
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
  
  public List<SingularityRequestDeployState> getAllRequestDeployStates() {
    final List<String> requestIds = getChildren(BY_REQUEST_ROOT);
    final List<String> paths = Lists.newArrayListWithCapacity(requestIds.size());
    
    for (String requestId : requestIds) {
      paths.add(getRequestDeployStatePath(requestId));
    }
    
    return getAsync("request_deploy_states", paths, requestDeployStateTranscoder);
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
  
  public ConditionalPersistResult persistDeploy(SingularityRequest request, SingularityDeployMarker deployMarker, SingularityDeploy deploy) {
    final SingularityCreateResult deploySaveResult = create(getDeployDataPath(deploy.getRequestId(), deploy.getId()), Optional.of(deployTranscoder.toBytes(deploy)));
    
    if (deploySaveResult == SingularityCreateResult.EXISTED) {
      LOG.info(String.format("Deploy object for %s already existed (new marker: %s)", deploy, deployMarker));
    }
    
    create(getDeployMarkerPath(deploy.getRequestId(), deploy.getId()), Optional.of(deployMarkerTranscoder.toBytes(deployMarker)));
    
    final Optional<SingularityRequestDeployStateHelper> currentState = getDeployStateHelper(deploy.getRequestId());
    
    final Optional<Stat> deployStateStat = currentState.isPresent() ? Optional.of(currentState.get().getStat()) : Optional.<Stat> absent();
    
    Optional<SingularityDeployMarker> activeDeploy = Optional.absent();
    Optional<SingularityDeployMarker> pendingDeploy = Optional.absent();
    
    if (request.isDeployable()) {
      if (currentState.isPresent()) {
        activeDeploy = currentState.get().getRequestDeployState().getActiveDeploy();
      }
      pendingDeploy = Optional.of(deployMarker);
    } else {
      activeDeploy = Optional.of(deployMarker);
    }
    
    final SingularityRequestDeployState newState = new SingularityRequestDeployState(deploy.getRequestId(), activeDeploy, pendingDeploy);
    
    return saveNewRequestDeployState(newState, deployStateStat, !deployStateStat.isPresent());
  }
  
  public Optional<SingularityDeployHistory> getDeployHistory(String requestId, String deployId, boolean loadEntireHistory) {
    Optional<SingularityDeployMarker> deployMarker = getData(getDeployMarkerPath(requestId, deployId), deployMarkerTranscoder);
    
    if (!deployMarker.isPresent()) {
      return Optional.absent();
    }
    
    if (!loadEntireHistory) {
      return Optional.of(new SingularityDeployHistory(Optional.<DeployState> absent(), deployMarker.get(), Optional.<SingularityDeploy> absent(), Optional.<SingularityDeployStatistics >absent()));
    }
    
    Optional<SingularityDeploy> deploy = getDeploy(requestId, deployId);
    
    if (!deploy.isPresent()) {
      return Optional.absent();
    }
    
    Optional<SingularityDeployStatistics> deployStatistics = getDeployStatistics(requestId, deployId);
    Optional<DeployState> deployState = getDeployState(requestId, deployId);
    
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
    Optional<SingularityRequestDeployStateHelper> maybeHelper = getDeployStateHelper(requestId);
    
    if (!maybeHelper.isPresent()) {
      return Optional.absent();
    }
  
    return Optional.of(maybeHelper.get().getRequestDeployState());
  }
  
  private Optional<SingularityRequestDeployStateHelper> getDeployStateHelper(String requestId) {
    final String statePath = getRequestDeployStatePath(requestId);
    
    Stat stat = new Stat();
    
    Optional<SingularityRequestDeployState> deployState = getData(statePath, Optional.of(stat), requestDeployStateTranscoder);
    
    if (!deployState.isPresent()) {
      return Optional.absent(); 
    }
    
    return Optional.of(new SingularityRequestDeployStateHelper(deployState.get(), stat));
  }
  
  public class SingularityRequestDeployStateHelper {
    
    private final SingularityRequestDeployState requestDeployState;
    private final Stat stat;
    
    public SingularityRequestDeployStateHelper(SingularityRequestDeployState requestDeployState, Stat stat) {
      this.requestDeployState = requestDeployState;
      this.stat = stat;
    }

    public SingularityRequestDeployState getRequestDeployState() {
      return requestDeployState;
    }

    public Stat getStat() {
      return stat;
    }
    
  }
  
  public enum ConditionalPersistResult {
    
    SAVED, STATE_CHANGED;
  
  }
  
  public ConditionalPersistResult saveNewRequestDeployState(SingularityRequestDeployState newDeployState, Optional<Stat> maybeStat, boolean createNew) {
    final String statePath = getRequestDeployStatePath(newDeployState.getRequestId());
    final byte[] data = newDeployState.getAsBytes(objectMapper);
    
    try {
      
      if (!createNew) {
        SetDataBuilder setDataBuilder = curator.setData();
        
        if (maybeStat.isPresent()) {
          setDataBuilder.withVersion(maybeStat.get().getVersion());
        }
        
        setDataBuilder.forPath(statePath, data);
      } else {
        curator.create().creatingParentsIfNeeded().forPath(statePath, data);
      }
      
      return ConditionalPersistResult.SAVED;
    
    } catch (NodeExistsException nee) {
      LOG.trace(String.format("Conflict - NodeExists while persisting new deploy state (%s)", newDeployState), nee);
    } catch (BadVersionException bve) {
      LOG.trace(String.format("Conflict - BadVersion while persisting new deploy state (%s)", newDeployState), bve);
    } catch (NoNodeException nne) {
      LOG.trace(String.format("Conflict - NoNode while persisting new deploy state (%s)", newDeployState), nne);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
    
    return ConditionalPersistResult.STATE_CHANGED;
  }
  
  public Optional<SingularityDeployStatistics> getDeployStatistics(String requestId, String deployId) {
    return getData(getDeployStatisticsPath(requestId, deployId), deployStatisticsTranscoder);
  }
  
  public SingularityCreateResult saveDeployStatistics(SingularityDeployStatistics newDeployStatistics) {
    return save(getDeployStatisticsPath(newDeployStatistics.getRequestId(), newDeployStatistics.getDeployId()), Optional.of(newDeployStatistics.getAsBytes(objectMapper))); 
  }
  
  private String getPendingDeployPath(String requestId) {
    return ZKPaths.makePath(PENDING_ROOT, requestId);
  }
  
  private String getCancelDeployPath(SingularityDeployMarker deployMarker) {
    return ZKPaths.makePath(CANCEL_ROOT, String.format("%s-%s", deployMarker.getRequestId(), deployMarker.getDeployId()));
  }
  
  public SingularityCreateResult cancelDeploy(SingularityDeployMarker deployMarker) {
    return create(getCancelDeployPath(deployMarker), Optional.of(deployMarker.getAsBytes(objectMapper)));
  }

  public SingularityDeleteResult deleteDeployHistory(SingularityDeployKey deployKey) {
    return delete(getDeployParentPath(deployKey.getRequestId(), deployKey.getDeployId()));
  }
  
  public SingularityDeleteResult deletePendingDeploy(SingularityPendingDeploy pendingDeploy) {
    return delete(getPendingDeployPath(pendingDeploy.getDeployMarker().getRequestId()));
  }
  
  public SingularityDeleteResult deleteCancelRequest(SingularityDeployMarker deployMarker) {
    return delete(getCancelDeployPath(deployMarker));
  }
  
  public SingularityCreateResult createPendingDeploy(SingularityPendingDeploy pendingDeploy) {
    return create(getPendingDeployPath(pendingDeploy.getDeployMarker().getRequestId()), Optional.of(pendingDeploy.getAsBytes(objectMapper)));
  }
  
  public SingularityCreateResult savePendingDeploy(SingularityPendingDeploy pendingDeploy) {
    return save(getPendingDeployPath(pendingDeploy.getDeployMarker().getRequestId()), Optional.of(pendingDeploy.getAsBytes(objectMapper)));
  }

  public Optional<SingularityPendingDeploy> getPendingDeploy(String requestId) {
    return getData(getPendingDeployPath(requestId), pendingDeployTranscoder);
  }
  
  public SingularityCreateResult saveDeployState(SingularityDeployMarker deploy, DeployState state) {
    return save(getDeployResultPath(deploy.getRequestId(), deploy.getDeployId()), Optional.of(deployStateTranscoder.toBytes(state)));
  }
  
  public Optional<DeployState> getDeployState(String requestId, String deployId) {
    return getData(getDeployResultPath(requestId, deployId), deployStateTranscoder);
  }

}
