package com.hubspot.singularity.data.curator;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.apache.curator.CuratorZookeeperClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.WatcherRemoveCuratorFramework;
import org.apache.curator.framework.api.CreateBuilder;
import org.apache.curator.framework.api.CuratorListener;
import org.apache.curator.framework.api.DeleteBuilder;
import org.apache.curator.framework.api.ExistsBuilder;
import org.apache.curator.framework.api.GetACLBuilder;
import org.apache.curator.framework.api.GetChildrenBuilder;
import org.apache.curator.framework.api.GetConfigBuilder;
import org.apache.curator.framework.api.GetDataBuilder;
import org.apache.curator.framework.api.ReconfigBuilder;
import org.apache.curator.framework.api.RemoveWatchesBuilder;
import org.apache.curator.framework.api.SetACLBuilder;
import org.apache.curator.framework.api.SetDataBuilder;
import org.apache.curator.framework.api.SyncBuilder;
import org.apache.curator.framework.api.UnhandledErrorListener;
import org.apache.curator.framework.api.transaction.CuratorMultiTransaction;
import org.apache.curator.framework.api.transaction.CuratorTransaction;
import org.apache.curator.framework.api.transaction.TransactionOp;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.listen.Listenable;
import org.apache.curator.framework.schema.SchemaSet;
import org.apache.curator.framework.state.ConnectionStateErrorPolicy;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.utils.EnsurePath;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.server.quorum.flexible.QuorumVerifier;

public class SingularityCuratorFramework implements CuratorFramework {
  private final ZkClientsLoadDistributor distributor;

  public SingularityCuratorFramework(ZkClientsLoadDistributor distributor) {
    this.distributor = distributor;
  }

  @Override
  public void start() {
    distributor.start();
  }

  @Override
  public void close() {
    distributor.close();
  }

  @Override
  public CuratorFrameworkState getState() {
    return null;
  }

  @Override
  public boolean isStarted() {
    return false;
  }

  @Override
  public CreateBuilder create() {
    return null;
  }

  @Override
  public DeleteBuilder delete() {
    return null;
  }

  @Override
  public ExistsBuilder checkExists() {
    return null;
  }

  @Override
  public GetDataBuilder getData() {
    return null;
  }

  @Override
  public SetDataBuilder setData() {
    return null;
  }

  @Override
  public GetChildrenBuilder getChildren() {
    return null;
  }

  @Override
  public GetACLBuilder getACL() {
    return null;
  }

  @Override
  public SetACLBuilder setACL() {
    return null;
  }

  @Override
  public ReconfigBuilder reconfig() {
    return null;
  }

  @Override
  public GetConfigBuilder getConfig() {
    return null;
  }

  @Override
  public CuratorTransaction inTransaction() {
    return null;
  }

  @Override
  public CuratorMultiTransaction transaction() {
    return null;
  }

  @Override
  public TransactionOp transactionOp() {
    return null;
  }

  @Override
  public void sync(String path, Object backgroundContextObject) {}

  @Override
  public void createContainers(String path) throws Exception {}

  @Override
  public SyncBuilder sync() {
    return null;
  }

  @Override
  public RemoveWatchesBuilder watches() {
    return null;
  }

  @Override
  public Listenable<ConnectionStateListener> getConnectionStateListenable() {
    return null;
  }

  @Override
  public Listenable<CuratorListener> getCuratorListenable() {
    return null;
  }

  @Override
  public Listenable<UnhandledErrorListener> getUnhandledErrorListenable() {
    return null;
  }

  @Override
  public CuratorFramework nonNamespaceView() {
    return null;
  }

  @Override
  public CuratorFramework usingNamespace(String newNamespace) {
    return null;
  }

  @Override
  public String getNamespace() {
    return null;
  }

  @Override
  public CuratorZookeeperClient getZookeeperClient() {
    return null;
  }

  @Override
  public EnsurePath newNamespaceAwareEnsurePath(String path) {
    return null;
  }

  @Override
  public void clearWatcherReferences(Watcher watcher) {}

  @Override
  public boolean blockUntilConnected(int maxWaitTime, TimeUnit units)
    throws InterruptedException {
    return false;
  }

  @Override
  public void blockUntilConnected() throws InterruptedException {}

  @Override
  public WatcherRemoveCuratorFramework newWatcherRemoveCuratorFramework() {
    return null;
  }

  @Override
  public ConnectionStateErrorPolicy getConnectionStateErrorPolicy() {
    return null;
  }

  @Override
  public QuorumVerifier getCurrentConfig() {
    return null;
  }

  @Override
  public SchemaSet getSchemaSet() {
    return null;
  }

  @Override
  public boolean isZk34CompatibilityMode() {
    return false;
  }

  @Override
  public CompletableFuture<Void> runSafe(Runnable runnable) {
    return null;
  }
}
