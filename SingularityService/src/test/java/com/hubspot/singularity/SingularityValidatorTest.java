package com.hubspot.singularity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Set;

import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.hubspot.singularity.config.LDAPConfiguration;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.SingularityValidator;

public class SingularityValidatorTest {
  public static SingularityConfiguration buildNoAuthConfig() {
    LDAPConfiguration ldapConfiguration = new LDAPConfiguration();

    ldapConfiguration.setEnabled(false);

    SingularityConfiguration configuration = new SingularityConfiguration();
    configuration.setLdapConfiguration(ldapConfiguration);
    configuration.setMesosConfiguration(new MesosConfiguration());

    return configuration;
  }

  public static SingularityConfiguration buildAuthEnabledConfig() {
    return buildAuthEnabledConfig(Collections.<String>emptySet(), Collections.<String>emptySet());
  }

  public static SingularityConfiguration buildAuthEnabledConfig(Set<String> requiredGroups, Set<String> adminGroups) {
    LDAPConfiguration ldapConfiguration = new LDAPConfiguration();

    ldapConfiguration.setEnabled(true);
    ldapConfiguration.setRequiredGroups(requiredGroups);
    ldapConfiguration.setAdminGroups(adminGroups);

    SingularityConfiguration configuration = new SingularityConfiguration();
    configuration.setLdapConfiguration(ldapConfiguration);
    configuration.setMesosConfiguration(new MesosConfiguration());

    return configuration;
  }

  public static final SingularityRequest REQUEST_WITH_NO_GROUP = new SingularityRequestBuilder("test", RequestType.SERVICE).build();
  public static final SingularityRequest REQUEST_WITH_GROUP_A = new SingularityRequestBuilder("test", RequestType.SERVICE).setGroup(Optional.of("a")).build();
  public static final SingularityRequest REQUEST_WITH_GROUP_B = new SingularityRequestBuilder("test", RequestType.SERVICE).setGroup(Optional.of("b")).build();

  public static final Optional<SingularityUser> NOT_LOGGED_IN = Optional.absent();
  public static final Optional<SingularityUser> USER_GROUP_A = Optional.of(new SingularityUser("test", ImmutableSet.of("a")));
  public static final Optional<SingularityUser> USER_GROUP_AB = Optional.of(new SingularityUser("test", ImmutableSet.of("a", "b")));
  public static final Optional<SingularityUser> USER_GROUP_B = Optional.of(new SingularityUser("test", ImmutableSet.of("b")));
  public static final Optional<SingularityUser> USER_GROUP_ADMIN = Optional.of(new SingularityUser("admin", ImmutableSet.of("admin")));

  private SingularityValidator buildValidator(SingularityConfiguration configuration) {
    return new SingularityValidator(configuration, null, null);
  }

  @Test
  public void testNoAuth() {
    final SingularityValidator validator = buildValidator(buildNoAuthConfig());

    // anyone should be authorized for requests with no group
    assertTrue(validator.isAuthorizedForRequest(REQUEST_WITH_NO_GROUP, NOT_LOGGED_IN));
    assertTrue(validator.isAuthorizedForRequest(REQUEST_WITH_NO_GROUP, USER_GROUP_A));

    // users with matching group(s) should be authorized
    assertTrue(validator.isAuthorizedForRequest(REQUEST_WITH_GROUP_A, USER_GROUP_A));
    assertTrue(validator.isAuthorizedForRequest(REQUEST_WITH_GROUP_A, USER_GROUP_AB));

    // users without matching group(s) should be authorized
    assertTrue(validator.isAuthorizedForRequest(REQUEST_WITH_GROUP_A, USER_GROUP_B));
  }

  @Test
  public void testAuth() {
    final SingularityValidator validator = buildValidator(buildAuthEnabledConfig());

    // user must be authenticated
    assertFalse(validator.isAuthorizedForRequest(REQUEST_WITH_NO_GROUP, NOT_LOGGED_IN));
    assertFalse(validator.isAuthorizedForRequest(REQUEST_WITH_GROUP_A, NOT_LOGGED_IN));

    // anyone should be authorized for requests with no group
    assertTrue(validator.isAuthorizedForRequest(REQUEST_WITH_NO_GROUP, USER_GROUP_A));

    // user must be logged in to be authorized for any request
    assertFalse(validator.isAuthorizedForRequest(REQUEST_WITH_NO_GROUP, NOT_LOGGED_IN));
    assertFalse(validator.isAuthorizedForRequest(REQUEST_WITH_GROUP_A, NOT_LOGGED_IN));

    // users with matching group(s) should be authorized
    assertTrue(validator.isAuthorizedForRequest(REQUEST_WITH_GROUP_A, USER_GROUP_A));
    assertTrue(validator.isAuthorizedForRequest(REQUEST_WITH_GROUP_A, USER_GROUP_AB));

    // users without matching group(s) should not be authorized
    assertFalse(validator.isAuthorizedForRequest(REQUEST_WITH_GROUP_B, USER_GROUP_A));
  }

  @Test
  public void testAuthRequiredGroup() {
    final SingularityValidator validator = buildValidator(buildAuthEnabledConfig(ImmutableSet.of("a"), Collections.<String>emptySet()));

    // users not in the required group are unauthorized
    assertFalse(validator.isAuthorizedForRequest(REQUEST_WITH_NO_GROUP, NOT_LOGGED_IN));
    assertTrue(validator.isAuthorizedForRequest(REQUEST_WITH_NO_GROUP, USER_GROUP_A));
    assertFalse(validator.isAuthorizedForRequest(REQUEST_WITH_NO_GROUP, USER_GROUP_B));

    // user must be part of required group(s) and request group
    assertTrue(validator.isAuthorizedForRequest(REQUEST_WITH_GROUP_B, USER_GROUP_AB));
    assertFalse(validator.isAuthorizedForRequest(REQUEST_WITH_GROUP_B, USER_GROUP_A));
    assertFalse(validator.isAuthorizedForRequest(REQUEST_WITH_GROUP_B, USER_GROUP_B));
  }

  @Test
  public void testAuthAdminGroup() {
    final SingularityValidator validator = buildValidator(buildAuthEnabledConfig(ImmutableSet.of("a"), ImmutableSet.of("admin")));

    // users in admin group have access to all
    assertTrue(validator.isAuthorizedForRequest(REQUEST_WITH_NO_GROUP, USER_GROUP_ADMIN));
    assertTrue(validator.isAuthorizedForRequest(REQUEST_WITH_GROUP_A, USER_GROUP_ADMIN));
    assertTrue(validator.isAuthorizedForRequest(REQUEST_WITH_GROUP_B, USER_GROUP_ADMIN));
  }
}
