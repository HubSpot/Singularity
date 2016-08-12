package com.hubspot.singularity.data;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityUserSettings;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.Transcoder;

public class UserManager extends CuratorManager {

  private final Transcoder<SingularityUserSettings> settingsTranscoder;

  private static final String USER_ROOT = "/users";

  private static final String SETTINGS_ROOT = USER_ROOT + "/settings";

  @Inject
  public UserManager(CuratorFramework curator, SingularityConfiguration configuration, MetricRegistry metricRegistry,
                     Transcoder<SingularityUserSettings> settingsTranscoder) {
    super(curator, configuration, metricRegistry);
    this.settingsTranscoder = settingsTranscoder;
  }

  private String getUserSettingsPath(String id) {
    return ZKPaths.makePath(SETTINGS_ROOT, id);
  }

  public void updateUserSettings(String id, SingularityUserSettings userSettings) {
    save(getUserSettingsPath(id), userSettings, settingsTranscoder);
  }

  public Optional<SingularityUserSettings> getUserSettings(String id) {
    return getData(getUserSettingsPath(id), settingsTranscoder);
  }

  public void deleteUserSettings(String id) {
    delete(getUserSettingsPath(id));
  }

}
