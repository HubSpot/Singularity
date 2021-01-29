package com.hubspot.singularity.data;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingCuratorFramework implements CuratorFramework {
  private final CuratorFramework curator;

  private final ConcurrentMap<String, Integer> counters;

  private final Long interval = 60000L * 3; // 3 minutes

  private static final Logger LOG = LoggerFactory.getLogger(
    LoggingCuratorFramework.class
  );

  private class ClearCounter extends TimerTask {

    @Override
    public void run() {
      logAndClear();
    }
  }

  @Inject
  public LoggingCuratorFramework(CuratorFramework curator) {
    this.curator = curator;
    counters = new ConcurrentHashMap<>();
    Timer timer = new Timer();

    timer.schedule(new ClearCounter(), interval, interval);
  }

  private String getCaller() {
    StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
    LOG.info("stackTraceElements: {}", Arrays.toString(stackTraceElements));
    long threadId = Thread.currentThread().getId();

    int levelInStack = 0;
    String className = "";
    for (int i = 0; i < stackTraceElements.length; i++) {
      String longClassName = stackTraceElements[levelInStack].getClassName();
      className = longClassName.substring(longClassName.lastIndexOf(".") + 1);

      if (
        !className.equals("Thread") ||
        !className.equals("LoggingCuratorFramework") ||
        !className.equals("CuratorManager") ||
        !className.equals("CuratorAsyncManager")
      ) {
        levelInStack = i;
        break;
      }
    }

    return (
      threadId +
      ": " +
      className +
      "\'s " +
      stackTraceElements[levelInStack].getMethodName()
    );
  }

  private void setCounter() {
    String caller = getCaller();
    int counter = counters.getOrDefault(caller, 0);
    counters.put(caller, ++counter);
  }

  public void logAndClear() {
    counters.forEach(
      (caller, count) -> {
        LOG.info("{} called ZK {} times in {} milliseconds", caller, count, interval);

        counters.remove(caller);
      }
    );
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
