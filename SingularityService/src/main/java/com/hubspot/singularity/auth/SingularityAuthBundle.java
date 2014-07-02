package com.hubspot.singularity.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.yammer.dropwizard.authenticator.LdapAuthenticator;
import com.yammer.dropwizard.authenticator.LdapCanAuthenticate;
import com.yammer.dropwizard.authenticator.LdapConfiguration;
import com.yammer.dropwizard.authenticator.ResourceAuthenticator;
import com.yammer.dropwizard.authenticator.healthchecks.LdapHealthCheck;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.CachingAuthenticator;
import io.dropwizard.auth.basic.BasicAuthProvider;
import io.dropwizard.auth.basic.BasicCredentials;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class SingularityAuthBundle implements ConfiguredBundle<SingularityConfiguration> {
  private final static Logger LOG = LoggerFactory.getLogger(SingularityService.class);

  @Override
  public void initialize(Bootstrap<?> bootstrap) {

  }

  @Override
  public void run(SingularityConfiguration configuration, Environment environment) {
    if (configuration.getLdapConfiguration() != null) {
      LOG.info("Configuring LDAP authentication...");
      final LdapConfiguration ldapConfiguration = configuration.getLdapConfiguration();

      final Authenticator<BasicCredentials, SingularityUser> ldapAuthenticator = new SingularityResourceAuthenticator(new LdapAuthenticator(ldapConfiguration));
      final CachingAuthenticator<BasicCredentials, SingularityUser> cachingLdapAuthenticator = new CachingAuthenticator<BasicCredentials, SingularityUser>(environment.metrics(), ldapAuthenticator, ldapConfiguration.getCachePolicy());

      environment.jersey().register(new BasicAuthProvider<>(cachingLdapAuthenticator, "Singularity"));

      environment.healthChecks().register("ldap", new LdapHealthCheck(new ResourceAuthenticator(new LdapCanAuthenticate(ldapConfiguration))));
    } else {
      LOG.info("No authentication defined, using anonymous user {}...", SingularityUser.ANONYMOUS);
      environment.jersey().register(new SingularityAnonymousUserProvider());
    }
  }
}
