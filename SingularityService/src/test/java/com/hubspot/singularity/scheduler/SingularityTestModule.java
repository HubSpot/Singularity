package com.hubspot.singularity.scheduler;

import static com.google.inject.name.Names.named;
import static com.hubspot.singularity.SingularityMainModule.HTTP_HOST_AND_PORT;
import static org.mockito.Mockito.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.net.HostAndPort;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.util.Modules;
import com.hubspot.dropwizard.guicier.DropwizardModule;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.client.MesosClient;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityLeaderController;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.SingularityTestAuthenticator;
import com.hubspot.singularity.auth.SingularityAuthorizer;
import com.hubspot.singularity.auth.SingularityGroupsAuthorizer;
import com.hubspot.singularity.auth.authenticator.SingularityAuthenticator;
import com.hubspot.singularity.auth.datastore.SingularityAuthDatastore;
import com.hubspot.singularity.auth.datastore.SingularityDisabledAuthDatastore;
import com.hubspot.singularity.config.AuthConfiguration;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.SMTPConfiguration;
import com.hubspot.singularity.config.SentryConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.ZooKeeperConfiguration;
import com.hubspot.singularity.data.SingularityDataModule;
import com.hubspot.singularity.data.history.SingularityDbModule;
import com.hubspot.singularity.data.history.SingularityHistoryModule;
import com.hubspot.singularity.data.transcoders.SingularityTranscoderModule;
import com.hubspot.singularity.data.zkmigrations.SingularityZkMigrationsModule;
import com.hubspot.singularity.event.SingularityEventModule;
import com.hubspot.singularity.hooks.DeployAcceptanceHook;
import com.hubspot.singularity.hooks.LoadBalancerClient;
import com.hubspot.singularity.managed.SingularityLifecycleManaged;
import com.hubspot.singularity.managed.SingularityLifecycleManagedTest;
import com.hubspot.singularity.mesos.SingularityMesosExecutorInfoSupport;
import com.hubspot.singularity.mesos.SingularityMesosModule;
import com.hubspot.singularity.mesos.SingularityMesosOfferManager;
import com.hubspot.singularity.mesos.SingularityMesosScheduler;
import com.hubspot.singularity.mesos.SingularityMesosSchedulerClient;
import com.hubspot.singularity.mesos.TestMesosSchedulerImpl;
import com.hubspot.singularity.resources.AgentResource;
import com.hubspot.singularity.resources.DeployResource;
import com.hubspot.singularity.resources.PriorityResource;
import com.hubspot.singularity.resources.RackResource;
import com.hubspot.singularity.resources.RequestResource;
import com.hubspot.singularity.resources.TaskResource;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;
import com.hubspot.singularity.smtp.SingularityMailer;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.setup.Environment;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import javax.servlet.http.HttpServletRequest;
import net.kencochrane.raven.Raven;
import org.apache.curator.test.TestingServer;
import org.eclipse.jetty.util.component.LifeCycle;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.LoggerFactory;

public class SingularityTestModule implements Module {
  private final TestingServer ts;
  private final DropwizardModule dropwizardModule;
  private final ObjectMapper om = JavaUtils.newObjectMapper();
  private final Environment environment = new Environment(
    "test-env",
    om,
    null,
    new MetricRegistry(),
    null
  );

  private final boolean useDBTests;
  private final Function<SingularityConfiguration, Void> customConfigSetup;

  public SingularityTestModule(
    boolean useDbTests,
    Function<SingularityConfiguration, Void> customConfigSetup
  )
    throws Exception {
    this.useDBTests = useDbTests;
    this.customConfigSetup = customConfigSetup;

    dropwizardModule = new DropwizardModule(environment);

    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    Logger rootLogger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    rootLogger.setLevel(
      Level.toLevel(System.getProperty("singularity.test.log.level", "WARN"))
    );

    Logger hsLogger = context.getLogger("com.hubspot");
    hsLogger.setLevel(
      Level.toLevel(
        System.getProperty("singularity.test.log.level.for.com.hubspot", "WARN")
      )
    );

    this.ts = new TestingServer();
  }

  public Injector getInjector() throws Exception {
    return Guice.createInjector(Stage.PRODUCTION, dropwizardModule, this);
  }

  public void start() throws Exception {
    // Start all the managed instances in dropwizard.
    Set<LifeCycle> managedObjects = ImmutableSet.copyOf(
      environment.lifecycle().getManagedObjects()
    );
    for (LifeCycle managed : managedObjects) {
      managed.start();
    }
  }

  public void stop() throws Exception {
    ImmutableSet<LifeCycle> managedObjects = ImmutableSet.copyOf(
      environment.lifecycle().getManagedObjects()
    );
    for (LifeCycle managed : Lists.reverse(managedObjects.asList())) {
      managed.stop();
    }
  }

  @Override
  public void configure(Binder mainBinder) {
    mainBinder.install(new GuiceBundle.GuiceEnforcerModule());

    TestingMesosClient tmc = new TestingMesosClient();
    mainBinder.bind(MesosClient.class).toInstance(tmc);
    mainBinder.bind(TestingMesosClient.class).toInstance(tmc);

    mainBinder.bind(TestingServer.class).toInstance(ts);
    final SingularityConfiguration configuration = getSingularityConfigurationForTestingServer(
      ts
    );
    configuration.getMesosConfiguration().setMaster("");

    if (useDBTests) {
      configuration.setDatabaseConfiguration(getDataSourceFactory());
    } else {
      mainBinder.bind(Jdbi.class).toProvider(() -> null);
    }

    if (customConfigSetup != null) {
      customConfigSetup.apply(configuration);
    }

    mainBinder.bind(SingularityConfiguration.class).toInstance(configuration);

    mainBinder.install(
      Modules
        .override(
          new SingularityMainModule(configuration, TestingLoadBalancerClient.class)
        )
        .with(
          new Module() {

            @Override
            public void configure(Binder binder) {
              binder
                .bind(SingularityExceptionNotifier.class)
                .toInstance(mock(SingularityExceptionNotifier.class));

              SingularityAbort abort = mock(SingularityAbort.class);
              SingularityMailer mailer = mock(SingularityMailer.class);

              binder.bind(SingularityMailer.class).toInstance(mailer);
              binder.bind(SingularityAbort.class).toInstance(abort);

              TestingLoadBalancerClient tlbc = new TestingLoadBalancerClient(
                configuration,
                om
              );
              binder.bind(LoadBalancerClient.class).toInstance(tlbc);
              binder.bind(TestingLoadBalancerClient.class).toInstance(tlbc);

              binder.bind(ObjectMapper.class).toInstance(om);
              binder.bind(Environment.class).toInstance(environment);
              binder
                .bind(HostAndPort.class)
                .annotatedWith(named(HTTP_HOST_AND_PORT))
                .toInstance(HostAndPort.fromString("localhost:8080"));
              binder
                .bind(SingularityLifecycleManaged.class)
                .to(SingularityLifecycleManagedTest.class)
                .asEagerSingleton();

              binder
                .bind(new TypeLiteral<Optional<Raven>>() {})
                .toInstance(Optional.<Raven>empty());
              binder
                .bind(new TypeLiteral<Optional<SentryConfiguration>>() {})
                .toInstance(Optional.<SentryConfiguration>empty());

              binder
                .bind(HttpServletRequest.class)
                .toProvider(
                  new Provider<HttpServletRequest>() {

                    @Override
                    public HttpServletRequest get() {
                      throw new OutOfScopeException("testing");
                    }
                  }
                );

              binder
                .bind(SingularityLeaderController.class)
                .to(SingularityTestLeaderController.class)
                .in(Scopes.SINGLETON);
            }
          }
        )
    );

    mainBinder.install(
      Modules
        .override(new SingularityMesosModule())
        .with(
          binder -> {
            SingularityMesosExecutorInfoSupport logSupport = mock(
              SingularityMesosExecutorInfoSupport.class
            );
            binder.bind(SingularityMesosExecutorInfoSupport.class).toInstance(logSupport);

            SingularityMesosSchedulerClient mockClient = mock(
              SingularityMesosSchedulerClient.class
            );
            when(mockClient.isRunning()).thenReturn(true);
            binder.bind(SingularityMesosSchedulerClient.class).toInstance(mockClient);
            binder.bind(SingularityMesosScheduler.class).to(TestMesosSchedulerImpl.class);
            Multibinder
              .newSetBinder(binder, DeployAcceptanceHook.class)
              .addBinding()
              .to(NoopDeployAcceptanceHook.class)
              .in(Scopes.SINGLETON);
          }
        )
    );

    mainBinder.install(new SingularityDataModule(configuration));
    mainBinder.install(new SingularitySchedulerModule());
    mainBinder.install(new SingularityTranscoderModule());
    mainBinder.install(new SingularityHistoryModule());
    mainBinder.install(new SingularityDbModule(configuration));
    mainBinder.install(new SingularityZkMigrationsModule());

    mainBinder.install(
      new SingularityEventModule(configuration.getWebhookQueueConfiguration())
    );

    // Auth module bits
    mainBinder
      .bind(SingularityAuthenticator.class)
      .to(SingularityTestAuthenticator.class);
    mainBinder
      .bind(SingularityAuthDatastore.class)
      .to(SingularityDisabledAuthDatastore.class);
    mainBinder
      .bind(SingularityAuthorizer.class)
      .to(SingularityGroupsAuthorizer.class)
      .in(Scopes.SINGLETON);
    mainBinder.bind(SingularityTestAuthenticator.class).in(Scopes.SINGLETON);
    mainBinder
      .bind(AuthConfiguration.class)
      .toInstance(configuration.getAuthConfiguration());

    mainBinder.bind(DeployResource.class);
    mainBinder.bind(RequestResource.class);
    mainBinder.bind(TaskResource.class);
    mainBinder.bind(AgentResource.class);
    mainBinder.bind(RackResource.class);
    mainBinder.bind(PriorityResource.class);
  }

  private DataSourceFactory getDataSourceFactory() {
    DataSourceFactory dataSourceFactory = new DataSourceFactory();
    dataSourceFactory.setDriverClass("org.h2.Driver");
    dataSourceFactory.setUrl("jdbc:h2:mem:singularity;DB_CLOSE_DELAY=-1");
    dataSourceFactory.setUser("user");
    dataSourceFactory.setPassword("password");

    return dataSourceFactory;
  }

  private static SingularityConfiguration getSingularityConfigurationForTestingServer(
    final TestingServer ts
  ) {
    SingularityConfiguration config = new SingularityConfiguration();
    config.setLoadBalancerUri("test");

    MesosConfiguration mc = new MesosConfiguration();
    mc.setDefaultCpus(1);
    mc.setDefaultMemory(128);
    mc.setDefaultDisk(1024);
    config.setMesosConfiguration(mc);

    config.setSmtpConfiguration(new SMTPConfiguration());

    ZooKeeperConfiguration zookeeperConfiguration = new ZooKeeperConfiguration();
    zookeeperConfiguration.setZkNamespace(
      Optional.ofNullable(System.getProperty("zkNamespace")).orElse("sy")
    );
    zookeeperConfiguration.setQuorum(ts.getConnectString());

    config.setZooKeeperConfiguration(zookeeperConfiguration);
    config.setConsiderTaskHealthyAfterRunningForSeconds(0);

    return config;
  }
}
