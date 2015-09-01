package com.hubspot.singularity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import javax.ws.rs.WebApplicationException;

import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.config.AuthConfiguration;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.RequestManager;

public class SingularityAuthorizationHelperTest {

  public static SingularityConfiguration buildAuthDisabledConfig() {
    AuthConfiguration authConfiguration = new AuthConfiguration();
    authConfiguration.setEnabled(false);

    SingularityConfiguration configuration = new SingularityConfiguration();
    configuration.setAuthConfiguration(authConfiguration);
    configuration.setMesosConfiguration(new MesosConfiguration());

    return configuration;
  }

  public static SingularityConfiguration buildAuthEnabledConfig() {
    return buildAuthEnabledConfig(Collections.<String>emptySet(), Collections.<String>emptySet(), Collections.<String>emptySet());
  }

  public static SingularityConfiguration buildAuthEnabledConfig(Set<String> requiredGroups, Set<String> adminGroups, Set<String> jitaGroups) {
    AuthConfiguration authConfiguration = new AuthConfiguration();
    authConfiguration.setEnabled(true);
    authConfiguration.setRequiredGroups(requiredGroups);
    authConfiguration.setAdminGroups(adminGroups);
    authConfiguration.setJitaGroups(jitaGroups);

    SingularityConfiguration configuration = new SingularityConfiguration();
    configuration.setAuthConfiguration(authConfiguration);
    configuration.setMesosConfiguration(new MesosConfiguration());

    return configuration;
  }

  public static final SingularityRequest REQUEST_WITH_NO_GROUP = new SingularityRequestBuilder("test", RequestType.SERVICE).build();
  public static final SingularityRequest REQUEST_WITH_GROUP_A = new SingularityRequestBuilder("test_a", RequestType.SERVICE).setGroup(Optional.of("a")).build();
  public static final SingularityRequest REQUEST_WITH_GROUP_A_CHANGED_TO_B = new SingularityRequestBuilder("test_a", RequestType.SERVICE).setGroup(Optional.of("b")).build();
  public static final SingularityRequest REQUEST_WITH_GROUP_B = new SingularityRequestBuilder("test_b", RequestType.SERVICE).setGroup(Optional.of("b")).build();

  public static final Optional<SingularityUser> NOT_LOGGED_IN = Optional.absent();
  public static final Optional<SingularityUser> USER_GROUP_A = Optional.of(new SingularityUser("test1", Optional.of("test user1"), Optional.of("test1@test.com"), ImmutableSet.of("a"), Optional.<Long>absent()));
  public static final Optional<SingularityUser> USER_GROUP_AB = Optional.of(new SingularityUser("test2", Optional.of("test user2"), Optional.of("test2@test.com"), ImmutableSet.of("a", "b"), Optional.<Long>absent()));
  public static final Optional<SingularityUser> USER_GROUP_B = Optional.of(new SingularityUser("test3", Optional.of("test user3"), Optional.of("test3@test.com"), ImmutableSet.of("b"), Optional.<Long>absent()));
  public static final Optional<SingularityUser> USER_GROUP_ADMIN = Optional.of(new SingularityUser("admin", Optional.of("admin user"), Optional.of("admin@test.com"), ImmutableSet.of("admin"), Optional.<Long>absent()));

  private SingularityAuthorizationHelper buildAuthorizationHelper(SingularityConfiguration configuration) {
    return new SingularityAuthorizationHelper(requestManager, configuration);
  }

  private final RequestManager requestManager;

  public SingularityAuthorizationHelperTest() {
    requestManager = mock(RequestManager.class);

    when(requestManager.getRequest(REQUEST_WITH_NO_GROUP.getId())).thenReturn(Optional.of(new SingularityRequestWithState(REQUEST_WITH_NO_GROUP, RequestState.ACTIVE, 0)));
    when(requestManager.getRequest(REQUEST_WITH_GROUP_A.getId())).thenReturn(Optional.of(new SingularityRequestWithState(REQUEST_WITH_GROUP_A, RequestState.ACTIVE, 0)));
    when(requestManager.getRequest(REQUEST_WITH_GROUP_B.getId())).thenReturn(Optional.of(new SingularityRequestWithState(REQUEST_WITH_GROUP_B, RequestState.ACTIVE, 0)));
  }

  @Test
  public void testAuthDisabled() {
    final SingularityAuthorizationHelper authorizationHelper = buildAuthorizationHelper(buildAuthDisabledConfig());

    // anyone should be authorized for requests with no group
    assertTrue(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_NO_GROUP, NOT_LOGGED_IN));
    assertTrue(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_NO_GROUP, USER_GROUP_A));

    // users with matching group(s) should be authorized
    assertTrue(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_GROUP_A, USER_GROUP_A));
    assertTrue(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_GROUP_A, USER_GROUP_AB));

    // users without matching group(s) should be authorized
    assertTrue(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_GROUP_A, USER_GROUP_B));
  }

  @Test
  public void testAuth() {
    final SingularityAuthorizationHelper authorizationHelper = buildAuthorizationHelper(buildAuthEnabledConfig());

    // user must be authenticated
    assertFalse(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_NO_GROUP, NOT_LOGGED_IN));
    assertFalse(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_GROUP_A, NOT_LOGGED_IN));

    // anyone should be authorized for requests with no group
    assertTrue(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_NO_GROUP, USER_GROUP_A));

    // user must be logged in to be authorized for any request
    assertFalse(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_NO_GROUP, NOT_LOGGED_IN));
    assertFalse(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_GROUP_A, NOT_LOGGED_IN));

    // users with matching group(s) should be authorized
    assertTrue(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_GROUP_A, USER_GROUP_A));
    assertTrue(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_GROUP_A, USER_GROUP_AB));

    // users without matching group(s) should not be authorized
    assertFalse(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_GROUP_B, USER_GROUP_A));
  }

  @Test
  public void testAuthRequiredGroup() {
    final SingularityAuthorizationHelper authorizationHelper = buildAuthorizationHelper(buildAuthEnabledConfig(ImmutableSet.of("a"), Collections.<String>emptySet(), Collections.<String>emptySet()));

    // users not in the required group are unauthorized
    assertFalse(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_NO_GROUP, NOT_LOGGED_IN));
    assertTrue(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_NO_GROUP, USER_GROUP_A));
    assertFalse(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_NO_GROUP, USER_GROUP_B));

    // user must be part of required group(s) and request group
    assertTrue(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_GROUP_B, USER_GROUP_AB));
    assertFalse(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_GROUP_B, USER_GROUP_A));
    assertFalse(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_GROUP_B, USER_GROUP_B));
  }

  @Test
  public void testAuthAdminGroup() {
    final SingularityAuthorizationHelper authorizationHelper = buildAuthorizationHelper(buildAuthEnabledConfig(Collections.<String>emptySet(), ImmutableSet.of("admin"), Collections.<String>emptySet()));

    // only users in admin group has admin authorization
    assertFalse(authorizationHelper.hasAdminAuthorization(NOT_LOGGED_IN));
    assertFalse(authorizationHelper.hasAdminAuthorization(USER_GROUP_A));
    assertFalse(authorizationHelper.hasAdminAuthorization(USER_GROUP_AB));
    assertTrue(authorizationHelper.hasAdminAuthorization(USER_GROUP_ADMIN));

    // users in admin group have access to all
    assertTrue(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_NO_GROUP, USER_GROUP_ADMIN));
    assertTrue(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_GROUP_A, USER_GROUP_ADMIN));
    assertTrue(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_GROUP_B, USER_GROUP_ADMIN));
  }

  @Test
  public void testAuthJitaGroup() {
    final SingularityAuthorizationHelper authorizationHelper = buildAuthorizationHelper(buildAuthEnabledConfig(Collections.<String>emptySet(), ImmutableSet.of("admin"), ImmutableSet.of("b")));

    // user in JITA group(s) are authorized for all requests
    assertTrue(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_NO_GROUP, USER_GROUP_B));
    assertTrue(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_GROUP_A, USER_GROUP_B));
    assertTrue(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_GROUP_B, USER_GROUP_B));

    // but still aren't admins
    assertFalse(authorizationHelper.hasAdminAuthorization(USER_GROUP_B));
  }

  @Test(expected = WebApplicationException.class)
   public void testCheckAdminAuthorizationThrowsOnForbidden() {
    final SingularityAuthorizationHelper authorizationHelper = buildAuthorizationHelper(buildAuthEnabledConfig(Collections.<String>emptySet(), ImmutableSet.of("admin"), Collections.<String>emptySet()));

    authorizationHelper.checkAdminAuthorization(USER_GROUP_A);
  }

  @Test
  public void testCheckAdminAuthorizationDoesntThrowOnAuthorized() {
    final SingularityAuthorizationHelper authorizationHelper = buildAuthorizationHelper(buildAuthEnabledConfig(Collections.<String>emptySet(), ImmutableSet.of("admin"), Collections.<String>emptySet()));

    authorizationHelper.checkAdminAuthorization(USER_GROUP_ADMIN);
  }

  @Test(expected = WebApplicationException.class)
  public void testCheckRequiredAuthorizationThrowsOnForbidden() {
    final SingularityAuthorizationHelper authorizationHelper = buildAuthorizationHelper(buildAuthEnabledConfig(ImmutableSet.of("a"), ImmutableSet.of("admin"), Collections.<String>emptySet()));

    authorizationHelper.checkRequiredAuthorization(USER_GROUP_B);
  }

  @Test
  public void testCheckRequiredAuthorizationDoesntThrowOnAuthorized() {
    final SingularityAuthorizationHelper authorizationHelper = buildAuthorizationHelper(buildAuthEnabledConfig(ImmutableSet.of("a"), ImmutableSet.of("admin"), Collections.<String>emptySet()));

    authorizationHelper.checkRequiredAuthorization(USER_GROUP_A);
  }

  @Test
  public void testCheckForAuthorizationByTaskIdDoesntThrowOnAuthorized() {
    final SingularityAuthorizationHelper authorizationHelper = buildAuthorizationHelper(buildAuthEnabledConfig(Collections.<String>emptySet(), ImmutableSet.of("admin"), Collections.<String>emptySet()));

    authorizationHelper.checkForAuthorizationByRequestId(REQUEST_WITH_GROUP_A.getId(), USER_GROUP_A);
  }

  @Test(expected = WebApplicationException.class)
  public void testCheckForAuthorizationByTaskIdThrowsOnForbidden() {
    final SingularityAuthorizationHelper authorizationHelper = buildAuthorizationHelper(buildAuthEnabledConfig(Collections.<String>emptySet(), ImmutableSet.of("admin"), Collections.<String>emptySet()));

    authorizationHelper.checkForAuthorizationByRequestId(REQUEST_WITH_GROUP_A.getId(), USER_GROUP_B);
  }

  @Test
  public void testCheckForAuthorizationDoesntThrowOnAuthorized() {
    final SingularityAuthorizationHelper authorizationHelper = buildAuthorizationHelper(buildAuthEnabledConfig(Collections.<String>emptySet(), ImmutableSet.of("admin"), Collections.<String>emptySet()));

    authorizationHelper.checkForAuthorization(REQUEST_WITH_GROUP_A, Optional.<SingularityRequest>absent(), USER_GROUP_A);
  }

  @Test(expected = WebApplicationException.class)
  public void testCheckForAuthorizationThrowsOnForbidden() {
    final SingularityAuthorizationHelper authorizationHelper = buildAuthorizationHelper(buildAuthEnabledConfig(Collections.<String>emptySet(), ImmutableSet.of("admin"), Collections.<String>emptySet()));

    authorizationHelper.checkForAuthorization(REQUEST_WITH_GROUP_A, Optional.<SingularityRequest>absent(), USER_GROUP_B);
  }

  @Test
  public void testCheckForAuthorizationDoesntThrowOnValidChange() {
    final SingularityAuthorizationHelper authorizationHelper = buildAuthorizationHelper(buildAuthEnabledConfig(Collections.<String>emptySet(), ImmutableSet.of("admin"), Collections.<String>emptySet()));

    authorizationHelper.checkForAuthorization(REQUEST_WITH_GROUP_A_CHANGED_TO_B, Optional.of(REQUEST_WITH_GROUP_A), USER_GROUP_AB);
  }

  @Test(expected = WebApplicationException.class)
  public void testCheckForAuthorizationThrowsOnForbiddenChange() {
    final SingularityAuthorizationHelper authorizationHelper = buildAuthorizationHelper(buildAuthEnabledConfig(Collections.<String>emptySet(), ImmutableSet.of("admin"), Collections.<String>emptySet()));

    authorizationHelper.checkForAuthorization(REQUEST_WITH_GROUP_A_CHANGED_TO_B, Optional.of(REQUEST_WITH_GROUP_A), USER_GROUP_A);
  }
}
