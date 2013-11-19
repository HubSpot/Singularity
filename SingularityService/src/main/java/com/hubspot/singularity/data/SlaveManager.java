package com.hubspot.singularity.data;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularitySlave;

public class SlaveManager extends AbstractMachineManager {

  private final static Logger LOG = LoggerFactory.getLogger(SlaveManager.class);
  
  private static final String SLAVE_ROOT = "slaves";
  
  private final ObjectMapper objectMapper;
  
  @Inject
  public SlaveManager(CuratorFramework curator, ObjectMapper objectMapper) {
    super(curator);
  
    this.objectMapper = objectMapper;
  }
  
  @Override
  public String getRoot() {
    return SLAVE_ROOT;
  }
  
  public Optional<SingularitySlave> getActiveSlave(String slaveId) {
    try {
      byte[] data = curator.getData().forPath(getActivePath(slaveId));
      return Optional.of(SingularitySlave.fromBytes(data, objectMapper));
    } catch (NoNodeException nee) {
      return Optional.absent();
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
  
  public void save(SingularitySlave slave) {
    final String path = getActivePath(slave.getSlaveId());
    try {
      curator.create().creatingParentsIfNeeded().forPath(path, slave.getAsBytes(objectMapper));
    } catch (NodeExistsException nee) {
      LOG.warn(String.format("Node already existed for slave %s at path %s", slave, path));
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
  
  public List<SingularitySlave> getActiveSlaves() {
    List<String> activePaths = getActive();
    List<SingularitySlave> slaves = Lists.newArrayListWithCapacity(activePaths.size());
    
    for (String activePath : activePaths) {
      try {
        slaves.add(SingularitySlave.fromBytes(curator.getData().forPath(getActivePath(activePath)), objectMapper));
      } catch (NoNodeException nne) {
        LOG.warn(String.format("Unexpected no node exception while fetching active slaves on path %s", activePath));
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }
    
    return slaves;
  }
  
  
  
}
