package com.hubspot.singularity;

import java.util.Set;
import java.util.function.Function;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.test.TestingServer;
import org.apache.curator.utils.ZKPaths;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.ZkCache;
import com.hubspot.singularity.helpers.MesosProtosUtils;
import com.hubspot.singularity.mesos.OfferCache;
import com.hubspot.singularity.scheduler.SingularityLeaderCache;
import com.hubspot.singularity.scheduler.SingularityLeaderCacheCoordinator;
import com.hubspot.singularity.scheduler.SingularityTestModule;
import com.squarespace.jersey2.guice.JerseyGuiceUtils;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.FileSystemResourceAccessor;

@Timeout(value = 60)
@TestInstance(Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
public class SingularityCuratorTestBase {

  @Inject
  protected Provider<Jdbi> dbiProvider;
  @Inject
  protected CuratorFramework cf;
  @Inject
  protected TestingServer ts;
  @Inject
  private SingularityLeaderCache cache;
  @Inject
  private SingularityLeaderCacheCoordinator leaderCacheCoordinator;
  @Inject
  private ZkCache<SingularityDeploy> deployZkCache;
  @Inject
  private ZkCache<SingularityTask> taskZkCache;
  @Inject
  private OfferCache offerCache;
  @Inject
  protected MesosProtosUtils mesosProtosUtils;

  private SingularityTestModule singularityTestModule;

  private final boolean useDBTests;
  private final Function<SingularityConfiguration, Void> customConfigSetup;

  private static final Set<String> DELETE_NODES = ImmutableSet.of("requests", "deploys", "hosts", "metadata", "racks", "slaves");

  @AfterEach
  public void clearData() {
    try {
      // clean up most data
      for (String parent : cf.getChildren().forPath("/")) {
        if (DELETE_NODES.contains(parent)) {
          cf.delete().deletingChildrenIfNeeded().forPath("/" + parent);
        }
      }

      // clean task data
      for (String child : cf.getChildren().forPath("/tasks")) {
        if (child.equals("statuses") || child.equals("history")) {
          for (String node : cf.getChildren().forPath(ZKPaths.makePath("/tasks", child))) {
            cf.delete().deletingChildrenIfNeeded().forPath(ZKPaths.makePath("/tasks", child, node));
          }
        } else {
          cf.delete().deletingChildrenIfNeeded().forPath(ZKPaths.makePath("/tasks", child));
        }
      }

      // clear from caches
      cache.clear();
      deployZkCache.invalidateAll();
      taskZkCache.invalidateAll();
      offerCache.peekOffers().forEach((o) -> offerCache.rescindOffer(o.getId()));

      // clear db
      if (useDBTests) {
        Handle handle = dbiProvider.get().open();
        handle.execute("DELETE FROM taskHistory;DELETE FROM requestHistory;DELETE FROM deployHistory;");
        handle.close();
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @BeforeAll
  public void setup() throws Exception {
    JerseyGuiceUtils.reset();
    singularityTestModule = new SingularityTestModule(useDBTests, customConfigSetup);

    singularityTestModule.getInjector().injectMembers(this);
    singularityTestModule.start();
    leaderCacheCoordinator.activateLeaderCache();
    if (useDBTests) {
      Handle handle = dbiProvider.get().open();
      handle.getConnection().setAutoCommit(true);

      Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(handle.getConnection()));

      Liquibase liquibase = new Liquibase("singularity_test.sql", new FileSystemResourceAccessor(), database);
      liquibase.update((String) null);

      try {
        database.close();
        handle.close();
      } catch (Throwable t) {
      }
    }
  }

  public SingularityCuratorTestBase(boolean useDBTests) {
    this(useDBTests, null);
  }

  public SingularityCuratorTestBase(boolean useDBTests, Function<SingularityConfiguration, Void> customConfigSetup) {
    this.useDBTests = useDBTests;
    this.customConfigSetup = customConfigSetup;
  }

  @AfterAll
  public void teardown() throws Exception {
    if (singularityTestModule != null) {
      singularityTestModule.stop();
    }

    if (cf != null) {
      cf.close();
    }

    if (ts != null) {
      ts.close();
    }

  }


}
