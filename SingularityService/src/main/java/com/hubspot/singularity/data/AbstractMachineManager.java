package com.hubspot.singularity.data;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.hubspot.singularity.SingularityMachineAbstraction;
import com.hubspot.singularity.SingularityMachineAbstraction.SingularityMachineState;

public abstract class AbstractMachineManager<T extends SingularityMachineAbstraction> extends CuratorManager {

  private final static Logger LOG = LoggerFactory.getLogger(AbstractMachineManager.class);
  
  private static final String ACTIVE_PATH = "active";
  private static final String DECOMISSIONING_PATH = "decomissioning";
  private static final String DEAD_PATH = "dead";
  
  private final ObjectMapper objectMapper;
  
  public AbstractMachineManager(CuratorFramework curator, ObjectMapper objectMapper) {
    super(curator);
    
    this.objectMapper = objectMapper;
  }

  public abstract String getRoot();
  
  public abstract T fromBytes(byte[] bytes);
  
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
  
  public List<T> getDecomissioningObjectsFiltered() {
    List<T> decomissioning = getDecomissioningObjects();
    List<T> filtered = Lists.newArrayListWithCapacity(decomissioning.size());
    
    for (T object : decomissioning) {
      if (object.getStateEnum() == SingularityMachineState.DECOMISSIONING) {
        filtered.add(object);
      }
    }
    
    return filtered;
  }
  
  private Optional<T> getObject(String path) {
    try {
      byte[] bytes = curator.getData().forPath(path);
      return Optional.of(fromBytes(bytes));
    } catch (NoNodeException nee) {
      return Optional.absent();
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
  
  public Optional<T> getActiveObject(String objectId) {
    return getObject(getActivePath(objectId));
  }
  
  public Optional<T> getDeadSlave(String objectId) {
    return getObject(getDeadPath(objectId));
  }
  
  public Optional<T> getDecomissioning(String objectId) {
    return getObject(getDecomissioningPath(objectId));
  }
  
  private List<T> getObjects(String root) {
    List<String> children = getChildren(root);
    List<T> objects = Lists.newArrayListWithCapacity(children.size());
    
    for (String child : children) {
      final String fullPath = ZKPaths.makePath(root, child);
      
      try {
        byte[] bytes = curator.getData().forPath(fullPath);
        
        objects.add(fromBytes(bytes));
        
      } catch (NoNodeException nne) {
        LOG.warn(String.format("Unexpected no node exception while fetching objects on path %s", fullPath));
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
      
    }
    
    return objects;    
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
    
    // return this?
    if (!activeObject.isPresent()) {
      LOG.warn(String.format("Marking an object %s as dead - but it wasn't active", objectId));
    }
 
    if (delete(getActivePath(objectId)) != DeleteResult.DELETED) {
      LOG.warn(String.format("Deleting active object at %s failed", getActivePath(objectId)));
    }
  
    activeObject.get().setState(SingularityMachineState.DEAD);
    
    if (create(getDeadPath(objectId), Optional.of(activeObject.get().getAsBytes(objectMapper))) != CreateResult.CREATED) {
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
    mark(object, getDecomissioningPath(object.getId()), SingularityMachineState.DECOMISSIONED);
  }
  
  public DeleteResult removeDecomissioning(String objectId) {
    return delete(getDecomissioningPath(objectId));
  }
  
  public DeleteResult removeDead(String objectId) {
    return delete(getDeadPath(objectId));
  }
  
  public enum DecomissionResult {
    SUCCESS_DECOMISSIONING, FAILURE_NOT_FOUND, FAILURE_ALREADY_DECOMISSIONING, FAILURE_DEAD;
  }
  
  public DecomissionResult decomission(String objectId) {
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
  
  public void save(T object) {
    final String path = getActivePath(object.getId());
    
    try {
      curator.create().creatingParentsIfNeeded().forPath(path, object.getAsBytes(objectMapper));
    } catch (NodeExistsException nee) {
      LOG.warn(String.format("Node already existed for object %s at path %s", object, path));
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
  
}
