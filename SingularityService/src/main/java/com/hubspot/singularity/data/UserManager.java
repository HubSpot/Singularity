package com.hubspot.singularity.data;

import java.util.Set;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.io.BaseEncoding;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityUserSettings;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.Transcoder;

public class UserManager extends CuratorManager {

  private final Transcoder<SingularityUserSettings> settingsTranscoder;
  private final SingularityValidator validator;

  private static final String USER_ROOT = "/users";
  private static final String SETTINGS_ROOT = USER_ROOT + "/settings";

  @Inject
  public UserManager(CuratorFramework curator, SingularityConfiguration configuration, MetricRegistry metricRegistry,
                     Transcoder<SingularityUserSettings> settingsTranscoder, SingularityValidator validator) {
    super(curator, configuration, metricRegistry);
    this.settingsTranscoder = settingsTranscoder;
    this.validator = validator;
  }

  private String encodeUserId(String userId) {
    validator.checkUserId(userId);
    return BaseEncoding.base64Url().encode(userId.getBytes(Charsets.UTF_8));
  }

  private String getUserSettingsPath(String userId) {
    return ZKPaths.makePath(SETTINGS_ROOT, encodeUserId(userId));
  }

  public void updateUserSettings(String userId, SingularityUserSettings userSettings) {
    save(getUserSettingsPath(userId), userSettings, settingsTranscoder);
  }

  public void addStarredRequestIds(String userId, Set<String> starredRequestIds) {
    final String path = getUserSettingsPath(userId);
    final Optional<SingularityUserSettings> settings = getData(path, settingsTranscoder);
    if (!settings.isPresent()) {
      validator.checkStarredRequests(starredRequestIds);
      save(path, new SingularityUserSettings(starredRequestIds), settingsTranscoder);
      return;
    }
    settings.get().addStarredRequestIds(starredRequestIds);
    validator.checkStarredRequests(settings.get().getStarredRequestIds());
    save(path, settings.get(), settingsTranscoder);
  }

  public void deleteStarredRequestIds(String userId, Set<String> starredRequestIds) {
    final String path = getUserSettingsPath(userId);
    final Optional<SingularityUserSettings> settings = getData(path, settingsTranscoder);
    if (!settings.isPresent()) {
      return;
    }
    save(path, settings.get().deleteStarredRequestIds(starredRequestIds), settingsTranscoder);
  }

  public Optional<SingularityUserSettings> getUserSettings(String userId) {
    return getData(getUserSettingsPath(userId), settingsTranscoder);
  }

  public void deleteUserSettings(String userId) {
    delete(getUserSettingsPath(userId));
  }

}
