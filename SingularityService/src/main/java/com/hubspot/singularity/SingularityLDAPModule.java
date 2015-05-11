package com.hubspot.singularity;

import java.io.IOException;

import org.apache.directory.ldap.client.api.DefaultPoolableLdapConnectionFactory;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapConnectionPool;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.servlet.ServletScopes;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.ldap.SingularityAuthManager;
import com.hubspot.singularity.ldap.SingularityLDAPManager;

public class SingularityLDAPModule implements Module {
  public static final String LDAP_GROUP_CACHE = "ldap.group.cache";

  private final SingularityConfiguration configuration;

  public SingularityLDAPModule(SingularityConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public void configure(Binder binder) {
    binder.bind(SingularityLDAPManager.class);
    binder.bind(SingularityAuthManager.class).in(ServletScopes.REQUEST);
  }

  @Provides
  @Singleton
  public LdapConnectionPool providePool() throws IOException {
    final LdapConnectionConfig config = new LdapConnectionConfig();
    config.setLdapHost(configuration.getLdapConfiguration().getHostname());
    config.setLdapPort(configuration.getLdapConfiguration().getPort());
    config.setName(configuration.getLdapConfiguration().getBindDn());
    config.setCredentials(configuration.getLdapConfiguration().getBindPassword());

    final DefaultPoolableLdapConnectionFactory factory = new DefaultPoolableLdapConnectionFactory(config);

    final LdapConnectionPool pool = new LdapConnectionPool(factory);
    pool.setTestOnBorrow(configuration.getLdapConfiguration().isPoolTestOnBorrow());
    pool.setTestOnReturn(configuration.getLdapConfiguration().isPoolTestOnReturn());
    pool.setTestWhileIdle(configuration.getLdapConfiguration().isPoolTestWhileIdle());

    switch (configuration.getLdapConfiguration().getPoolWhenExhaustedAction()) {
      case BLOCK:
        pool.setWhenExhaustedAction(LdapConnectionPool.WHEN_EXHAUSTED_BLOCK);
        break;
      case FAIL:
        pool.setWhenExhaustedAction(LdapConnectionPool.WHEN_EXHAUSTED_FAIL);
        break;
      case GROW:
        pool.setWhenExhaustedAction(LdapConnectionPool.WHEN_EXHAUSTED_GROW);
        break;
      default:
        pool.setWhenExhaustedAction(LdapConnectionPool.DEFAULT_WHEN_EXHAUSTED_ACTION);
    }

    return pool;
  }
}
