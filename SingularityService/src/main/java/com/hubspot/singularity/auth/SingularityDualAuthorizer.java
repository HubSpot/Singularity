package com.hubspot.singularity.auth;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.data.RequestManager;
import javax.ws.rs.WebApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Temporary class meant for comparing the newer groups + scopes auth to the older groups-only.
 * This authorizer will use the response of the older groups auth, but log if the newer auth does
 * not produce the same result.
 * This class consumes the newer SingularityUser with both groups and scopes attached
 */
@Singleton
public class SingularityDualAuthorizer extends SingularityAuthorizer {
  private static final Logger LOG = LoggerFactory.getLogger(
    SingularityDualAuthorizer.class
  );

  private final SingularityGroupsAuthorizer groupsAuthorizer;
  private final SingularityGroupsScopesAuthorizer groupsScopesAuthorizer;

  @Inject
  public SingularityDualAuthorizer(
    RequestManager requestManager,
    boolean authEnabled,
    SingularityGroupsAuthorizer groupsAuthorizer,
    SingularityGroupsScopesAuthorizer groupsScopesAuthorizer
  ) {
    super(requestManager, authEnabled);
    this.groupsAuthorizer = groupsAuthorizer;
    this.groupsScopesAuthorizer = groupsScopesAuthorizer;
  }

  @Override
  public boolean hasAdminAuthorization(SingularityUser user) {
    boolean result = groupsAuthorizer.hasAdminAuthorization(user.withOnlyGroups());
    if (result != groupsScopesAuthorizer.hasAdminAuthorization(user)) {
      LOG.warn(
        "Difference in auth of user {} for ADMIN, scopes authorizer: {}, groups authorizer: {}, user: {}",
        user.getId(),
        !result,
        result,
        user
      );
    }
    return result;
  }

  @Override
  public void checkAdminAuthorization(SingularityUser user) {
    boolean grantedByScopes = checkGrantedByScopes(
      () -> groupsScopesAuthorizer.checkAdminAuthorization(user)
    );
    try {
      groupsAuthorizer.checkAdminAuthorization(user.withOnlyGroups());
    } catch (WebApplicationException e) {
      if (grantedByScopes) {
        LOG.warn(
          "Difference in auth of user {} for ADMIN, scopes authorizer: {}, groups authorizer: false, user: {}",
          user.getId(),
          grantedByScopes,
          user
        );
      }
      throw e;
    }
    if (!grantedByScopes) {
      LOG.warn(
        "Difference in auth of user {} for ADMIN, scopes authorizer: {}, groups authorizer: true, user: {}",
        user.getId(),
        grantedByScopes,
        user
      );
    }
  }

  @Override
  public void checkReadAuthorization(SingularityUser user) {
    boolean grantedByScopes = checkGrantedByScopes(
      () -> groupsScopesAuthorizer.checkReadAuthorization(user)
    );
    try {
      groupsAuthorizer.checkReadAuthorization(user.withOnlyGroups());
    } catch (WebApplicationException e) {
      if (grantedByScopes) {
        LOG.warn(
          "Difference in auth of user {} for READ, scopes authorizer: {}, groups authorizer: false, user: {}",
          user.getId(),
          grantedByScopes,
          user
        );
      }
      throw e;
    }
    if (!grantedByScopes) {
      LOG.warn(
        "Difference in auth of user {} for READ, scopes authorizer: {}, groups authorizer: true, user: {}",
        user.getId(),
        grantedByScopes,
        user
      );
    }
  }

  @Override
  public void checkForAuthorization(
    SingularityRequest request,
    SingularityUser user,
    SingularityAuthorizationScope scope
  ) {
    boolean grantedByScopes = checkGrantedByScopes(
      () -> groupsScopesAuthorizer.checkForAuthorization(request, user, scope)
    );
    try {
      groupsAuthorizer.checkForAuthorization(request, user.withOnlyGroups(), scope);
    } catch (WebApplicationException e) {
      if (grantedByScopes) {
        LOG.warn(
          "Difference in auth of user {} for READ, scopes authorizer: {}, groups authorizer: false, user: {}",
          user.getId(),
          grantedByScopes,
          user
        );
      }
      throw e;
    }
    if (!grantedByScopes) {
      LOG.warn(
        "Difference in auth of user {} for READ, scopes authorizer: {}, groups authorizer: true, user: {}",
        user.getId(),
        grantedByScopes,
        user
      );
    }
  }

  @Override
  public boolean isAuthorizedForRequest(
    SingularityRequest request,
    SingularityUser user,
    SingularityAuthorizationScope scope
  ) {
    boolean result = groupsAuthorizer.isAuthorizedForRequest(
      request,
      user.withOnlyGroups(),
      scope
    );
    if (result != groupsScopesAuthorizer.isAuthorizedForRequest(request, user, scope)) {
      LOG.warn(
        "Difference in auth of user {} for {}, scopes authorizer: {}, groups authorizer: {}, user: {}",
        user.getId(),
        request.getId(),
        !result,
        result,
        user
      );
    }
    return result;
  }

  @Override
  public void checkForAuthorizedChanges(
    SingularityRequest request,
    SingularityRequest oldRequest,
    SingularityUser user
  ) {
    boolean grantedByScopes = checkGrantedByScopes(
      () -> groupsScopesAuthorizer.checkForAuthorizedChanges(request, oldRequest, user)
    );
    try {
      groupsAuthorizer.checkForAuthorizedChanges(
        request,
        oldRequest,
        user.withOnlyGroups()
      );
    } catch (WebApplicationException e) {
      if (grantedByScopes) {
        LOG.warn(
          "Difference in auth of user {} for READ, scopes authorizer: {}, groups authorizer: false, user: {}",
          user.getId(),
          grantedByScopes,
          user
        );
      }
      throw e;
    }
    if (!grantedByScopes) {
      LOG.warn(
        "Difference in auth of user {} for READ, scopes authorizer: {}, groups authorizer: true, user: {}",
        user.getId(),
        grantedByScopes,
        user
      );
    }
  }

  private boolean checkGrantedByScopes(Runnable r) {
    try {
      r.run();
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
