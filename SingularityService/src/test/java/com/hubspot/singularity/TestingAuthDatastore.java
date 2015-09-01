package com.hubspot.singularity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;
import com.hubspot.singularity.auth.datastore.SingularityAuthDatastore;

public class TestingAuthDatastore implements SingularityAuthDatastore {
  private final Map<String, SingularityUser> users;

  public TestingAuthDatastore() {
    this.users = new HashMap<>();
  }

  public void addUser(SingularityUser user) {
    users.put(user.getId(), user);
  }

  public void removeUser(String userId) {
    users.remove(userId);
  }

  public void clearUsers() {
    users.clear();
  }

  @Override
  public Optional<SingularityUser> getUser(String username) {
    return Optional.fromNullable(users.get(username));
  }

  @Override
  public Optional<Boolean> isHealthy() {
    return Optional.absent();
  }

  @Override
  public List<SingularityUser> getUsers() {
    return new ArrayList<>(users.values());
  }
}
