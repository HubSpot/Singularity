package com.hubspot.singularity.auth;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityAuthState;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.auth.datastore.SingularityAuthDatastore;
import com.hubspot.singularity.config.AuthConfiguration;
import com.hubspot.singularity.data.AuthManager;
import com.hubspot.singularity.scheduler.SingularityLeaderOnlyPoller;

@Singleton
public class SingularityAuthorizationUpdater extends SingularityLeaderOnlyPoller {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityAuthorizationUpdater.class);

  private final AuthManager authManager;
  private final AuthConfiguration authConfiguration;
  private final SingularityAuthDatastore datastore;

  @Inject
  public SingularityAuthorizationUpdater(AuthConfiguration authConfiguration, AuthManager authManager, SingularityAuthDatastore datastore) {
    super(authConfiguration.getUpdateUsersEveryMinutes(), TimeUnit.MINUTES);
    this.authManager = authManager;
    this.datastore = datastore;
    this.authConfiguration = authConfiguration;
  }

  @Override
  protected boolean isEnabled() {
    return authConfiguration.isEnabled();
  }

  @Override
  protected boolean abortsOnError() {
    return false;
  }

  @Override
  public void runActionOnPoll() {
    final long getUsersStart = System.currentTimeMillis();
    final List<SingularityUser> users = datastore.getUsers();

    LOG.info("Loaded {} users from {} in {}ms", users.size(), datastore.getClass().getSimpleName(), System.currentTimeMillis() - getUsersStart);

    final Set<String> existingUserIds = new HashSet<>(authManager.getUserIds());

    final long updateUserStart = System.currentTimeMillis();
    int newUsers = 0;
    for (SingularityUser user : users) {
      authManager.updateUser(user);

      if (!existingUserIds.contains(user.getId())) {
        newUsers++;
      } else {
        existingUserIds.remove(user.getId());
      }
    }

    int purgedUsers = 0;
    for (String userId : existingUserIds) {
      final Optional<SingularityUser> maybeUser = authManager.getUser(userId);

      if (!maybeUser.isPresent()) {
        LOG.warn("Failed to load data for stale user: {}", userId);
        continue;
      }

      if (maybeUser.get().getLastUpdatedAt().isPresent() && ((System.currentTimeMillis() - maybeUser.get().getLastUpdatedAt().get()) > TimeUnit.DAYS.toMillis(authConfiguration.getPurgeOldUsersAfterDays()))) {
        LOG.info("Purging user {}", userId);
        authManager.deleteUser(userId);
        purgedUsers++;
      }
    }

    LOG.info("Updated {} users in {}ms: {} users added, {} stale users, {} users purged", users.size(), System.currentTimeMillis() - updateUserStart, newUsers, existingUserIds.size(), purgedUsers);

    authManager.setAuthState(new SingularityAuthState(Optional.of(System.currentTimeMillis())));
  }
}
