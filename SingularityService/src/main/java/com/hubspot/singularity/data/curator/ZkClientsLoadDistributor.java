package com.hubspot.singularity.data.curator;

import java.util.List;
import org.apache.curator.framework.CuratorFramework;

public class ZkClientsLoadDistributor {
  private final List<CuratorFramework> curatorFrameworks;

  public ZkClientsLoadDistributor(List<CuratorFramework> curatorFrameworks) {
    this.curatorFrameworks = curatorFrameworks;
  }

  public void start() {}

  public void close() {}
}
