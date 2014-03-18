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
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityDeployState;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.SingularityDeployMarkerTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityDeployStateTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityDeployTranscoder;

public class DeployManager extends CuratorAsyncManager {
  
  private final static Logger LOG = LoggerFactory.getLogger(DeployManager.class);
  
  private final ObjectMapper objectMapper;
  
  private final SingularityDeployTranscoder deployTranscoder;
  private final SingularityDeployMarkerTranscoder deployMarkerTranscoder;
  private final SingularityDeployStateTranscoder deployStateTranscoder;
  
  private final static String DEPLOY_ROOT = "/deploys";
  
  private final static String ACTIVE_ROOT = DEPLOY_ROOT + "/active";
  private final static String BY_REQUEST_ROOT = DEPLOY_ROOT + "/requests";
  
  private final static String DEPLOY_STATE_KEY = "STATE";
  private final static String DEPLOY_LIST_KEY = "/ids"; 
  
  @Inject
  public DeployManager(SingularityConfiguration configuration, CuratorFramework curator, SingularityDeployTranscoder deployTranscoder, SingularityDeployMarkerTranscoder deployMarkerTranscoder, SingularityDeployStateTranscoder deployStateTranscoder, ObjectMapper objectMapper) {
    super(curator, configuration.getZookeeperAsyncTimeout());
    
    this.objectMapper = objectMapper;
      
    this.deployTranscoder = deployTranscoder;
    this.deployMarkerTranscoder = deployMarkerTranscoder;
    this.deployStateTranscoder = deployStateTranscoder;
  }
  
  public List<SingularityDeployMarker> getActiveDeploys() {
    return getAsyncChildren(ACTIVE_ROOT, deployMarkerTranscoder);
  }
  
  private String getRequestDeployPath(String requestId) {
    return ZKPaths.makePath(BY_REQUEST_ROOT, requestId);
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
        return SingularityDeployKey.fromDepoy(input);
      }  
    });
    
    return deployKeyToDeploy;
  }
  
  public ConditionalPersistResult persistDeploy(SingularityDeployMarker deployMarker, SingularityDeploy deploy) {
    SingularityCreateResult deploySaveResult = create(getDeployPath(deploy.getRequestId(), deploy.getId()), Optional.of(deploy.getAsBytes(objectMapper)));
    
    if (deploySaveResult == SingularityCreateResult.EXISTED) {
      LOG.info(String.format("Deploy object for %s already existed (new marker: %s)", deploy, deployMarker));
    }
    
    Optional<SingularityDeployStateHelper> currentState = getDeployStateHelper(deploy.getRequestId());
    
    Optional<SingularityDeployMarker> activeDeploy = Optional.absent();
    Optional<Stat> deployStateStat = Optional.absent();
    
    if (currentState.isPresent()) {
      activeDeploy = currentState.get().getDeployState().getActiveDeploy();
      deployStateStat = Optional.of(currentState.get().getStat());
    }
    
    SingularityDeployState newState = new SingularityDeployState(deploy.getRequestId(), activeDeploy, Optional.of(deployMarker));
    
    return saveNewDeployState(newState, deployStateStat, !deployStateStat.isPresent());
  }
  
  public Optional<SingularityDeploy> getDeploy(String requestId, String deployId) {
    final String deployPath = getDeployPath(requestId, deployId);
    
    return getData(deployPath, deployTranscoder);
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
  
  private String getDeployMarkerPath(String requestId) {
    return ZKPaths.makePath(ACTIVE_ROOT, requestId);
  }
  
  public void deleteActiveDeploy(SingularityDeployMarker deployMarker) {
    delete(getDeployMarkerPath(deployMarker.getRequestId()));
  }
  
  public SingularityCreateResult markDeploy(SingularityDeployMarker deployMarker) {
    return create(getDeployMarkerPath(deployMarker.getRequestId()), Optional.of(deployMarker.getAsBytes(objectMapper)));
  }

  public Optional<SingularityDeployMarker> getDeployMarker(String requestId) {
    return getData(getDeployMarkerPath(requestId), deployMarkerTranscoder);
  }  

}
