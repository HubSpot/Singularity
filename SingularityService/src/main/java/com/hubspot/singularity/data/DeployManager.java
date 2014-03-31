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
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityDeployState;
import com.hubspot.singularity.SingularityDeployStatistics;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.SingularityDeployMarkerTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityDeployStateTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityDeployStatisticsTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityDeployTranscoder;

public class DeployManager extends CuratorAsyncManager {
  
  private final static Logger LOG = LoggerFactory.getLogger(DeployManager.class);
  
  private final ObjectMapper objectMapper;
  
  private final SingularityDeployTranscoder deployTranscoder;
  private final SingularityDeployMarkerTranscoder deployMarkerTranscoder;
  private final SingularityDeployStateTranscoder deployStateTranscoder;
  private final SingularityDeployStatisticsTranscoder deployStatisticsTranscoder;
  
  private final static String DEPLOY_ROOT = "/deploys";
  
  private final static String ACTIVE_ROOT = DEPLOY_ROOT + "/active";
  private final static String CANCEL_ROOT = DEPLOY_ROOT + "/cancel";
 
  private final static String BY_REQUEST_ROOT = DEPLOY_ROOT + "/requests";
  
  private final static String DEPLOY_STATE_KEY = "STATE";
  private final static String DEPLOY_LIST_KEY = "/ids"; 
  
  private final static String DEPLOY_STATISTICS_KEY = "STATISTICS";
  
  @Inject
  public DeployManager(SingularityConfiguration configuration, CuratorFramework curator, SingularityDeployTranscoder deployTranscoder, SingularityDeployMarkerTranscoder deployMarkerTranscoder, SingularityDeployStatisticsTranscoder deployStatisticsTranscoder, SingularityDeployStateTranscoder deployStateTranscoder, ObjectMapper objectMapper) {
    super(curator, configuration.getZookeeperAsyncTimeout());
    
    this.objectMapper = objectMapper;
      
    this.deployTranscoder = deployTranscoder;
    this.deployStatisticsTranscoder = deployStatisticsTranscoder;
    this.deployMarkerTranscoder = deployMarkerTranscoder;
    this.deployStateTranscoder = deployStateTranscoder;
  }
  
  public List<SingularityDeployMarker> getCancelDeploys() {
    return getAsyncChildren(CANCEL_ROOT, deployMarkerTranscoder);
  }
  
  public List<SingularityDeployMarker> getActiveDeploys() {
    return getAsyncChildren(ACTIVE_ROOT, deployMarkerTranscoder);
  }
  
  private String getRequestDeployPath(String requestId) {
    return ZKPaths.makePath(BY_REQUEST_ROOT, requestId);
  }
  
  private String getDeployStatisticsPath(String requestId, String deployId) {
    return ZKPaths.makePath(getDeployPath(requestId, deployId), DEPLOY_STATISTICS_KEY);
  }
  
  private String getDeployPath(String requestId, String deployId) {
    return ZKPaths.makePath(getRequestDeployPath(requestId), ZKPaths.makePath(DEPLOY_LIST_KEY, deployId));
  }
  
  private String getDeployStatePath(String requestId) {
    return ZKPaths.makePath(getRequestDeployPath(requestId), DEPLOY_STATE_KEY);
  }
  
  public Map<SingularityDeployKey, SingularityDeploy> getDeploysForKeys(Collection<SingularityDeployKey> deployKeys) {
    final List<String> paths = Lists.newArrayListWithCapacity(deployKeys.size());
    
    for (SingularityDeployKey deployKey : deployKeys) {
      paths.add(getDeployPath(deployKey.getRequestId(), deployKey.getDeployId()));
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
    final SingularityCreateResult deploySaveResult = create(getDeployPath(deploy.getRequestId(), deploy.getId()), Optional.of(deploy.getAsBytes(objectMapper)));
    
    if (deploySaveResult == SingularityCreateResult.EXISTED) {
      LOG.info(String.format("Deploy object for %s already existed (new marker: %s)", deploy, deployMarker));
    }
    
    final Optional<SingularityDeployStateHelper> currentState = getDeployStateHelper(deploy.getRequestId());
    
    final Optional<Stat> deployStateStat = currentState.isPresent() ? Optional.of(currentState.get().getStat()) : Optional.<Stat> absent();
    
    Optional<SingularityDeployMarker> activeDeploy = Optional.absent();
    Optional<SingularityDeployMarker> pendingDeploy = Optional.absent();
    
    if (request.isDeployable()) {
      if (currentState.isPresent()) {
        activeDeploy = currentState.get().getDeployState().getActiveDeploy();
      }
      pendingDeploy = Optional.of(deployMarker);
    } else {
      activeDeploy = Optional.of(deployMarker);
    }
    
    final SingularityDeployState newState = new SingularityDeployState(deploy.getRequestId(), activeDeploy, pendingDeploy);
    
    return saveNewDeployState(newState, deployStateStat, !deployStateStat.isPresent());
  }
  
  public Optional<SingularityDeploy> getDeploy(String requestId, String deployId) {
    final String deployPath = getDeployPath(requestId, deployId);
    
    return getData(deployPath, deployTranscoder);
  }
  
  public Optional<String> getInUseDeployId(String requestId) {
    Optional<SingularityDeployState> deployState = getDeployState(requestId);
    
    if (!deployState.isPresent() || (!deployState.get().getActiveDeploy().isPresent() && !deployState.get().getPendingDeploy().isPresent())) {
      return Optional.absent();
    }
    
    return Optional.of(deployState.get().getActiveDeploy().or(deployState.get().getPendingDeploy()).get().getDeployId());
  }
  
  public Optional<SingularityDeployState> getDeployState(String requestId) {
    Optional<SingularityDeployStateHelper> maybeHelper = getDeployStateHelper(requestId);
    
    if (!maybeHelper.isPresent()) {
      return Optional.absent();
    }
  
    return Optional.of(maybeHelper.get().getDeployState());
  }
  
  private Optional<SingularityDeployStateHelper> getDeployStateHelper(String requestId) {
    final String statePath = getDeployStatePath(requestId);
    
    Stat stat = new Stat();
    
    Optional<SingularityDeployState> deployState = getData(statePath, Optional.of(stat), deployStateTranscoder);
    
    if (!deployState.isPresent()) {
      return Optional.absent(); 
    }
    
    return Optional.of(new SingularityDeployStateHelper(deployState.get(), stat));
  }
  
  public class SingularityDeployStateHelper {
    
    private final SingularityDeployState deployState;
    private final Stat stat;
    
    public SingularityDeployStateHelper(SingularityDeployState deployState, Stat stat) {
      this.deployState = deployState;
      this.stat = stat;
    }

    public SingularityDeployState getDeployState() {
      return deployState;
    }

    public Stat getStat() {
      return stat;
    }
    
  }
  
  public enum ConditionalPersistResult {
    
    SAVED, STATE_CHANGED;
  
  }
  
  public ConditionalPersistResult saveNewDeployState(SingularityDeployState newDeployState, Optional<Stat> maybeStat, boolean createNew) {
    final String statePath = getDeployStatePath(newDeployState.getRequestId());
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
  
  private String getDeployMarkerPath(String requestId) {
    return ZKPaths.makePath(ACTIVE_ROOT, requestId);
  }
  
  private String getCancelDeployPath(SingularityDeployMarker deployMarker) {
    return ZKPaths.makePath(CANCEL_ROOT, String.format("%s-%s", deployMarker.getRequestId(), deployMarker.getDeployId()));
  }
  
  public SingularityCreateResult cancelDeploy(SingularityDeployMarker deployMarker) {
    return create(getCancelDeployPath(deployMarker), Optional.of(deployMarker.getAsBytes(objectMapper)));
  }
  
  public SingularityDeleteResult deleteActiveDeploy(SingularityDeployMarker deployMarker) {
    return delete(getDeployMarkerPath(deployMarker.getRequestId()));
  }
  
  public SingularityDeleteResult deleteCancelRequest(SingularityDeployMarker deployMarker) {
    return delete(getCancelDeployPath(deployMarker));
  }
  
  public SingularityCreateResult markDeploy(SingularityDeployMarker deployMarker) {
    return create(getDeployMarkerPath(deployMarker.getRequestId()), Optional.of(deployMarker.getAsBytes(objectMapper)));
  }

  public Optional<SingularityDeployMarker> getDeployMarker(String requestId) {
    return getData(getDeployMarkerPath(requestId), deployMarkerTranscoder);
  }  

}
