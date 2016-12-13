package com.hubspot.singularity.scheduler;

import static com.google.inject.name.Names.named;
import static com.hubspot.singularity.SingularityMainModule.HTTP_HOST_AND_PORT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.curator.test.TestingServer;
import org.apache.mesos.Protos.MasterInfo;
import org.apache.mesos.Protos.Status;
import org.apache.mesos.SchedulerDriver;
import org.mockito.Matchers;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
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
import com.google.inject.util.Modules;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import com.hubspot.mesos.client.SingularityMesosClientModule;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityAuthModule;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTestAuthenticator;
import com.hubspot.singularity.auth.authenticator.SingularityAuthenticator;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.SMTPConfiguration;
import com.hubspot.singularity.config.SentryConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.ZooKeeperConfiguration;
import com.hubspot.singularity.data.SingularityDataModule;
import com.hubspot.singularity.data.history.SingularityHistoryModule;
import com.hubspot.singularity.data.transcoders.SingularityTranscoderModule;
import com.hubspot.singularity.data.zkmigrations.SingularityZkMigrationsModule;
import com.hubspot.singularity.event.SingularityEventModule;
import com.hubspot.singularity.hooks.LoadBalancerClient;
import com.hubspot.singularity.mesos.SchedulerDriverSupplier;
import com.hubspot.singularity.mesos.SingularityDriver;
import com.hubspot.singularity.mesos.SingularityMesosExecutorInfoSupport;
import com.hubspot.singularity.mesos.SingularityMesosModule;
import com.hubspot.singularity.resources.DeployResource;
import com.hubspot.singularity.resources.PriorityResource;
import com.hubspot.singularity.resources.RackResource;
import com.hubspot.singularity.resources.RequestResource;
import com.hubspot.singularity.resources.SlaveResource;
import com.hubspot.singularity.resources.TaskResource;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;
import com.hubspot.singularity.smtp.SingularityMailer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;
import net.kencochrane.raven.Raven;

public class SingularityTestModule implements Module {
  private final TestingServer ts;
  private final GuiceBundle.DropwizardModule dropwizardModule;

  private final boolean useDBTests;

  public SingularityTestModule(boolean useDbTests) throws Exception {
    this.useDBTests = useDbTests;

    dropwizardModule = new GuiceBundle.DropwizardModule();

    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    Logger rootLogger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    rootLogger.setLevel(Level.toLevel(System.getProperty("singularity.test.log.level", "ERROR")));

    Logger hsLogger = context.getLogger("com.hubspot");
    hsLogger.setLevel(Level.toLevel(System.getProperty("singularity.test.log.level.for.com.hubspot", "ERROR")));

    this.ts = new TestingServer();
  }

  public Injector getInjector() throws Exception {
    return Guice.createInjector(Stage.PRODUCTION, dropwizardModule, this);
  }

  public void start() throws Exception {
    // Start all the managed instances in dropwizard.
    Set<Managed> managedObjects = ImmutableSet.copyOf(dropwizardModule.getManaged());
    for (Managed managed : managedObjects) {
      managed.start();
    }
  }

  public void stop() throws Exception {
    ImmutableSet<Managed> managedObjects = ImmutableSet.copyOf(dropwizardModule.getManaged());
    for (Managed managed : Lists.reverse(managedObjects.asList())) {
      managed.stop();
    }
  }

  @Override
  public void configure(Binder mainBinder) {

    mainBinder.install(new GuiceBundle.GuiceEnforcerModule());

    mainBinder.bind(TestingServer.class).toInstance(ts);
    final SingularityConfiguration configuration = getSingularityConfigurationForTestingServer(ts);

    if (useDBTests) {
      configuration.setDatabaseConfiguration(getDataSourceFactory());
    }

    mainBinder.bind(SingularityConfiguration.class).toInstance(configuration);

    mainBinder.install(Modules.override(new SingularityMainModule(configuration))
        .with(new Module() {

          @Override
          public void configure(Binder binder) {
            binder.bind(SingularityExceptionNotifier.class).toInstance(mock(SingularityExceptionNotifier.class));

            SingularityAbort abort = mock(SingularityAbort.class);
            SingularityMailer mailer = mock(SingularityMailer.class);

            binder.bind(SingularityMailer.class).toInstance(mailer);
            binder.bind(SingularityAbort.class).toInstance(abort);

            TestingLoadBalancerClient tlbc = new TestingLoadBalancerClient();
            binder.bind(LoadBalancerClient.class).toInstance(tlbc);
            binder.bind(TestingLoadBalancerClient.class).toInstance(tlbc);

            ObjectMapper om = Jackson.newObjectMapper()
              .setSerializationInclusion(Include.NON_NULL)
              .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
              .registerModule(new ProtobufModule());

            binder.bind(ObjectMapper.class).toInstance(om);

            Environment environment = new Environment("test-env", om, null, new MetricRegistry(), null);
            binder.bind(Environment.class).toInstance(environment);

            binder.bind(HostAndPort.class).annotatedWith(named(HTTP_HOST_AND_PORT)).toInstance(HostAndPort.fromString("localhost:8080"));

            binder.bind(new TypeLiteral<Optional<Raven>>() {}).toInstance(Optional.<Raven>absent());
            binder.bind(new TypeLiteral<Optional<SentryConfiguration>>() {}).toInstance(Optional.<SentryConfiguration>absent());

            binder.bind(HttpServletRequest.class).toProvider(new Provider<HttpServletRequest>() {
              @Override
              public HttpServletRequest get() {
                throw new OutOfScopeException("testing");
              }
            });
          }
        }));

    mainBinder.install(Modules.override(new SingularityMesosModule())
        .with(new Module() {

          @Override
          public void configure(Binder binder) {
            SingularityMesosExecutorInfoSupport logSupport = mock(SingularityMesosExecutorInfoSupport.class);
            binder.bind(SingularityMesosExecutorInfoSupport.class).toInstance(logSupport);

            SingularityDriver mock = mock(SingularityDriver.class);
            when(mock.kill((SingularityTaskId) Matchers.any())).thenReturn(Status.DRIVER_RUNNING);
            when(mock.getMaster()).thenReturn(Optional.<MasterInfo>absent());
            when(mock.start()).thenReturn(Status.DRIVER_RUNNING);
            when(mock.getLastOfferTimestamp()).thenReturn(Optional.<Long>absent());
            binder.bind(SingularityDriver.class).toInstance(mock);

            SchedulerDriver driver = mock(SchedulerDriver.class);

            when(driver.killTask(null)).thenReturn(Status.DRIVER_RUNNING);

            SchedulerDriverSupplier driverSupplier = new SchedulerDriverSupplier();
            driverSupplier.setSchedulerDriver(driver);

            binder.bind(SchedulerDriverSupplier.class).toInstance(driverSupplier);
          }
        }));

    mainBinder.install(new SingularityDataModule());
    mainBinder.install(new SingularitySchedulerModule());
    mainBinder.install(new SingularityTranscoderModule());
    mainBinder.install(new SingularityHistoryModule(configuration));
    mainBinder.install(new SingularityZkMigrationsModule());
    mainBinder.install(new SingularityMesosClientModule());
    mainBinder.install(new SingularityEventModule(configuration));
    mainBinder.install(Modules.override(new SingularityAuthModule(configuration))
            .with(new Module() {
              @Override
              public void configure(Binder binder) {
                binder.bind(SingularityAuthenticator.class).to(SingularityTestAuthenticator.class);
                binder.bind(SingularityTestAuthenticator.class).in(Scopes.SINGLETON);
              }
            }));

    mainBinder.bind(DeployResource.class);
    mainBinder.bind(RequestResource.class);
    mainBinder.bind(TaskResource.class);
    mainBinder.bind(SlaveResource.class);
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

  private static SingularityConfiguration getSingularityConfigurationForTestingServer(final TestingServer ts) {
    SingularityConfiguration config = new SingularityConfiguration();
    config.setLoadBalancerUri("test");

    MesosConfiguration mc = new MesosConfiguration();
    mc.setDefaultCpus(1);
    mc.setDefaultMemory(128);
    config.setMesosConfiguration(mc);

    config.setSmtpConfiguration(new SMTPConfiguration());

    ZooKeeperConfiguration zookeeperConfiguration = new ZooKeeperConfiguration();
    zookeeperConfiguration.setQuorum(ts.getConnectString());

    config.setZooKeeperConfiguration(zookeeperConfiguration);
    config.setConsiderTaskHealthyAfterRunningForSeconds(0);

    return config;
  }

}
