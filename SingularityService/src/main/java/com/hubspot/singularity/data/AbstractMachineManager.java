package com.hubspot.singularity.data;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityMachineAbstraction;
import com.hubspot.singularity.SingularityMachineAbstraction.SingularityMachineState;
import com.hubspot.singularity.data.transcoders.Transcoder;

public abstract class AbstractMachineManager<T extends SingularityMachineAbstraction> extends CuratorAsyncManager {

  private final static Logger LOG = LoggerFactory.getLogger(AbstractMachineManager.class);
  
  private static final String ACTIVE_PATH = "active";
  private static final String DECOMISSIONING_PATH = "decomissioning";
  private static final String DEAD_PATH = "dead";
  
  private final ObjectMapper objectMapper;
  private final Transcoder<T> transcoder;
  
  public AbstractMachineManager(CuratorFramework curator, long zkAsyncTimeout, ObjectMapper objectMapper, Transcoder<T> transcoder) {
    super(curator, zkAsyncTimeout);
    
    this.objectMapper = objectMapper;
    this.transcoder = transcoder;
  }

  public abstract String getRoot();
    
  protected String getActiveRoot() {
    return ZKPaths.makePath(getRoot(), ACTIVE_PATH);
  }
  
  protected String getActivePath(String objectId) {
    return ZKPaths.makePath(getActiveRoot(), objectId);
  }
  
  protected String getDeadRoot() {
    return ZKPaths.makePath(getRoot(), DEAD_PATH);
  }
  
  protected String getDeadPath(String objectId) {
    return ZKPaths.makePath(getDeadRoot(), objectId);
  }
  
  protected String getDecomissioningRoot() {
    return ZKPaths.makePath(getRoot(), DECOMISSIONING_PATH);
  }
  
  protected String getDecomissioningPath(String objectId) {
    return ZKPaths.makePath(getDecomissioningRoot(), objectId);
  }
  
  public List<T> getActiveObjects() {
    return getObjects(getActiveRoot());
  }
  
  public List<T> getDeadObjects() {
    return getObjects(getDeadRoot());
  }
  
  public List<T> getDecomissioningObjects() {
    return getObjects(getDecomissioningRoot());
  }
  
  public List<T> getDecomissioningObjectsFiltered(List<T> decomissioning) {
    List<T> filtered = Lists.newArrayListWithCapacity(decomissioning.size());
    
    for (T object : decomissioning) {
      if (object.getState() == SingularityMachineState.DECOMISSIONING) {
        filtered.add(object);
      }
    }
    
    return filtered;
  }
  
  private Optional<T> getObject(String path) {
    return getData(path, transcoder);
  }
  
  public Optional<T> getActiveObject(String objectId) {
    return getObject(getActivePath(objectId));
  }
  
  public Optional<T> getDeadObject(String objectId) {
    return getObject(getDeadPath(objectId));
  }
  
  public Optional<T> getDecomissioning(String objectId) {
    return getObject(getDecomissioningPath(objectId));
  }
  
  private List<T> getObjects(String root) {
    return getAsyncChildren(root, transcoder);
  }
  
  public List<String> getActive() {
    return getChildren(getActiveRoot());
  }
  
  public List<String> getDecomissioning() {
    return getChildren(getDecomissioningRoot());
  }
  
  public List<String> getDead() {
    return getChildren(getDeadRoot());
  }
  
  public int getNumActive() {
    return getNumChildren(getActiveRoot());
  }
  
  public int getNumDecomissioning() {
    return getNumChildren(getDecomissioningRoot());
  }
  
  public int getNumDead() {
    return getNumChildren(getDeadRoot());
  }
  
  public void markAsDead(String objectId) {
    Optional<T> activeObject = getActiveObject(objectId);
    
    if (!activeObject.isPresent()) {
      LOG.warn(String.format("Marking an object %s as dead - but it wasn't active", objectId));
      return;
    }
 
    if (delete(getActivePath(objectId)) != SingularityDeleteResult.DELETED) {
      LOG.warn(String.format("Deleting active object at %s failed", getActivePath(objectId)));
    }
  
    activeObject.get().setState(SingularityMachineState.DEAD);
    activeObject.get().setDeadAt(Optional.of(System.currentTimeMillis()));
    
    if (create(getDeadPath(objectId), Optional.of(activeObject.get().getAsBytes(objectMapper))) != SingularityCreateResult.CREATED) {
      LOG.warn(String.format("Creating dead object at %s failed", getDeadPath(objectId)));
    }
  }
  
  private void mark(T object, String path, SingularityMachineState state) {
    object.setState(state);
    
    final byte[] data = object.getAsBytes(objectMapper);
    
    try {
      curator.setData().forPath(path, data);
    } catch (NoNodeException nne) {
      LOG.warn(String.format("Unexpected no node exception while storing decomissioned state for %s on path %s", object, path));
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
  
  public void markAsDecomissioned(T object) {
    object.setDecomissionedAt(Optional.of(System.currentTimeMillis()));
    mark(object, getDecomissioningPath(object.getId()), SingularityMachineState.DECOMISSIONED);
  }
  
  public SingularityDeleteResult removeDecomissioning(String objectId) {
    return delete(getDecomissioningPath(objectId));
  }
  
  public SingularityDeleteResult removeDead(String objectId) {
    return delete(getDeadPath(objectId));
  }
  
  public enum DecomissionResult {
    SUCCESS_DECOMISSIONING, FAILURE_NOT_FOUND, FAILURE_ALREADY_DECOMISSIONING, FAILURE_DEAD;
  }
  
  public DecomissionResult decomission(String objectId, Optional<String> user) {
    Optional<T> object = getActiveObject(objectId);
    
    if (!object.isPresent()) {
      if (isDecomissioning(objectId)) {
        return DecomissionResult.FAILURE_ALREADY_DECOMISSIONING;
      } else if (isDead(objectId)) {
        return DecomissionResult.FAILURE_DEAD;
      }
      return DecomissionResult.FAILURE_NOT_FOUND;
    }
    
    object.get().setState(SingularityMachineState.DECOMISSIONING);
    object.get().setDecomissioningAt(Optional.of(System.currentTimeMillis()));
    object.get().setDecomissioningBy(user);
    
    create(getDecomissioningPath(objectId), Optional.of(object.get().getAsBytes(objectMapper)));
    
    delete(getActivePath(objectId));
    
    return DecomissionResult.SUCCESS_DECOMISSIONING;
  }
  
  public boolean isActive(String objectId) {
    return exists(getActivePath(objectId));
  }
  
  public boolean isDead(String objectId) {
    return exists(getDeadPath(objectId));
  }
  
  public boolean isDecomissioning(String objectId) {
    return exists(getDecomissioningPath(objectId));
  }
  
  public int clearActive() {
    int numCleared = 0;
    
    for (String active : getActive()) {
      numCleared += 1;
      delete(getActivePath(active));
    }
    
    return numCleared;
  }
  
  public SingularityCreateResult save(T object) {
    return create(getActivePath(object.getId()), Optional.of(object.getAsBytes(objectMapper)));
  }
  
}
