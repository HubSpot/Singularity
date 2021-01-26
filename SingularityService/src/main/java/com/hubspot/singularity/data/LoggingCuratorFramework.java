package com.hubspot.singularity.data;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingCuratorFramework implements CuratorFramework {
  private final CuratorFramework curator;
  private final Map<String, AtomicLong> counters;

  private static final Logger LOG = LoggerFactory.getLogger(
    LoggingCuratorFramework.class
  );

  @Inject
  public LoggingCuratorFramework(CuratorFramework curator) {
    this.curator = curator;
    this.counters = new HashMap<>();
  }

  private String getCaller() {
    StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

    if (
      stackTraceElements[1].getClassName()
        .equals("com.hubspot.singularity.data.CuratorManager") ||
      stackTraceElements[1].getClassName()
        .equals("com.hubspot.singularity.data.CuratorAsyncManager")
    ) {
      return (
        stackTraceElements[2].getClassName() +
        "\'s " +
        stackTraceElements[2].getMethodName()
      );
    } else {
      return (
        stackTraceElements[1].getClassName() +
        "\'s " +
        stackTraceElements[1].getMethodName()
      );
    }
  }

  private void setCounter() {
    String caller = getCaller();
    AtomicLong counter = this.counters.containsKey(caller)
      ? this.counters.get(caller)
      : new AtomicLong(0);
    counter.getAndIncrement();
    this.counters.put(caller, counter);
  }

  public void clear() {
    String caller = getCaller();
    AtomicLong counter = this.counters.containsKey(caller)
      ? this.counters.get(caller)
      : new AtomicLong(0);

    LOG.info("{} called ZK {} times", caller, counter.get());

    this.counters.remove(caller);
  }

  @Override
  public void start() {
    curator.start();
  }

  @Override
  public void close() {
    curator.close();
  }

  @Override
  public CuratorFrameworkState getState() {
    setCounter();
    return curator.getState();
  }

  @Override
  public boolean isStarted() {
    return curator.isStarted();
  }

  @Override
  public CreateBuilder create() {
    return curator.create();
  }

  @Override
  public DeleteBuilder delete() {
    setCounter();
    return curator.delete();
  }

  @Override
  public ExistsBuilder checkExists() {
    setCounter();
    return curator.checkExists();
  }

  @Override
  public GetDataBuilder getData() {
    setCounter();
    return curator.getData();
  }

  @Override
  public SetDataBuilder setData() {
    return curator.setData();
  }

  @Override
  public GetChildrenBuilder getChildren() {
    setCounter();
    return curator.getChildren();
  }

  @Override
  public GetACLBuilder getACL() {
    return curator.getACL();
  }

  @Override
  public SetACLBuilder setACL() {
    return curator.setACL();
  }

  @Override
  public ReconfigBuilder reconfig() {
    return curator.reconfig();
  }

  @Override
  public GetConfigBuilder getConfig() {
    return curator.getConfig();
  }

  @Override
  public CuratorTransaction inTransaction() {
    return curator.inTransaction();
  }

  @Override
  public CuratorMultiTransaction transaction() {
    return curator.transaction();
  }

  @Override
  public TransactionOp transactionOp() {
    return curator.transactionOp();
  }

  @Override
  public void sync(String s, Object o) {
    curator.sync(s, o);
  }

  @Override
  public void createContainers(String s) throws Exception {
    curator.createContainers(s);
  }

  @Override
  public SyncBuilder sync() {
    return curator.sync();
  }

  @Override
  public RemoveWatchesBuilder watches() {
    return curator.watches();
  }

  @Override
  public Listenable<ConnectionStateListener> getConnectionStateListenable() {
    return curator.getConnectionStateListenable();
  }

  @Override
  public Listenable<CuratorListener> getCuratorListenable() {
    return curator.getCuratorListenable();
  }

  @Override
  public Listenable<UnhandledErrorListener> getUnhandledErrorListenable() {
    return curator.getUnhandledErrorListenable();
  }

  @Override
  public CuratorFramework nonNamespaceView() {
    return curator.nonNamespaceView();
  }

  @Override
  public CuratorFramework usingNamespace(String s) {
    return curator.usingNamespace(s);
  }

  @Override
  public String getNamespace() {
    return curator.getNamespace();
  }

  @Override
  public CuratorZookeeperClient getZookeeperClient() {
    return curator.getZookeeperClient();
  }

  @Override
  public EnsurePath newNamespaceAwareEnsurePath(String s) {
    return curator.newNamespaceAwareEnsurePath(s);
  }

  @Override
  public void clearWatcherReferences(Watcher watcher) {
    curator.clearWatcherReferences(watcher);
  }

  @Override
  public boolean blockUntilConnected(int i, TimeUnit timeUnit)
    throws InterruptedException {
    return curator.blockUntilConnected(i, timeUnit);
  }

  @Override
  public void blockUntilConnected() throws InterruptedException {
    curator.blockUntilConnected();
  }

  @Override
  public WatcherRemoveCuratorFramework newWatcherRemoveCuratorFramework() {
    return curator.newWatcherRemoveCuratorFramework();
  }

  @Override
  public ConnectionStateErrorPolicy getConnectionStateErrorPolicy() {
    return curator.getConnectionStateErrorPolicy();
  }

  @Override
  public QuorumVerifier getCurrentConfig() {
    return curator.getCurrentConfig();
  }

  @Override
  public SchemaSet getSchemaSet() {
    return curator.getSchemaSet();
  }

  @Override
  public boolean isZk34CompatibilityMode() {
    return curator.isZk34CompatibilityMode();
  }

  @Override
  public CompletableFuture<Void> runSafe(Runnable runnable) {
    return curator.runSafe(runnable);
  }
}
