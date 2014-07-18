package com.hubspot.singularity.scheduler;

import org.apache.curator.RetryPolicy;
import org.apache.curator.RetrySleeper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.test.TestingServer;
import org.apache.mesos.Protos.Status;
import org.apache.mesos.SchedulerDriver;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityServiceModule;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.SMTPConfiguration;
import com.hubspot.singularity.config.SentryConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.hooks.LoadBalancerClient;
import com.hubspot.singularity.mesos.SingularityLogSupport;
import com.hubspot.singularity.smtp.SingularityMailer;

import net.kencochrane.raven.Raven;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SingularityTestModule extends AbstractModule {

  @Override
  protected void configure() {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
    rootLogger.setLevel(Level.WARN);
    context.getLogger("com.hubspot").setLevel(Level.TRACE);
    
    
    try {
      TestingServer ts = new TestingServer();
    
      bind(TestingServer.class).toInstance(ts);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
    
    SingularityConfiguration config = new SingularityConfiguration();
    config.setLoadBalancerUri("test");
    
    bind(SingularityConfiguration.class).toInstance(config);
    bind(SMTPConfiguration.class).toInstance(new SMTPConfiguration());
    
    MesosConfiguration mc = new MesosConfiguration();
    mc.setDefaultCpus(1);
    mc.setDefaultMemory(128);
    
    bind(MesosConfiguration.class).toInstance(mc);
    
    SingularityAbort abort = mock(SingularityAbort.class);
    SingularityMailer mailer = mock(SingularityMailer.class);
    SchedulerDriver driver = mock(SchedulerDriver.class);
    SingularityLogSupport logSupport = mock(SingularityLogSupport.class);
    
    when(driver.killTask(null)).thenReturn(Status.DRIVER_RUNNING);
    
    bind(SingularityLogSupport.class).toInstance(logSupport);
    bind(SchedulerDriver.class).toInstance(driver);
    bind(SingularityMailer.class).toInstance(mailer);
    bind(SingularityAbort.class).toInstance(abort);
    
    TestingLoadBalancerClient tlbc = new TestingLoadBalancerClient();
    
    bind(LoadBalancerClient.class).toInstance(tlbc);
    bind(TestingLoadBalancerClient.class).toInstance(tlbc);
    
    bind(SingularityHealthchecker.class).in(Scopes.SINGLETON);
    bind(SingularityNewTaskChecker.class).in(Scopes.SINGLETON);
  }
  
  @Singleton
  @Provides
  @Named(SingularityServiceModule.UNDERLYING_CURATOR)
  public CuratorFramework provideUnderlyingCurator(CuratorFramework cf) {
    return cf;
  }
  
  @Provides
  @Singleton
  public ObjectMapper getObjectMapper() {
    return SingularityServiceModule.OBJECT_MAPPER;
  }
  
  @Singleton
  @Provides
  public CuratorFramework provideNamespaceCurator(TestingServer ts) {
    CuratorFramework cf= CuratorFrameworkFactory.newClient(ts.getConnectString(), new RetryPolicy() {
      
      @Override
      public boolean allowRetry(int retryCount, long elapsedTimeMs, RetrySleeper sleeper) {
        return false;
      }
    });
    cf.start();
    return cf;
  }
  
  @Provides
  @Singleton
  public Optional<Raven> providesNoRaven() {
    return Optional.<Raven> absent();
  }

  @Provides
  @Singleton
  public Optional<SentryConfiguration> providesNoSentryConfiguration() {
    return Optional.absent();
  }
}
