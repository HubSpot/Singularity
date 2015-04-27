package com.hubspot.singularity;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.directory.ldap.client.api.DefaultPoolableLdapConnectionFactory;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapConnectionPool;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.singularity.config.SingularityConfiguration;
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
  }

  @Provides
  @Singleton
  public LdapConnectionPool providePool() throws IOException {
    LdapConnectionConfig config = new LdapConnectionConfig();
    config.setLdapHost(configuration.getLdapConfiguration().getHostname());
    config.setLdapPort(configuration.getLdapConfiguration().getPort());
    config.setName(configuration.getLdapConfiguration().getBindDn());
    config.setCredentials(configuration.getLdapConfiguration().getBindPassword());

    DefaultPoolableLdapConnectionFactory factory = new DefaultPoolableLdapConnectionFactory(config);
    LdapConnectionPool pool = new LdapConnectionPool(factory);
    pool.setTestOnBorrow(true); // test the validity of borrowed connections on borrow, if not valid, removes the connection from the pool and sends a different connection
    pool.setTestOnReturn(true); // test the validity of a borrowed connection on connection return, toss if invalid
    pool.setTestWhileIdle(true); // periodically tests the validity of connections
    pool.setWhenExhaustedAction(LdapConnectionPool.WHEN_EXHAUSTED_BLOCK); // instead of spawning a new connection when more requests come in, block until a connection is freed

    return pool;
  }

  @Provides
  @Singleton
  @Named(LDAP_GROUP_CACHE)
  public LoadingCache<String, Set<String>> providesGroupCache(final SingularityLDAPManager ldapManager) {
    return CacheBuilder.newBuilder()
            .expireAfterWrite(configuration.getLdapConfiguration().getCacheExpirationMs(), TimeUnit.MILLISECONDS)
            .build(new CacheLoader<String, Set<String>>() {
              @Override
              public Set<String> load(String key) throws Exception {
                return ldapManager.getGroupsForUser(key);
              }
            });
  }
}
