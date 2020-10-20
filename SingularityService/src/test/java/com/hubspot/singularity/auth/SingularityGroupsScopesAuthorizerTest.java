package com.hubspot.singularity.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableSet;
import com.hubspot.singularity.CrashLoopInfo;
import com.hubspot.singularity.ElevatedAccessEvent;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityDeployUpdate;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestBuilder;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskWebhook;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.SingularityUserFacingAction;
import com.hubspot.singularity.config.AuthConfiguration;
import com.hubspot.singularity.config.UserAuthMode;
import com.hubspot.singularity.event.SingularityEventListener;
import com.squarespace.jersey2.guice.JerseyGuiceUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SingularityGroupsScopesAuthorizerTest {

  static {
    JerseyGuiceUtils.install((s, serviceLocator) -> null);
  }

  private static final SingularityUser ADMIN_USER = createSingularityUser(
    "superman",
    "admin",
    "SINGULARITY_ADMIN"
  );

  private static final SingularityUser GROUP_AB_READ_WRITE = createSingularityUser(
    "a",
    ImmutableSet.of("a", "b"),
    ImmutableSet.of("SINGULARITY_READONLY", "SINGULARITY_WRITE")
  );

  private static final SingularityUser GROUP_A_READ_WRITE = createSingularityUser(
    "a",
    Collections.singleton("a"),
    ImmutableSet.of("SINGULARITY_READONLY", "SINGULARITY_WRITE")
  );

  private static final SingularityUser GROUP_B_READ_WRITE = createSingularityUser(
    "b",
    Collections.singleton("b"),
    ImmutableSet.of("SINGULARITY_READONLY", "SINGULARITY_WRITE")
  );

  private static final SingularityUser GROUP_B_READ_ONLY = createSingularityUser(
    "b",
    "b",
    "SINGULARITY_READONLY"
  );

  private static final SingularityUser GROUP_A_READ_ONLY = createSingularityUser(
    "a",
    "a",
    "SINGULARITY_READONLY"
  );

  private static final SingularityUser GROUP_A_WRITE_ONLY = createSingularityUser(
    "a",
    "a",
    "SINGULARITY_WRITE"
  );

  private static final SingularityUser DEFAULT_READ_GROUP = createSingularityUser(
    "a",
    "default-read",
    "SINGULARITY_READONLY"
  );

  private static final SingularityUser JITA_USER_READ = createSingularityUser(
    "a",
    "jita",
    "SINGULARITY_READONLY"
  );

  private static final SingularityUser JITA_USER_READ_WRITE = createSingularityUser(
    "a",
    Collections.singleton("jita"),
    ImmutableSet.of("SINGULARITY_READONLY", "SINGULARITY_WRITE")
  );

  private static final SingularityRequest GROUP_A_REQUEST = new SingularityRequestBuilder(
    "a",
    RequestType.WORKER
  )
    .setGroup(Optional.of("a"))
    .build();

  private static final SingularityRequest GROUP_A_REQUEST_READ_ONLY_OVERRIDE = new SingularityRequestBuilder(
    "a",
    RequestType.WORKER
  )
    .setGroup(Optional.of("a"))
    .setReadOnlyGroups(Optional.of(Collections.singleton("a")))
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

  private List<ElevatedAccessEvent> elevatedAccessEvents = new ArrayList<>();
  protected SingularityGroupsScopesAuthorizer authorizer;

  private final SingularityEventListener singularityEventListener = new SingularityEventListener() {

    @Override
    public void requestHistoryEvent(
      SingularityRequestHistory singularityRequestHistory
    ) {}

    @Override
    public void taskHistoryUpdateEvent(SingularityTaskWebhook singularityTaskWebhook) {}

    @Override
    public void deployHistoryEvent(SingularityDeployUpdate singularityDeployUpdate) {}

    @Override
    public void crashLoopEvent(CrashLoopInfo crashLoopUpdate) {}

    @Override
    public void elevatedAccessEvent(ElevatedAccessEvent elevatedAccessEvent) {
      elevatedAccessEvents.add(elevatedAccessEvent);
    }
  };

  @BeforeEach
  public void setup() {
    AuthConfiguration authConfiguration = getAuthConfiguration();
    authorizer =
      new SingularityGroupsScopesAuthorizer(
        null,
        authConfiguration,
        singularityEventListener
      );
  }

  private AuthConfiguration getAuthConfiguration() {
    AuthConfiguration authConfiguration = new AuthConfiguration();
    authConfiguration.setEnabled(true);
    authConfiguration.setAuthMode(UserAuthMode.GROUPS_SCOPES);
    authConfiguration.setJitaGroups(Collections.singleton("jita"));
    authConfiguration.setDefaultReadOnlyGroups(Collections.singleton("default-read"));
    return authConfiguration;
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
    assertEquals(1, elevatedAccessEvents.size());
    assertThrows(
      WebApplicationException.class,
      () ->
        authorizer.checkForAuthorization(
          GROUP_A_REQUEST,
          JITA_USER_READ,
          SingularityAuthorizationScope.WRITE
        )
    );
    assertEquals(2, elevatedAccessEvents.size());
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
  public void itChecksActionPermissionsForJitaUsers() {
    assertNotAuthorized(
      GROUP_A_REQUEST,
      JITA_USER_READ,
      SingularityAuthorizationScope.WRITE,
      SingularityUserFacingAction.BOUNCE
    );
    assertAuthorized(
      GROUP_A_REQUEST,
      JITA_USER_READ_WRITE,
      SingularityAuthorizationScope.WRITE,
      SingularityUserFacingAction.BOUNCE
    );
  }

  @Test
  public void itChecksActionPermissionsForOwningGroup() {
    SingularityRequest request = GROUP_A_REQUEST
      .toBuilder()
      .setActionPermissions(
        Optional.of(
          Collections.singletonMap(
            "a",
            ImmutableSet.of(
              SingularityUserFacingAction.BOUNCE,
              SingularityUserFacingAction.SCALE
            )
          )
        )
      )
      .build();

    assertAuthorized(request, GROUP_A_READ_WRITE, SingularityAuthorizationScope.WRITE);

    assertAuthorized(
      request,
      GROUP_A_READ_WRITE,
      SingularityAuthorizationScope.WRITE,
      SingularityUserFacingAction.BOUNCE
    );
    assertAuthorized(
      request,
      GROUP_A_READ_WRITE,
      SingularityAuthorizationScope.WRITE,
      SingularityUserFacingAction.SCALE
    );

    // KILL_TASK not explicitly allowed
    assertNotAuthorized(
      request,
      GROUP_A_READ_WRITE,
      SingularityAuthorizationScope.WRITE,
      SingularityUserFacingAction.KILL_TASK
    );

    // Write scope missing
    assertNotAuthorized(
      request,
      GROUP_A_READ_ONLY,
      SingularityAuthorizationScope.WRITE,
      SingularityUserFacingAction.BOUNCE
    );

    // Different group
    assertNotAuthorized(
      request,
      GROUP_B_READ_WRITE,
      SingularityAuthorizationScope.WRITE,
      SingularityUserFacingAction.BOUNCE
    );

    // Check admin
    assertAuthorized(
      request,
      ADMIN_USER,
      SingularityAuthorizationScope.WRITE,
      SingularityUserFacingAction.BOUNCE
    );
    assertAuthorized(
      request,
      ADMIN_USER,
      SingularityAuthorizationScope.WRITE,
      SingularityUserFacingAction.PAUSE
    );
  }

  @Test
  public void itBlocksOwningGroupFromActionsWhenNonOwningGroupPermissionsSpecified() {
    SingularityRequest request = GROUP_A_REQUEST
      .toBuilder()
      .setActionPermissions(
        Optional.of(
          Collections.singletonMap(
            "b",
            Collections.singleton(SingularityUserFacingAction.BOUNCE)
          )
        )
      )
      .build();

    assertAuthorized(request, GROUP_A_READ_WRITE, SingularityAuthorizationScope.WRITE);
    assertNotAuthorized(
      request,
      GROUP_A_READ_WRITE,
      SingularityAuthorizationScope.WRITE,
      SingularityUserFacingAction.BOUNCE
    );

    assertAuthorized(
      request.toBuilder().setReadWriteGroups(Optional.of(ImmutableSet.of("b"))).build(),
      GROUP_B_READ_WRITE,
      SingularityAuthorizationScope.WRITE,
      SingularityUserFacingAction.BOUNCE
    );
    assertNotAuthorized(
      request,
      GROUP_B_READ_WRITE,
      SingularityAuthorizationScope.WRITE,
      SingularityUserFacingAction.BOUNCE
    );
    assertNotAuthorized(request, GROUP_B_READ_WRITE, SingularityAuthorizationScope.WRITE);
  }

  @Test
  public void itAllowsAccessWhenInGroupAndDeniesOtherwise() {
    // user and request in group a
    assertAuthorized(
      GROUP_A_REQUEST,
      GROUP_A_READ_ONLY,
      SingularityAuthorizationScope.READ
    );
    assertAuthorized(
      GROUP_A_REQUEST,
      GROUP_A_READ_WRITE,
      SingularityAuthorizationScope.WRITE
    );
    assertAuthorized(
      GROUP_A_REQUEST,
      GROUP_A_WRITE_ONLY,
      SingularityAuthorizationScope.WRITE
    );

    // user a is owner AND is a read only owner
    assertAuthorized(
      GROUP_A_REQUEST_READ_ONLY_OVERRIDE,
      GROUP_A_READ_ONLY,
      SingularityAuthorizationScope.READ
    );
    assertNotAuthorized(
      GROUP_A_REQUEST_READ_ONLY_OVERRIDE,
      GROUP_A_READ_WRITE,
      SingularityAuthorizationScope.WRITE
    );

    // user in b not allowed a
    assertNotAuthorized(
      GROUP_A_REQUEST,
      GROUP_B_READ_WRITE,
      SingularityAuthorizationScope.WRITE
    );
    assertNotAuthorized(
      GROUP_A_REQUEST,
      GROUP_B_READ_WRITE,
      SingularityAuthorizationScope.READ
    );

    // user allowed when in override read group
    assertAuthorized(
      GROUP_A_REQUEST_W_READ_ONLY_B,
      GROUP_B_READ_WRITE,
      SingularityAuthorizationScope.READ
    );
    assertAuthorized(
      GROUP_A_REQUEST_W_READ_ONLY_B,
      GROUP_B_READ_ONLY,
      SingularityAuthorizationScope.READ
    );
    assertNotAuthorized(
      GROUP_A_REQUEST_W_READ_ONLY_B,
      GROUP_B_READ_WRITE,
      SingularityAuthorizationScope.WRITE
    );

    // user allowed read/write when in override write group and has write
    assertAuthorized(
      GROUP_A_REQUEST_W_READ_WRITE_B,
      GROUP_B_READ_ONLY,
      SingularityAuthorizationScope.READ
    );

    assertNotAuthorized(
      GROUP_A_REQUEST_W_READ_WRITE_B,
      GROUP_B_READ_ONLY,
      SingularityAuthorizationScope.WRITE
    );

    assertAuthorized(
      GROUP_A_REQUEST_W_READ_WRITE_B,
      GROUP_B_READ_WRITE,
      SingularityAuthorizationScope.READ
    );

    assertAuthorized(
      GROUP_A_REQUEST_W_READ_WRITE_B,
      GROUP_B_READ_WRITE,
      SingularityAuthorizationScope.WRITE
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

  private static SingularityUser createSingularityUser(
    String id,
    String groups,
    String scope
  ) {
    return new SingularityUser(
      id,
      Optional.empty(),
      Optional.empty(),
      Collections.singleton(groups),
      ImmutableSet.of(scope),
      true
    );
  }

  private static SingularityUser createSingularityUser(
    String id,
    Set<String> groups,
    Set<String> scopes
  ) {
    return new SingularityUser(
      id,
      Optional.empty(),
      Optional.empty(),
      groups,
      scopes,
      true
    );
  }

  private void assertAuthorized(
    SingularityRequest request,
    SingularityUser user,
    SingularityAuthorizationScope scope
  ) {
    assertDoesNotThrow(() -> authorizer.checkForAuthorization(request, user, scope));
    assertTrue(authorizer.isAuthorizedForRequest(request, user, scope));
  }

  private void assertAuthorized(
    SingularityRequest request,
    SingularityUser user,
    SingularityAuthorizationScope scope,
    SingularityUserFacingAction action
  ) {
    assertDoesNotThrow(
      () -> authorizer.checkForAuthorization(request, user, scope, action)
    );
    assertTrue(authorizer.isAuthorizedForRequest(request, user, scope, action));
  }

  private void assertNotAuthorized(
    SingularityRequest request,
    SingularityUser user,
    SingularityAuthorizationScope scope
  ) {
    assertThrows(
      WebApplicationException.class,
      () -> authorizer.checkForAuthorization(request, user, scope)
    );
    assertFalse(authorizer.isAuthorizedForRequest(request, user, scope));
  }

  private void assertNotAuthorized(
    SingularityRequest request,
    SingularityUser user,
    SingularityAuthorizationScope scope,
    SingularityUserFacingAction action
  ) {
    assertThrows(
      WebApplicationException.class,
      () -> authorizer.checkForAuthorization(request, user, scope, action)
    );
    assertFalse(authorizer.isAuthorizedForRequest(request, user, scope, action));
  }
}
