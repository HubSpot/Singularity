package com.hubspot.singularity.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableSet;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestBuilder;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.config.AuthConfiguration;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.RequestManager;
import com.squarespace.jersey2.guice.JerseyGuiceUtils;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SingularityGroupsAuthorizerTest {

  static {
    JerseyGuiceUtils.install((s, serviceLocator) -> null);
  }

  public static SingularityConfiguration buildAuthDisabledConfig() {
    AuthConfiguration authConfiguration = new AuthConfiguration();
    authConfiguration.setEnabled(false);

    SingularityConfiguration configuration = new SingularityConfiguration();
    configuration.setAuthConfiguration(authConfiguration);
    configuration.setMesosConfiguration(new MesosConfiguration());

    return configuration;
  }

  public static SingularityConfiguration buildAuthEnabledConfig() {
    return buildAuthEnabledConfig(
      Collections.<String>emptySet(),
      Collections.<String>emptySet(),
      Collections.<String>emptySet()
    );
  }

  public static SingularityConfiguration buildAuthEnabledConfig(
    Set<String> requiredGroups,
    Set<String> adminGroups,
    Set<String> jitaGroups
  ) {
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

  public static final SingularityRequest REQUEST_WITH_NO_GROUP = new SingularityRequestBuilder(
    "test",
    RequestType.SERVICE
  )
  .build();
  public static final SingularityRequest REQUEST_WITH_GROUP_A = new SingularityRequestBuilder(
    "test_a",
    RequestType.SERVICE
  )
    .setGroup(Optional.of("a"))
    .build();
  public static final SingularityRequest REQUEST_WITH_GROUP_A_CHANGED_TO_B = new SingularityRequestBuilder(
    "test_a",
    RequestType.SERVICE
  )
    .setGroup(Optional.of("b"))
    .build();
  public static final SingularityRequest REQUEST_WITH_GROUP_B = new SingularityRequestBuilder(
    "test_b",
    RequestType.SERVICE
  )
    .setGroup(Optional.of("b"))
    .build();

  public static final SingularityUser NOT_LOGGED_IN = SingularityUser.DEFAULT_USER;
  public static final SingularityUser USER_GROUP_A = new SingularityUser(
    "test1",
    Optional.of("test user1"),
    Optional.of("test1@test.com"),
    ImmutableSet.of("a")
  );
  public static final SingularityUser USER_GROUP_AB = new SingularityUser(
    "test2",
    Optional.of("test user2"),
    Optional.of("test2@test.com"),
    ImmutableSet.of("a", "b")
  );
  public static final SingularityUser USER_GROUP_B = new SingularityUser(
    "test3",
    Optional.of("test user3"),
    Optional.of("test3@test.com"),
    ImmutableSet.of("b")
  );
  public static final SingularityUser USER_GROUP_ADMIN = new SingularityUser(
    "admin",
    Optional.of("admin user"),
    Optional.of("admin@test.com"),
    ImmutableSet.of("admin")
  );

  private SingularityAuthorizer buildAuthorizationHelper(
    SingularityConfiguration configuration
  ) {
    return new SingularityGroupsAuthorizer(requestManager, configuration);
  }

  private final RequestManager requestManager;

  public SingularityGroupsAuthorizerTest() {
    requestManager = mock(RequestManager.class);

    when(requestManager.getRequest(REQUEST_WITH_NO_GROUP.getId()))
      .thenReturn(
        Optional.of(
          new SingularityRequestWithState(REQUEST_WITH_NO_GROUP, RequestState.ACTIVE, 0)
        )
      );
    when(requestManager.getRequest(REQUEST_WITH_GROUP_A.getId()))
      .thenReturn(
        Optional.of(
          new SingularityRequestWithState(REQUEST_WITH_GROUP_A, RequestState.ACTIVE, 0)
        )
      );
    when(requestManager.getRequest(REQUEST_WITH_GROUP_B.getId()))
      .thenReturn(
        Optional.of(
          new SingularityRequestWithState(REQUEST_WITH_GROUP_B, RequestState.ACTIVE, 0)
        )
      );
  }

  @Test
  public void testAuthDisabled() {
    final SingularityAuthorizer authorizationHelper = buildAuthorizationHelper(
      buildAuthDisabledConfig()
    );

    // anyone should be authorized for requests with no group
    assertTrue(
      authorizationHelper.isAuthorizedForRequest(
        REQUEST_WITH_NO_GROUP,
        NOT_LOGGED_IN,
        SingularityAuthorizationScope.READ
      )
    );
    assertTrue(
      authorizationHelper.isAuthorizedForRequest(
        REQUEST_WITH_NO_GROUP,
        USER_GROUP_A,
        SingularityAuthorizationScope.READ
      )
    );

    // users with matching group(s) should be authorized
    assertTrue(
      authorizationHelper.isAuthorizedForRequest(
        REQUEST_WITH_GROUP_A,
        USER_GROUP_A,
        SingularityAuthorizationScope.READ
      )
    );
    assertTrue(
      authorizationHelper.isAuthorizedForRequest(
        REQUEST_WITH_GROUP_A,
        USER_GROUP_AB,
        SingularityAuthorizationScope.READ
      )
    );

    // users without matching group(s) should be authorized
    assertTrue(
      authorizationHelper.isAuthorizedForRequest(
        REQUEST_WITH_GROUP_A,
        USER_GROUP_B,
        SingularityAuthorizationScope.READ
      )
    );
  }

  @Test
  public void testAuth() {
    final SingularityAuthorizer authorizationHelper = buildAuthorizationHelper(
      buildAuthEnabledConfig()
    );

    // user must be authenticated
    assertFalse(
      authorizationHelper.isAuthorizedForRequest(
        REQUEST_WITH_NO_GROUP,
        NOT_LOGGED_IN,
        SingularityAuthorizationScope.READ
      )
    );
    assertFalse(
      authorizationHelper.isAuthorizedForRequest(
        REQUEST_WITH_GROUP_A,
        NOT_LOGGED_IN,
        SingularityAuthorizationScope.READ
      )
    );

    // anyone should be authorized for requests with no group
    assertTrue(
      authorizationHelper.isAuthorizedForRequest(
        REQUEST_WITH_NO_GROUP,
        USER_GROUP_A,
        SingularityAuthorizationScope.READ
      )
    );

    // user must be logged in to be authorized for any request
    assertFalse(
      authorizationHelper.isAuthorizedForRequest(
        REQUEST_WITH_NO_GROUP,
        NOT_LOGGED_IN,
        SingularityAuthorizationScope.READ
      )
    );
    assertFalse(
      authorizationHelper.isAuthorizedForRequest(
        REQUEST_WITH_GROUP_A,
        NOT_LOGGED_IN,
        SingularityAuthorizationScope.READ
      )
    );

    // users with matching group(s) should be authorized
    assertTrue(
      authorizationHelper.isAuthorizedForRequest(
        REQUEST_WITH_GROUP_A,
        USER_GROUP_A,
        SingularityAuthorizationScope.READ
      )
    );
    assertTrue(
      authorizationHelper.isAuthorizedForRequest(
        REQUEST_WITH_GROUP_A,
        USER_GROUP_AB,
        SingularityAuthorizationScope.READ
      )
    );

    // users without matching group(s) should not be authorized
    assertFalse(
      authorizationHelper.isAuthorizedForRequest(
        REQUEST_WITH_GROUP_B,
        USER_GROUP_A,
        SingularityAuthorizationScope.READ
      )
    );
  }

  @Test
  public void testAuthRequiredGroup() {
    final SingularityAuthorizer authorizationHelper = buildAuthorizationHelper(
      buildAuthEnabledConfig(
        ImmutableSet.of("a"),
        Collections.<String>emptySet(),
        Collections.<String>emptySet()
      )
    );

    // users not in the required group are unauthorized
    assertFalse(
      authorizationHelper.isAuthorizedForRequest(
        REQUEST_WITH_NO_GROUP,
        NOT_LOGGED_IN,
        SingularityAuthorizationScope.READ
      )
    );
    assertTrue(
      authorizationHelper.isAuthorizedForRequest(
        REQUEST_WITH_NO_GROUP,
        USER_GROUP_A,
        SingularityAuthorizationScope.READ
      )
    );
    assertFalse(
      authorizationHelper.isAuthorizedForRequest(
        REQUEST_WITH_NO_GROUP,
        USER_GROUP_B,
        SingularityAuthorizationScope.READ
      )
    );

    // user must be part of required group(s) and request group
    assertTrue(
      authorizationHelper.isAuthorizedForRequest(
        REQUEST_WITH_GROUP_B,
        USER_GROUP_AB,
        SingularityAuthorizationScope.READ
      )
    );
    assertFalse(
      authorizationHelper.isAuthorizedForRequest(
        REQUEST_WITH_GROUP_B,
        USER_GROUP_A,
        SingularityAuthorizationScope.READ
      )
    );
    assertFalse(
      authorizationHelper.isAuthorizedForRequest(
        REQUEST_WITH_GROUP_B,
        USER_GROUP_B,
        SingularityAuthorizationScope.READ
      )
    );
  }

  @Test
  public void testAuthAdminGroup() {
    final SingularityAuthorizer authorizationHelper = buildAuthorizationHelper(
      buildAuthEnabledConfig(
        Collections.<String>emptySet(),
        ImmutableSet.of("admin"),
        Collections.<String>emptySet()
      )
    );

    // only users in admin group has admin authorization
    assertFalse(authorizationHelper.hasAdminAuthorization(NOT_LOGGED_IN));
    assertFalse(authorizationHelper.hasAdminAuthorization(USER_GROUP_A));
    assertFalse(authorizationHelper.hasAdminAuthorization(USER_GROUP_AB));
    assertTrue(authorizationHelper.hasAdminAuthorization(USER_GROUP_ADMIN));

    // users in admin group have access to all
    assertTrue(
      authorizationHelper.isAuthorizedForRequest(
        REQUEST_WITH_NO_GROUP,
        USER_GROUP_ADMIN,
        SingularityAuthorizationScope.READ
      )
    );
    assertTrue(
      authorizationHelper.isAuthorizedForRequest(
        REQUEST_WITH_GROUP_A,
        USER_GROUP_ADMIN,
        SingularityAuthorizationScope.READ
      )
    );
    assertTrue(
      authorizationHelper.isAuthorizedForRequest(
        REQUEST_WITH_GROUP_B,
        USER_GROUP_ADMIN,
        SingularityAuthorizationScope.READ
      )
    );
  }

  @Test
  public void testAuthJitaGroup() {
    final SingularityAuthorizer authorizationHelper = buildAuthorizationHelper(
      buildAuthEnabledConfig(
        Collections.<String>emptySet(),
        ImmutableSet.of("admin"),
        ImmutableSet.of("b")
      )
    );

    // user in JITA group(s) are authorized for all requests
    assertTrue(
      authorizationHelper.isAuthorizedForRequest(
        REQUEST_WITH_NO_GROUP,
        USER_GROUP_B,
        SingularityAuthorizationScope.READ
      )
    );
    assertTrue(
      authorizationHelper.isAuthorizedForRequest(
        REQUEST_WITH_GROUP_A,
        USER_GROUP_B,
        SingularityAuthorizationScope.READ
      )
    );
    assertTrue(
      authorizationHelper.isAuthorizedForRequest(
        REQUEST_WITH_GROUP_B,
        USER_GROUP_B,
        SingularityAuthorizationScope.READ
      )
    );

    // but still aren't admins
    assertFalse(authorizationHelper.hasAdminAuthorization(USER_GROUP_B));
  }

  @Test
  public void testCheckAdminAuthorizationThrowsOnForbidden() {
    final SingularityAuthorizer authorizationHelper = buildAuthorizationHelper(
      buildAuthEnabledConfig(
        Collections.<String>emptySet(),
        ImmutableSet.of("admin"),
        Collections.<String>emptySet()
      )
    );

    Assertions.assertThrows(
      WebApplicationException.class,
      () -> authorizationHelper.checkAdminAuthorization(USER_GROUP_A)
    );
  }

  @Test
  public void testCheckAdminAuthorizationDoesntThrowOnAuthorized() {
    final SingularityAuthorizer authorizationHelper = buildAuthorizationHelper(
      buildAuthEnabledConfig(
        Collections.<String>emptySet(),
        ImmutableSet.of("admin"),
        Collections.<String>emptySet()
      )
    );

    authorizationHelper.checkAdminAuthorization(USER_GROUP_ADMIN);
  }

  @Test
  public void testCheckForAuthorizationByTaskIdDoesntThrowOnAuthorized() {
    final SingularityAuthorizer authorizationHelper = buildAuthorizationHelper(
      buildAuthEnabledConfig(
        Collections.<String>emptySet(),
        ImmutableSet.of("admin"),
        Collections.<String>emptySet()
      )
    );

    authorizationHelper.checkForAuthorizationByRequestId(
      REQUEST_WITH_GROUP_A.getId(),
      USER_GROUP_A,
      SingularityAuthorizationScope.READ
    );
  }

  @Test
  public void testCheckForAuthorizationByTaskIdThrowsOnForbidden() {
    final SingularityAuthorizer authorizationHelper = buildAuthorizationHelper(
      buildAuthEnabledConfig(
        Collections.<String>emptySet(),
        ImmutableSet.of("admin"),
        Collections.<String>emptySet()
      )
    );

    Assertions.assertThrows(
      WebApplicationException.class,
      () ->
        authorizationHelper.checkForAuthorizationByRequestId(
          REQUEST_WITH_GROUP_A.getId(),
          USER_GROUP_B,
          SingularityAuthorizationScope.READ
        )
    );
  }

  @Test
  public void testCheckForAuthorizationDoesntThrowOnAuthorized() {
    final SingularityAuthorizer authorizationHelper = buildAuthorizationHelper(
      buildAuthEnabledConfig(
        Collections.<String>emptySet(),
        ImmutableSet.of("admin"),
        Collections.<String>emptySet()
      )
    );

    authorizationHelper.checkForAuthorization(
      REQUEST_WITH_GROUP_A,
      USER_GROUP_A,
      SingularityAuthorizationScope.READ
    );
  }

  @Test
  public void testCheckForAuthorizationThrowsOnForbidden() {
    final SingularityAuthorizer authorizationHelper = buildAuthorizationHelper(
      buildAuthEnabledConfig(
        Collections.<String>emptySet(),
        ImmutableSet.of("admin"),
        Collections.<String>emptySet()
      )
    );

    Assertions.assertThrows(
      WebApplicationException.class,
      () ->
        authorizationHelper.checkForAuthorization(
          REQUEST_WITH_GROUP_A,
          USER_GROUP_B,
          SingularityAuthorizationScope.READ
        )
    );
  }

  @Test
  public void testCheckForAuthorizationDoesntThrowOnValidChange() {
    final SingularityAuthorizer authorizationHelper = buildAuthorizationHelper(
      buildAuthEnabledConfig(
        Collections.<String>emptySet(),
        ImmutableSet.of("admin"),
        Collections.<String>emptySet()
      )
    );

    authorizationHelper.checkForAuthorization(
      REQUEST_WITH_GROUP_A_CHANGED_TO_B,
      USER_GROUP_AB,
      SingularityAuthorizationScope.READ
    );
  }

  @Test
  public void testCheckForAuthorizationThrowsOnForbiddenChange() {
    final SingularityAuthorizer authorizationHelper = buildAuthorizationHelper(
      buildAuthEnabledConfig(
        Collections.<String>emptySet(),
        ImmutableSet.of("admin"),
        Collections.<String>emptySet()
      )
    );

    Assertions.assertThrows(
      WebApplicationException.class,
      () ->
        authorizationHelper.checkForAuthorization(
          REQUEST_WITH_GROUP_A_CHANGED_TO_B,
          USER_GROUP_A,
          SingularityAuthorizationScope.READ
        )
    );
  }

  @Test
  public void itAllowsUserInReadWriteGroupsToUpdateReadWriteGroups() {
    final SingularityAuthorizer authorizationHelper = buildAuthorizationHelper(
      buildAuthEnabledConfig()
    );

    Set<String> readWriteGroupsOld = new HashSet<>();
    readWriteGroupsOld.add("a");
    final SingularityRequest oldRequest = new SingularityRequestBuilder(
      "test_c",
      RequestType.SERVICE
    )
      .setGroup(Optional.of("c"))
      .setReadWriteGroups(Optional.of(readWriteGroupsOld))
      .build();

    Set<String> readWriteGroupsNew = new HashSet<>();
    readWriteGroupsNew.addAll(readWriteGroupsOld);
    readWriteGroupsNew.add("b");
    final SingularityRequest newRequest = new SingularityRequestBuilder(
      "test_c",
      RequestType.SERVICE
    )
      .setGroup(Optional.of("c"))
      .setReadWriteGroups(Optional.of(readWriteGroupsNew))
      .build();

    authorizationHelper.checkForAuthorizedChanges(newRequest, oldRequest, USER_GROUP_A);
  }

  @Test
  public void itRestrictsAUserFromUpdatingGroupsIfTheyWillNotHaveAccess() {
    final SingularityAuthorizer authorizationHelper = buildAuthorizationHelper(
      buildAuthEnabledConfig()
    );

    Set<String> readWriteGroupsOld = new HashSet<>();
    readWriteGroupsOld.add("a");
    final SingularityRequest oldRequest = new SingularityRequestBuilder(
      "test_c",
      RequestType.SERVICE
    )
      .setGroup(Optional.of("c"))
      .setReadWriteGroups(Optional.of(readWriteGroupsOld))
      .build();

    Set<String> readWriteGroupsNew = new HashSet<>();
    readWriteGroupsNew.add("b");
    final SingularityRequest newRequest = new SingularityRequestBuilder(
      "test_c",
      RequestType.SERVICE
    )
      .setGroup(Optional.of("c"))
      .setReadWriteGroups(Optional.of(readWriteGroupsNew))
      .build();

    Assertions.assertThrows(
      WebApplicationException.class,
      () ->
        authorizationHelper.checkForAuthorizedChanges(
          newRequest,
          oldRequest,
          USER_GROUP_A
        )
    );
  }
}
