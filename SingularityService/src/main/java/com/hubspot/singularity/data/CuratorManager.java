package com.hubspot.singularity.data;

import java.util.Collections;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.data.transcoders.StringTranscoder;
import com.hubspot.singularity.data.transcoders.Transcoder;

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
  
  protected boolean exists(String path) {
    try {
      Stat s = curator.checkExists().forPath(path);
      return s != null;
    } catch (NoNodeException nne) {
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
    
    return false;
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
  
  protected SingularityDeleteResult delete(String path) {
    try {
      curator.delete().forPath(path);
      return SingularityDeleteResult.DELETED;
    } catch (NoNodeException nne) {
      LOG.warn(String.format("Tried to delete an item at path %s that didn't exist", path));
      return SingularityDeleteResult.DIDNT_EXIST;
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }
  
  protected SingularityCreateResult create(String path) {
    return create(path, Optional.<byte[]> absent());
  }
  
  protected SingularityCreateResult create(String path, Optional<byte[]> data) {
    try {
      if (data.isPresent()) {
        curator.create().creatingParentsIfNeeded().forPath(path, data.get());
      } else {
        curator.create().creatingParentsIfNeeded().forPath(path);
      }
      return SingularityCreateResult.CREATED;
    } catch (NodeExistsException nee) {
      return SingularityCreateResult.EXISTED;
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }
  
  protected <T> Optional<T> getData(String path, Transcoder<T> transcoder) {
    try {
      byte[] data = curator.getData().forPath(path);
      
      if (data == null || data.length == 0) {
        return Optional.absent();
      }
      
      return Optional.of(transcoder.transcode(data));
    } catch (NoNodeException nne) {
      return Optional.absent();
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }
  
  protected Optional<String> getStringData(String path) {
    return getData(path, StringTranscoder.STRING_TRANSCODER);
  }  
  
}
