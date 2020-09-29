package com.hubspot.singularity.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableSet;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestBuilder;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.config.AuthConfiguration;
import com.hubspot.singularity.config.UserAuthMode;
import com.squarespace.jersey2.guice.JerseyGuiceUtils;
import java.util.Collections;
import java.util.Optional;
import javax.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SingularityGroupsScopesAuthorizerTest {

  static {
    JerseyGuiceUtils.install((s, serviceLocator) -> null);
  }

  private static final SingularityUser ADMIN_USER = new SingularityUser(
    "superman",
    Optional.empty(),
    Optional.empty(),
    Collections.singleton("admin"),
    ImmutableSet.of("SINGULARITY_ADMIN"),
    true
  );

  private static final SingularityUser GROUP_AB_READ_WRITE = new SingularityUser(
    "a",
    Optional.empty(),
    Optional.empty(),
    ImmutableSet.of("a", "b"),
    ImmutableSet.of("SINGULARITY_READONLY", "SINGULARITY_WRITE"),
    true
  );

  private static final SingularityUser GROUP_A_READ_WRITE = new SingularityUser(
    "a",
    Optional.empty(),
    Optional.empty(),
    Collections.singleton("a"),
    ImmutableSet.of("SINGULARITY_READONLY", "SINGULARITY_WRITE"),
    true
  );

  private static final SingularityUser GROUP_B_READ_WRITE = new SingularityUser(
    "b",
    Optional.empty(),
    Optional.empty(),
    Collections.singleton("b"),
    ImmutableSet.of("SINGULARITY_READONLY", "SINGULARITY_WRITE"),
    true
  );

  private static final SingularityUser GROUP_B_READ_ONLY = new SingularityUser(
    "b",
    Optional.empty(),
    Optional.empty(),
    Collections.singleton("b"),
    ImmutableSet.of("SINGULARITY_READONLY"),
    true
  );

  private static final SingularityUser GROUP_A_READ_ONLY = new SingularityUser(
    "a",
    Optional.empty(),
    Optional.empty(),
    Collections.singleton("a"),
    ImmutableSet.of("SINGULARITY_READONLY"),
    true
  );

  private static final SingularityUser GROUP_A_WRITE_ONLY = new SingularityUser(
    "a",
    Optional.empty(),
    Optional.empty(),
    Collections.singleton("a"),
    ImmutableSet.of("SINGULARITY_WRITE"),
    true
  );

  private static final SingularityUser DEFAULT_READ_GROUP = new SingularityUser(
    "a",
    Optional.empty(),
    Optional.empty(),
    Collections.singleton("default-read"),
    ImmutableSet.of("SINGULARITY_READONLY"),
    true
  );

  private static final SingularityUser JITA_USER_READ = new SingularityUser(
    "a",
    Optional.empty(),
    Optional.empty(),
    Collections.singleton("jita"),
    ImmutableSet.of("SINGULARITY_READONLY"),
    true
  );

  private static final SingularityRequest GROUP_A_REQUEST = new SingularityRequestBuilder(
    "a",
    RequestType.WORKER
  )
    .setGroup(Optional.of("a"))
    .build();

  private static final SingularityRequest GROUP_A_REQUEST_W_READ_ONLY_B = new SingularityRequestBuilder(
    "a",
    RequestType.WORKER
  )
    .setGroup(Optional.of("a"))
    .setReadOnlyGroups(Optional.of(Collections.singleton("b")))
    .build();

  private static final SingularityRequest GROUP_A_REQUEST_W_READ_WRITE_B = new SingularityRequestBuilder(
    "a",
    RequestType.WORKER
  )
    .setGroup(Optional.of("a"))
    .setReadWriteGroups(Optional.of(Collections.singleton("b")))
    .build();

  protected SingularityGroupsScopesAuthorizer authorizer;

  @BeforeEach
  public void setup() {
    AuthConfiguration authConfiguration = new AuthConfiguration();
    authConfiguration.setEnabled(true);
    authConfiguration.setAuthMode(UserAuthMode.GROUPS_SCOPES);
    authConfiguration.setJitaGroups(Collections.singleton("jita"));
    authConfiguration.setDefaultReadOnlyGroups(Collections.singleton("default-read"));
    authorizer =
      new SingularityGroupsScopesAuthorizer(
        null,
        authConfiguration,
        singularityEventListener
      );
  }

  @Test
  public void itRecognizesAdminUsers() {
    assertTrue(authorizer.hasAdminAuthorization(ADMIN_USER));
    assertFalse(authorizer.hasAdminAuthorization(GROUP_A_WRITE_ONLY));
    assertDoesNotThrow(() -> authorizer.checkAdminAuthorization(ADMIN_USER));
    assertThrows(
      WebApplicationException.class,
      () -> authorizer.checkAdminAuthorization(GROUP_A_WRITE_ONLY)
    );
  }

  @Test
  public void itAllowsJita() {
    assertDoesNotThrow(() -> authorizer.checkReadAuthorization(JITA_USER_READ));
    assertDoesNotThrow(
      () ->
        authorizer.checkForAuthorization(
          GROUP_A_REQUEST,
          JITA_USER_READ,
          SingularityAuthorizationScope.READ
        )
    );
    assertThrows(
      WebApplicationException.class,
      () ->
        authorizer.checkForAuthorization(
          GROUP_A_REQUEST,
          JITA_USER_READ,
          SingularityAuthorizationScope.WRITE
        )
    );
  }

  @Test
  public void itAllowsDefaultReadOnlyUserToReadWithNoOverride() {
    assertDoesNotThrow(() -> authorizer.checkReadAuthorization(DEFAULT_READ_GROUP));
    assertDoesNotThrow(
      () ->
        authorizer.checkForAuthorization(
          GROUP_A_REQUEST,
          DEFAULT_READ_GROUP,
          SingularityAuthorizationScope.READ
        )
    );
  }

  @Test
  public void itDeniesDefaultGroupWhenOverriddenForWriteOrRead() {
    assertThrows(
      WebApplicationException.class,
      () ->
        authorizer.checkForAuthorization(
          GROUP_A_REQUEST_W_READ_ONLY_B,
          DEFAULT_READ_GROUP,
          SingularityAuthorizationScope.READ
        )
    );
    assertDoesNotThrow(
      () ->
        authorizer.checkForAuthorization(
          GROUP_A_REQUEST_W_READ_WRITE_B,
          DEFAULT_READ_GROUP,
          SingularityAuthorizationScope.READ
        )
    );
  }

  @Test
  public void itAllowsAccessWhenInGroupAndDeniesOtherwise() {
    // user and request in group a
    assertDoesNotThrow(
      () ->
        authorizer.checkForAuthorization(
          GROUP_A_REQUEST,
          GROUP_A_READ_ONLY,
          SingularityAuthorizationScope.READ
        )
    );
    assertTrue(
      authorizer.isAuthorizedForRequest(
        GROUP_A_REQUEST,
        GROUP_A_READ_ONLY,
        SingularityAuthorizationScope.READ
      )
    );
    assertDoesNotThrow(
      () ->
        authorizer.checkForAuthorization(
          GROUP_A_REQUEST,
          GROUP_A_READ_WRITE,
          SingularityAuthorizationScope.WRITE
        )
    );
    assertTrue(
      authorizer.isAuthorizedForRequest(
        GROUP_A_REQUEST,
        GROUP_A_READ_WRITE,
        SingularityAuthorizationScope.WRITE
      )
    );
    assertDoesNotThrow(
      () ->
        authorizer.checkForAuthorization(
          GROUP_A_REQUEST,
          GROUP_A_WRITE_ONLY,
          SingularityAuthorizationScope.WRITE
        )
    );
    assertTrue(
      authorizer.isAuthorizedForRequest(
        GROUP_A_REQUEST,
        GROUP_A_WRITE_ONLY,
        SingularityAuthorizationScope.WRITE
      )
    );

    // user in b not allowed a
    assertThrows(
      WebApplicationException.class,
      () ->
        authorizer.checkForAuthorization(
          GROUP_A_REQUEST,
          GROUP_B_READ_WRITE,
          SingularityAuthorizationScope.WRITE
        )
    );
    assertFalse(
      authorizer.isAuthorizedForRequest(
        GROUP_A_REQUEST,
        GROUP_B_READ_WRITE,
        SingularityAuthorizationScope.WRITE
      )
    );
    assertThrows(
      WebApplicationException.class,
      () ->
        authorizer.checkForAuthorization(
          GROUP_A_REQUEST,
          GROUP_B_READ_WRITE,
          SingularityAuthorizationScope.READ
        )
    );
    assertFalse(
      authorizer.isAuthorizedForRequest(
        GROUP_A_REQUEST,
        GROUP_B_READ_WRITE,
        SingularityAuthorizationScope.READ
      )
    );

    // user allowed when in override read group
    assertDoesNotThrow(
      () ->
        authorizer.checkForAuthorization(
          GROUP_A_REQUEST_W_READ_ONLY_B,
          GROUP_B_READ_WRITE,
          SingularityAuthorizationScope.READ
        )
    );
    assertTrue(
      authorizer.isAuthorizedForRequest(
        GROUP_A_REQUEST_W_READ_ONLY_B,
        GROUP_B_READ_WRITE,
        SingularityAuthorizationScope.READ
      )
    );
    assertDoesNotThrow(
      () ->
        authorizer.checkForAuthorization(
          GROUP_A_REQUEST_W_READ_ONLY_B,
          GROUP_B_READ_ONLY,
          SingularityAuthorizationScope.READ
        )
    );
    assertTrue(
      authorizer.isAuthorizedForRequest(
        GROUP_A_REQUEST_W_READ_ONLY_B,
        GROUP_B_READ_ONLY,
        SingularityAuthorizationScope.READ
      )
    );
    assertThrows(
      WebApplicationException.class,
      () ->
        authorizer.checkForAuthorization(
          GROUP_A_REQUEST_W_READ_ONLY_B,
          GROUP_B_READ_WRITE,
          SingularityAuthorizationScope.WRITE
        )
    );
    assertFalse(
      authorizer.isAuthorizedForRequest(
        GROUP_A_REQUEST_W_READ_ONLY_B,
        GROUP_B_READ_WRITE,
        SingularityAuthorizationScope.WRITE
      )
    );

    // user allowed read/write when in override write group and has write
    assertDoesNotThrow(
      () ->
        authorizer.checkForAuthorization(
          GROUP_A_REQUEST_W_READ_WRITE_B,
          GROUP_B_READ_ONLY,
          SingularityAuthorizationScope.READ
        )
    );
    assertTrue(
      authorizer.isAuthorizedForRequest(
        GROUP_A_REQUEST_W_READ_WRITE_B,
        GROUP_B_READ_ONLY,
        SingularityAuthorizationScope.READ
      )
    );

    assertThrows(
      WebApplicationException.class,
      () ->
        authorizer.checkForAuthorization(
          GROUP_A_REQUEST_W_READ_WRITE_B,
          GROUP_B_READ_ONLY,
          SingularityAuthorizationScope.WRITE
        )
    );
    assertFalse(
      authorizer.isAuthorizedForRequest(
        GROUP_A_REQUEST_W_READ_WRITE_B,
        GROUP_B_READ_ONLY,
        SingularityAuthorizationScope.WRITE
      )
    );

    assertDoesNotThrow(
      () ->
        authorizer.checkForAuthorization(
          GROUP_A_REQUEST_W_READ_WRITE_B,
          GROUP_B_READ_WRITE,
          SingularityAuthorizationScope.READ
        )
    );
    assertTrue(
      authorizer.isAuthorizedForRequest(
        GROUP_A_REQUEST_W_READ_WRITE_B,
        GROUP_B_READ_WRITE,
        SingularityAuthorizationScope.READ
      )
    );

    assertDoesNotThrow(
      () ->
        authorizer.checkForAuthorization(
          GROUP_A_REQUEST_W_READ_WRITE_B,
          GROUP_B_READ_WRITE,
          SingularityAuthorizationScope.WRITE
        )
    );
    assertTrue(
      authorizer.isAuthorizedForRequest(
        GROUP_A_REQUEST_W_READ_WRITE_B,
        GROUP_B_READ_WRITE,
        SingularityAuthorizationScope.WRITE
      )
    );
  }

  @Test
  public void itAllowsChangeOfGroupWhenInOriginal() {
    assertDoesNotThrow(
      () ->
        authorizer.checkForAuthorizedChanges(
          GROUP_A_REQUEST,
          GROUP_A_REQUEST_W_READ_WRITE_B,
          GROUP_AB_READ_WRITE
        )
    );
    assertThrows(
      WebApplicationException.class,
      () ->
        authorizer.checkForAuthorizedChanges(
          GROUP_A_REQUEST_W_READ_WRITE_B,
          GROUP_A_REQUEST,
          GROUP_B_READ_WRITE
        )
    );
  }
}
