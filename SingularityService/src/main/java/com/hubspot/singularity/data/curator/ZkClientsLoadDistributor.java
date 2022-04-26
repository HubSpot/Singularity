package com.hubspot.singularity.data.curator;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZkClientsLoadDistributor {

  private static final Logger LOG = LoggerFactory.getLogger(
    ZkClientsLoadDistributor.class
  );
  private static final ThreadLocal<Integer> ZK_CONTEXT = new ThreadLocal<>();

  private final List<CuratorFramework> curatorFrameworks;
  private final AtomicInteger curatorIndex;

  public ZkClientsLoadDistributor(List<CuratorFramework> curatorFrameworks) {
    this.curatorFrameworks = curatorFrameworks;
    this.curatorIndex = new AtomicInteger(0);
  }

  public void start() {
    for (CuratorFramework framework : curatorFrameworks) {
      try {
        framework.start();
      } catch (Exception e) {
        LOG.warn("Error starting framework: ");
      }
    }
  }

  public void close() {
    for (CuratorFramework framework : curatorFrameworks) {
      try {
        framework.close();
      } catch (Exception e) {
        LOG.warn("Error starting framework: ");
      }
    }
  }

  public CuratorFramework getCuratorFramework() {
    if (ZK_CONTEXT.get() == null) {
      int ci = curatorIndex.getAndUpdate(i -> (i + 1) % curatorFrameworks.size());
      ZK_CONTEXT.set(ci);
    }
    return curatorFrameworks.get(ZK_CONTEXT.get());
  }

  public List<CuratorFramework> getAll() {
    return this.curatorFrameworks;
  }

  public boolean isStarted() {
    boolean started = true;
    for (CuratorFramework framework : curatorFrameworks) {
      started = started & framework.isStarted();
    }
    return started;
  }
}
