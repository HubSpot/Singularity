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
    return distributor.getCuratorFramework().getState();
  }

  @Override
  public boolean isStarted() {
    return false;
  }

  @Override
  public CreateBuilder create() {
    return distributor.getCuratorFramework().create();
  }

  @Override
  public DeleteBuilder delete() {
    return distributor.getCuratorFramework().delete();
  }

  @Override
  public ExistsBuilder checkExists() {
    return distributor.getCuratorFramework().checkExists();
  }

  @Override
  public GetDataBuilder getData() {
    return distributor.getCuratorFramework().getData();
  }

  @Override
  public SetDataBuilder setData() {
    return distributor.getCuratorFramework().setData();
  }

  @Override
  public GetChildrenBuilder getChildren() {
    return distributor.getCuratorFramework().getChildren();
  }

  @Override
  public GetACLBuilder getACL() {
    return distributor.getCuratorFramework().getACL();
  }

  @Override
  public SetACLBuilder setACL() {
    return distributor.getCuratorFramework().setACL();
  }

  @Override
  public ReconfigBuilder reconfig() {
    return distributor.getCuratorFramework().reconfig();
  }

  @Override
  public GetConfigBuilder getConfig() {
    return distributor.getCuratorFramework().getConfig();
  }

  @Override
  public CuratorTransaction inTransaction() {
    return distributor.getCuratorFramework().inTransaction();
  }

  @Override
  public CuratorMultiTransaction transaction() {
    return distributor.getCuratorFramework().transaction();
  }

  @Override
  public TransactionOp transactionOp() {
    return distributor.getCuratorFramework().transactionOp();
  }

  @Override
  public void sync(String path, Object backgroundContextObject) {
    distributor.getCuratorFramework().sync(path, backgroundContextObject);
  }

  @Override
  public void createContainers(String path) throws Exception {
    distributor.getCuratorFramework().createContainers(path);
  }

  @Override
  public SyncBuilder sync() {
    return distributor.getCuratorFramework().sync();
  }

  @Override
  public RemoveWatchesBuilder watches() {
    return distributor.getCuratorFramework().watches();
  }

  @Override
  public Listenable<ConnectionStateListener> getConnectionStateListenable() {
    return distributor.getCuratorFramework().getConnectionStateListenable();
  }

  @Override
  public Listenable<CuratorListener> getCuratorListenable() {
    return distributor.getCuratorFramework().getCuratorListenable();
  }

  @Override
  public Listenable<UnhandledErrorListener> getUnhandledErrorListenable() {
    return distributor.getCuratorFramework().getUnhandledErrorListenable();
  }

  @Override
  public CuratorFramework nonNamespaceView() {
    return distributor.getCuratorFramework().nonNamespaceView();
  }

  @Override
  public CuratorFramework usingNamespace(String newNamespace) {
    return distributor.getCuratorFramework().usingNamespace(newNamespace);
  }

  @Override
  public String getNamespace() {
    return distributor.getCuratorFramework().getNamespace();
  }

  @Override
  public CuratorZookeeperClient getZookeeperClient() {
    return distributor.getCuratorFramework().getZookeeperClient();
  }

  @Override
  public EnsurePath newNamespaceAwareEnsurePath(String path) {
    return distributor.getCuratorFramework().newNamespaceAwareEnsurePath(path);
  }

  @Override
  public void clearWatcherReferences(Watcher watcher) {
    distributor.getCuratorFramework().clearWatcherReferences(watcher);
  }

  @Override
  public boolean blockUntilConnected(int maxWaitTime, TimeUnit units)
    throws InterruptedException {
    return false;
  }

  @Override
  public void blockUntilConnected() throws InterruptedException {}

  @Override
  public WatcherRemoveCuratorFramework newWatcherRemoveCuratorFramework() {
    return distributor.getCuratorFramework().newWatcherRemoveCuratorFramework();
  }

  @Override
  public ConnectionStateErrorPolicy getConnectionStateErrorPolicy() {
    return distributor.getCuratorFramework().getConnectionStateErrorPolicy();
  }

  @Override
  public QuorumVerifier getCurrentConfig() {
    return distributor.getCuratorFramework().getCurrentConfig();
  }

  @Override
  public SchemaSet getSchemaSet() {
    return distributor.getCuratorFramework().getSchemaSet();
  }

  @Override
  public boolean isZk34CompatibilityMode() {
    return distributor.getCuratorFramework().isZk34CompatibilityMode();
  }

  @Override
  public CompletableFuture<Void> runSafe(Runnable runnable) {
    return distributor.getCuratorFramework().runSafe(runnable);
  }
}
