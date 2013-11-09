package com.hubspot.singularity.data;

import java.util.Collections;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

public abstract class CuratorManager {

  private final static Logger LOG = LoggerFactory.getLogger(CuratorManager.class);
  
  protected final CuratorFramework curator;

  public CuratorManager(CuratorFramework curator) {
    this.curator = curator;
  }
    
  protected int getNumChildren(String path) {
    try {
      Stat s = curator.checkExists().forPath(path);
      if (s != null) {
        return s.getNumChildren();
      }
    } catch (NoNodeException nne) {
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
    
    return 0;
  }
  
  protected List<String> getChildren(String root) {
    try {
      return curator.getChildren().forPath(root);
    } catch (NoNodeException nne) {
      return Collections.emptyList();
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }
  
  protected void delete(String path) {
    try {
      curator.delete().forPath(path);
    } catch (NoNodeException nne) {
      LOG.warn(String.format("Expected item at %s", path), nne);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }
  
  protected void create(String path) {
    try {
      curator.create().creatingParentsIfNeeded().forPath(path);
    } catch (NodeExistsException nee) {
      // ignored
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }
  
  
}
