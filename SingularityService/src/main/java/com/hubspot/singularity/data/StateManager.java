package com.hubspot.singularity.data;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.NoNodeException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityHostState;

public class StateManager extends CuratorManager {

  private static final String ROOT_PATH = "/hosts";
  
  private final ObjectMapper objectMapper;
  
  @Inject
  public StateManager(CuratorFramework curator, ObjectMapper objectMapper) {
    super(curator);
    
    this.objectMapper = objectMapper;
  }

  public void save(SingularityHostState hostState) {
    final String path = ZKPaths.makePath(ROOT_PATH, hostState.getHostname());
    final byte[] data = hostState.getAsBytes(objectMapper);
   
    try {
      if (exists(path)) {
        curator.setData().forPath(path, data);
      } else {
        curator.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path, data);
      }
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }
  
  public List<SingularityHostState> getHostStates() {
    List<String> children = getChildren(ROOT_PATH);
    List<SingularityHostState> states = Lists.newArrayListWithCapacity(children.size());
    
    for (String child : children) {
      
      try {
        byte[] bytes = curator.getData().forPath(ZKPaths.makePath(ROOT_PATH, child));
        
        states.add(SingularityHostState.fromBytes(bytes, objectMapper));
      } catch (NoNodeException nne) {
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }
    
    return states;
  }
}
