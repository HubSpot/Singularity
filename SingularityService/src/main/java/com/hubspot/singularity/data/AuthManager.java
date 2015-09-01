package com.hubspot.singularity.data;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityAuthState;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.Transcoder;

@Singleton
public class AuthManager extends CuratorAsyncManager {
  private static final String ROOT_PATH = "/auth";
  private static final String USERS_PATH = ROOT_PATH + "/users";
  private static final String USER_PATH = USERS_PATH + "/";

  private final Transcoder<SingularityUser> singularityUserTranscoder;
  private final Transcoder<SingularityAuthState> singularityAuthStateTranscoder;

  @Inject
  public AuthManager(CuratorFramework curator, SingularityConfiguration configuration, Transcoder<SingularityUser> singularityUserTranscoder, Transcoder<SingularityAuthState> singularityAuthStateTranscoder) {
    super(curator, configuration.getZookeeperAsyncTimeout());
    this.singularityUserTranscoder = singularityUserTranscoder;
    this.singularityAuthStateTranscoder = singularityAuthStateTranscoder;
  }

  public List<String> getUserIds() {
    return getChildren(USERS_PATH);
  }

  public Optional<SingularityUser> getUser(String userId) {
    return getData(USER_PATH + userId, singularityUserTranscoder);
  }

  public SingularityCreateResult updateUser(SingularityUser user) {
    return save(USER_PATH + user.getId(), user, singularityUserTranscoder);
  }

  public SingularityDeleteResult deleteUser(String userId) {
    return delete(USER_PATH + userId);
  }

  public Optional<SingularityAuthState> getAuthState() {
    return getData(USERS_PATH, singularityAuthStateTranscoder);
  }

  public SingularityCreateResult setAuthState(SingularityAuthState authState) {
    return save(USERS_PATH, authState, singularityAuthStateTranscoder);
  }
}
