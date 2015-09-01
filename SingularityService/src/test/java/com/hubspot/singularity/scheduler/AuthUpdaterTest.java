package com.hubspot.singularity.scheduler;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularitySchedulerTestBase;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.SingularityUserBuilder;
import com.hubspot.singularity.TestingAuthDatastore;
import com.hubspot.singularity.auth.SingularityAuthUpdater;
import com.hubspot.singularity.config.AuthConfiguration;
import com.hubspot.singularity.data.AuthManager;

public class AuthUpdaterTest extends SingularitySchedulerTestBase {
  private static final String TEST_USER_ID1 = "test1";
  private static final String TEST_USER_ID2 = "test2";
  private static final String TEST_USER_GROUP = "testgroup";

  @Inject
  private AuthManager authManager;

  private final TestingAuthDatastore authDatastore;

  public AuthUpdaterTest() {
    super(false);

    authDatastore = new TestingAuthDatastore();
  }

  @Test
  public void testUserUpdate() {
    authDatastore.clearUsers();

    final SingularityAuthUpdater authUpdater = new SingularityAuthUpdater(new AuthConfiguration(), authManager, authDatastore);

    final long now = System.currentTimeMillis();

    final SingularityUser testUser = new SingularityUserBuilder(TEST_USER_ID1)
            .setLastUpdatedAt(Optional.of(now))
            .build();
    final SingularityUser testUserWithGroup = new SingularityUserBuilder(TEST_USER_ID1)
            .setGroups(ImmutableSet.of(TEST_USER_GROUP))
            .setLastUpdatedAt(Optional.of(now))
            .build();

    authDatastore.addUser(testUser);

    // user in datastore but not ZK
    assertEquals(Optional.of(testUser), authDatastore.getUser(TEST_USER_ID1));
    assertEquals(Optional.absent(), authManager.getUser(TEST_USER_ID1));

    authUpdater.runActionOnPoll();

    // user in ZK
    assertEquals(Optional.of(testUser), authManager.getUser(TEST_USER_ID1));

    // user updates
    authDatastore.addUser(testUserWithGroup);

    authUpdater.runActionOnPoll();

    // updated user in ZK
    assertEquals(Optional.of(testUserWithGroup), authManager.getUser(TEST_USER_ID1));
  }

  @Test
  public void testUserPurge() {
    authDatastore.clearUsers();

    final SingularityAuthUpdater authUpdater = new SingularityAuthUpdater(new AuthConfiguration(), authManager, authDatastore);

    final SingularityUser userYesterday = new SingularityUserBuilder(TEST_USER_ID1)
            .setLastUpdatedAt(Optional.of(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)))
            .build();
    final SingularityUser userFiveDaysAgo = new SingularityUserBuilder(TEST_USER_ID2)
            .setLastUpdatedAt(Optional.of(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(5)))
            .build();

    authManager.updateUser(userYesterday);
    authManager.updateUser(userFiveDaysAgo);

    assertEquals(Optional.of(userYesterday), authManager.getUser(userYesterday.getId()));
    assertEquals(Optional.of(userFiveDaysAgo), authManager.getUser(userFiveDaysAgo.getId()));

    authUpdater.runActionOnPoll();  // purge userFiveDaysAgo, but not userYesterday

    assertEquals(Optional.of(userYesterday), authManager.getUser(userYesterday.getId()));
    assertEquals(Optional.absent(), authManager.getUser(userFiveDaysAgo.getId()));
  }

}
