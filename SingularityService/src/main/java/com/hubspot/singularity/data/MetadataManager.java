package com.hubspot.singularity.data;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.mesos.JavaUtils;

@Singleton
public class MetadataManager extends CuratorManager {

  private static final String ROOT_PATH = "/metadata";
  private static final String ZK_DATA_VERSION_PATH = ZKPaths.makePath(ROOT_PATH, "ZK_DATA_VERSION");

  @Inject
  public MetadataManager(CuratorFramework curator) {
    super(curator);
  }

  public Optional<String> getZkDataVersion() {
    return getStringData(ZK_DATA_VERSION_PATH);
  }

  public void setZkDataVersion(String newVersion) {
    save(ZK_DATA_VERSION_PATH, Optional.of(JavaUtils.toBytes(newVersion)));
  }

}
