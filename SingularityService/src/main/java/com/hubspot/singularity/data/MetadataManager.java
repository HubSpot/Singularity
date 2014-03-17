package com.hubspot.singularity.data;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.KeeperException.NoNodeException;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityCreateResult;

public class MetadataManager extends CuratorManager {

  private static final String ROOT_PATH = "/metadata";
  
  private static final String LAST_STALE_CHECK_PATH = ZKPaths.makePath(ROOT_PATH, "lastStaleCheckTimestamp");
  
  @Inject
  public MetadataManager(CuratorFramework curator) {
    super(curator);
  }

  public Optional<Long> getLastCheckTimestamp() {
    try {
      byte[] bytes = curator.getData().forPath(LAST_STALE_CHECK_PATH);
      
      if (bytes != null) {
        String timestamp = JavaUtils.toString(bytes);
        return Optional.of(Long.parseLong(timestamp));
      }
      
    } catch (NoNodeException nne) {
    } catch (Throwable t) {
      throw Throwables.propagate(t); 
    }
    
    return Optional.absent();
  }
  
  public void saveLastCheckTimestamp(long newTimestamp) {
    byte[] data = JavaUtils.toBytes(Long.toString(newTimestamp));
    Optional<byte[]> dataOptional = Optional.of(data);    
    
    SingularityCreateResult result = create(LAST_STALE_CHECK_PATH, dataOptional);
    
    if (result == SingularityCreateResult.EXISTED) {
      try {
        curator.setData().forPath(LAST_STALE_CHECK_PATH, data);
      } catch (Throwable t) {
        throw Throwables.propagate(t);
      }
    }
  }
  
}
