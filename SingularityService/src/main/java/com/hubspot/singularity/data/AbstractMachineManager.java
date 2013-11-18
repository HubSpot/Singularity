package com.hubspot.singularity.data;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;

public abstract class AbstractMachineManager extends CuratorManager {

  private static final String ACTIVE_PATH = "active";
  private static final String DECOMISSIONING_PATH = "decomissioning";
  private static final String DEAD_PATH = "dead";
  
  public AbstractMachineManager(CuratorFramework curator) {
    super(curator);
  }

  public abstract String getRoot();
  
  protected String getActiveRoot() {
    return ZKPaths.makePath(getRoot(), ACTIVE_PATH);
  }
  
  protected String getActivePath(String object) {
    return ZKPaths.makePath(getActiveRoot(), object);
  }
  
  protected String getDeadRoot() {
    return ZKPaths.makePath(getRoot(), DEAD_PATH);
  }
  
  protected String getDeadPath(String object) {
    return ZKPaths.makePath(getDeadRoot(), object);
  }
  
  protected String getDecomissioningRoot() {
    return ZKPaths.makePath(getRoot(), DECOMISSIONING_PATH);
  }
  
  protected String getDecomissioningPath(String object) {
    return ZKPaths.makePath(getDecomissioningRoot(), object);
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
  
  public void markAsDead(String object) {
    delete(getActivePath(object));
    create(getDeadPath(object));
  }
  
  public DeleteResult removeDecomissioning(String object) {
    return delete(getDecomissioningPath(object));
  }
  
  public DeleteResult removeDead(String object) {
    return delete(getDeadPath(object));
  }
  
  public void decomission(String object) {
    create(getDecomissioningPath(object));
    delete(getActivePath(object));
  }
  
  public void addToActive(String object) {
    create(getActivePath(object));
  }
  
  public boolean isActive(String object) {
    return exists(getActivePath(object));
  }
  
  public boolean isDead(String object) {
    return exists(getDeadPath(object));
  }
  
  public boolean isDecomissioning(String object) {
    return exists(getDecomissioningPath(object));
  }
  
  public int clearActive() {
    int numCleared = 0;
    
    for (String active : getActive()) {
      numCleared += 1;
      delete(getActivePath(active));
    }
    
    return numCleared;
  }
  
}
