package com.hubspot.singularity.data.curator;

import com.google.common.collect.Iterators;
import java.util.Iterator;
import java.util.List;
import org.apache.curator.framework.CuratorFramework;

public class ZkClientsLoadDistributor {
  private final List<CuratorFramework> curatorFrameworks;
  private final Iterator<CuratorFramework> iterator;

  public ZkClientsLoadDistributor(List<CuratorFramework> curatorFrameworks) {
    this.curatorFrameworks = curatorFrameworks;
    this.iterator = Iterators.cycle(curatorFrameworks);
  }

  public void start() {
    for (CuratorFramework framework : curatorFrameworks) {
      framework.start();
    }
  }

  public void close() {
    for (CuratorFramework framework : curatorFrameworks) {
      framework.close();
    }
  }

  public CuratorFramework getCuratorFramework() {
    return iterator.next();
  }

  public List<CuratorFramework> getAll() {
    return this.curatorFrameworks;
  }
}
