package com.hubspot.singularity.auth;

import com.google.common.collect.ImmutableSet;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.config.AuthConfiguration;
import com.hubspot.singularity.config.UserAuthMode;
import java.util.Collections;
import java.util.Optional;
import org.junit.Before;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SingularityGroupsScopesAuthorizerTest {
  private static final SingularityUser ADMIN_USER = new SingularityUser(
    "superman",
    Optional.empty(),
    Optional.empty(),
    Collections.singleton("admin"),
    ImmutableSet.of("SINGULARITY_ADMIN"),
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
  public void itRecognizesAdminUsers() {}
}
