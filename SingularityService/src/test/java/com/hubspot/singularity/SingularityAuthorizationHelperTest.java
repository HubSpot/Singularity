package com.hubspot.singularity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
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


  public static final SingularityUser NOT_LOGGED_IN = SingularityUser.DEFAULT_USER;
  public static final SingularityUser USER_GROUP_A = new SingularityUser("test1", Optional.of("test user1"), Optional.of("test1@test.com"), ImmutableSet.of("a"));
  public static final SingularityUser USER_GROUP_AB = new SingularityUser("test2", Optional.of("test user2"), Optional.of("test2@test.com"), ImmutableSet.of("a", "b"));
  public static final SingularityUser USER_GROUP_B = new SingularityUser("test3", Optional.of("test user3"), Optional.of("test3@test.com"), ImmutableSet.of("b"));
  public static final SingularityUser USER_GROUP_ADMIN = new SingularityUser("admin", Optional.of("admin user"), Optional.of("admin@test.com"), ImmutableSet.of("admin"));

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
    assertTrue(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_NO_GROUP, NOT_LOGGED_IN, SingularityAuthorizationScope.READ));
    assertTrue(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_NO_GROUP, USER_GROUP_A, SingularityAuthorizationScope.READ));

    // users with matching group(s) should be authorized
    assertTrue(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_GROUP_A, USER_GROUP_A, SingularityAuthorizationScope.READ));
    assertTrue(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_GROUP_A, USER_GROUP_AB, SingularityAuthorizationScope.READ));

    // users without matching group(s) should be authorized
    assertTrue(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_GROUP_A, USER_GROUP_B, SingularityAuthorizationScope.READ));
  }

  @Test
  public void testAuth() {
    final SingularityAuthorizationHelper authorizationHelper = buildAuthorizationHelper(buildAuthEnabledConfig());

    // user must be authenticated
    assertFalse(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_NO_GROUP, NOT_LOGGED_IN, SingularityAuthorizationScope.READ));
    assertFalse(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_GROUP_A, NOT_LOGGED_IN, SingularityAuthorizationScope.READ));

    // anyone should be authorized for requests with no group
    assertTrue(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_NO_GROUP, USER_GROUP_A, SingularityAuthorizationScope.READ));

    // user must be logged in to be authorized for any request
    assertFalse(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_NO_GROUP, NOT_LOGGED_IN, SingularityAuthorizationScope.READ));
    assertFalse(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_GROUP_A, NOT_LOGGED_IN, SingularityAuthorizationScope.READ));

    // users with matching group(s) should be authorized
    assertTrue(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_GROUP_A, USER_GROUP_A, SingularityAuthorizationScope.READ));
    assertTrue(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_GROUP_A, USER_GROUP_AB, SingularityAuthorizationScope.READ));

    // users without matching group(s) should not be authorized
    assertFalse(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_GROUP_B, USER_GROUP_A, SingularityAuthorizationScope.READ));
  }

  @Test
  public void testAuthRequiredGroup() {
    final SingularityAuthorizationHelper authorizationHelper = buildAuthorizationHelper(buildAuthEnabledConfig(ImmutableSet.of("a"), Collections.<String>emptySet(), Collections.<String>emptySet()));

    // users not in the required group are unauthorized
    assertFalse(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_NO_GROUP, NOT_LOGGED_IN, SingularityAuthorizationScope.READ));
    assertTrue(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_NO_GROUP, USER_GROUP_A, SingularityAuthorizationScope.READ));
    assertFalse(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_NO_GROUP, USER_GROUP_B, SingularityAuthorizationScope.READ));

    // user must be part of required group(s) and request group
    assertTrue(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_GROUP_B, USER_GROUP_AB, SingularityAuthorizationScope.READ));
    assertFalse(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_GROUP_B, USER_GROUP_A, SingularityAuthorizationScope.READ));
    assertFalse(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_GROUP_B, USER_GROUP_B, SingularityAuthorizationScope.READ));
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
    assertTrue(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_NO_GROUP, USER_GROUP_ADMIN, SingularityAuthorizationScope.READ));
    assertTrue(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_GROUP_A, USER_GROUP_ADMIN, SingularityAuthorizationScope.READ));
    assertTrue(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_GROUP_B, USER_GROUP_ADMIN, SingularityAuthorizationScope.READ));
  }

  @Test
  public void testAuthJitaGroup() {
    final SingularityAuthorizationHelper authorizationHelper = buildAuthorizationHelper(buildAuthEnabledConfig(Collections.<String>emptySet(), ImmutableSet.of("admin"), ImmutableSet.of("b")));

    // user in JITA group(s) are authorized for all requests
    assertTrue(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_NO_GROUP, USER_GROUP_B, SingularityAuthorizationScope.READ));
    assertTrue(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_GROUP_A, USER_GROUP_B, SingularityAuthorizationScope.READ));
    assertTrue(authorizationHelper.isAuthorizedForRequest(REQUEST_WITH_GROUP_B, USER_GROUP_B, SingularityAuthorizationScope.READ));

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

  @Test
  public void testCheckForAuthorizationByTaskIdDoesntThrowOnAuthorized() {
    final SingularityAuthorizationHelper authorizationHelper = buildAuthorizationHelper(buildAuthEnabledConfig(Collections.<String>emptySet(), ImmutableSet.of("admin"), Collections.<String>emptySet()));

    authorizationHelper.checkForAuthorizationByRequestId(REQUEST_WITH_GROUP_A.getId(), USER_GROUP_A, SingularityAuthorizationScope.READ);
  }

  @Test(expected = WebApplicationException.class)
  public void testCheckForAuthorizationByTaskIdThrowsOnForbidden() {
    final SingularityAuthorizationHelper authorizationHelper = buildAuthorizationHelper(buildAuthEnabledConfig(Collections.<String>emptySet(), ImmutableSet.of("admin"), Collections.<String>emptySet()));

    authorizationHelper.checkForAuthorizationByRequestId(REQUEST_WITH_GROUP_A.getId(), USER_GROUP_B, SingularityAuthorizationScope.READ);
  }

  @Test
  public void testCheckForAuthorizationDoesntThrowOnAuthorized() {
    final SingularityAuthorizationHelper authorizationHelper = buildAuthorizationHelper(buildAuthEnabledConfig(Collections.<String>emptySet(), ImmutableSet.of("admin"), Collections.<String>emptySet()));

    authorizationHelper.checkForAuthorization(REQUEST_WITH_GROUP_A, USER_GROUP_A, SingularityAuthorizationScope.READ);
  }

  @Test(expected = WebApplicationException.class)
  public void testCheckForAuthorizationThrowsOnForbidden() {
    final SingularityAuthorizationHelper authorizationHelper = buildAuthorizationHelper(buildAuthEnabledConfig(Collections.<String>emptySet(), ImmutableSet.of("admin"), Collections.<String>emptySet()));

    authorizationHelper.checkForAuthorization(REQUEST_WITH_GROUP_A, USER_GROUP_B, SingularityAuthorizationScope.READ);
  }

  @Test
  public void testCheckForAuthorizationDoesntThrowOnValidChange() {
    final SingularityAuthorizationHelper authorizationHelper = buildAuthorizationHelper(buildAuthEnabledConfig(Collections.<String>emptySet(), ImmutableSet.of("admin"), Collections.<String>emptySet()));

    authorizationHelper.checkForAuthorization(REQUEST_WITH_GROUP_A_CHANGED_TO_B, USER_GROUP_AB, SingularityAuthorizationScope.READ);
  }

  @Test(expected = WebApplicationException.class)
  public void testCheckForAuthorizationThrowsOnForbiddenChange() {
    final SingularityAuthorizationHelper authorizationHelper = buildAuthorizationHelper(buildAuthEnabledConfig(Collections.<String>emptySet(), ImmutableSet.of("admin"), Collections.<String>emptySet()));

    authorizationHelper.checkForAuthorization(REQUEST_WITH_GROUP_A_CHANGED_TO_B, USER_GROUP_A, SingularityAuthorizationScope.READ);
  }

  @Test
  public void itAllowsUserInReadWriteGroupsToUpdateReadWriteGroups() {
    final SingularityAuthorizationHelper authorizationHelper = buildAuthorizationHelper(buildAuthEnabledConfig());

    Set<String> readWriteGroupsOld = new HashSet<>();
    readWriteGroupsOld.add("a");
    final SingularityRequest oldRequest = new SingularityRequestBuilder("test_c", RequestType.SERVICE)
        .setGroup(Optional.of("c"))
        .setReadWriteGroups(Optional.of(readWriteGroupsOld))
        .build();

    Set<String> readWriteGroupsNew = new HashSet<>();
    readWriteGroupsNew.addAll(readWriteGroupsOld);
    readWriteGroupsNew.add("b");
    final SingularityRequest newRequest = new SingularityRequestBuilder("test_c", RequestType.SERVICE)
        .setGroup(Optional.of("c"))
        .setReadWriteGroups(Optional.of(readWriteGroupsNew))
        .build();

    authorizationHelper.checkForAuthorizedChanges(newRequest, oldRequest, USER_GROUP_A);
  }

  @Test(expected = WebApplicationException.class)
  public void itRestrictsAUserFromUpdatingGroupsIfTheyWillNotHaveAccess() {
    final SingularityAuthorizationHelper authorizationHelper = buildAuthorizationHelper(buildAuthEnabledConfig());

    Set<String> readWriteGroupsOld = new HashSet<>();
    readWriteGroupsOld.add("a");
    final SingularityRequest oldRequest = new SingularityRequestBuilder("test_c", RequestType.SERVICE)
        .setGroup(Optional.of("c"))
        .setReadWriteGroups(Optional.of(readWriteGroupsOld))
        .build();

    Set<String> readWriteGroupsNew = new HashSet<>();
    readWriteGroupsNew.add("b");
    final SingularityRequest newRequest = new SingularityRequestBuilder("test_c", RequestType.SERVICE)
        .setGroup(Optional.of("c"))
        .setReadWriteGroups(Optional.of(readWriteGroupsNew))
        .build();

    authorizationHelper.checkForAuthorizedChanges(newRequest, oldRequest, USER_GROUP_A);
  }
}
