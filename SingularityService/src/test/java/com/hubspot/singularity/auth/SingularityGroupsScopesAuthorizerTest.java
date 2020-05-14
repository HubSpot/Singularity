package com.hubspot.singularity.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableSet;
import com.hubspot.singularity.RequestType;
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

  private static final SingularityRequest GROUP_A_REQUEST = new SingularityRequestBuilder(
    "a",
    RequestType.WORKER
  )
    .setGroup(Optional.of("a"))
    .build();

  private static final SingularityRequest GROUP_B_REQUEST = new SingularityRequestBuilder(
    "b",
    RequestType.WORKER
  )
    .setGroup(Optional.of("b"))
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
    authConfiguration.setDefaultReadOnlyGroups(Collections.singleton("default-read"));
    authorizer = new SingularityGroupsScopesAuthorizer(null, true, authConfiguration);
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
  public void itAllowsDefaultReadOnlyUserToReadWithNoOverride() {}

  @Test
  public void itDeniesDefaultGroupWhenOverriddenForWriteOrRead() {}

  @Test
  public void itDeniesAccessWhenNotInGroup() {}

  @Test
  public void itAllowsAccessWhenInGroup() {}

  @Test
  public void itAllowsAccessForDefaultAndOverridenGroups() {}

  @Test
  public void itAllowsChangeOfGroupWhenInBoth() {}

  @Test
  public void writeAlsoGrantsRead() {}
}
