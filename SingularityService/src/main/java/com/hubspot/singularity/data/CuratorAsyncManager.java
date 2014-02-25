package com.hubspot.singularity.data;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.BackgroundCallback;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

public abstract class CuratorAsyncManager<T> extends CuratorManager {

  private final static Logger LOG = LoggerFactory.getLogger(CuratorAsyncManager.class);
  
  private final long zkAsyncTimeout;
   
  public CuratorAsyncManager(CuratorFramework curator, long zkAsyncTimeout) {
    super(curator);
    
    this.zkAsyncTimeout = zkAsyncTimeout;
  }
   
  protected abstract T fromData(byte[] data) throws Exception;
  
  private List<T> getAsyncChildrenThrows(final String parent) throws Exception {
    final List<String> children = getChildren(parent);
    
    final List<T> objects = Lists.newArrayListWithCapacity(children.size());
    final CountDownLatch latch = new CountDownLatch(children.size());
    
    BackgroundCallback callback = new BackgroundCallback() {
      
      @Override
      public void processResult(CuratorFramework client, CuratorEvent event) throws Exception {
        latch.countDown();
        
        if (event.getData() == null || event.getData().length == 0) {
          LOG.info(String.format("Expected active node %s but it wasn't there", event.getPath()));
          return;
        }
        
        objects.add(fromData(event.getData()));
      }
    };
    
    for (String child : children) {
      final String path = ZKPaths.makePath(parent, child);
      
      curator.getData().inBackground(callback).forPath(path);
    }
    
    if (!latch.await(zkAsyncTimeout, TimeUnit.MILLISECONDS)) {
      throw new IllegalStateException(String.format("Timed out waiting response for objects from %s, waited %s millis", parent, zkAsyncTimeout)); 
    }
    
    return objects;
  }
  
  public List<T> getAsyncChildren(final String parent) {
    try {
      return getAsyncChildrenThrows(parent);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

}
