package com.hubspot.singularity.data;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityDisabledAction;
import com.hubspot.singularity.SingularityDisabledActionType;
import com.hubspot.singularity.SingularityDisasterStats;
import com.hubspot.singularity.SingularityDisasterType;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.Transcoder;

public class DisasterManager extends CuratorAsyncManager {
  private static final String DISASTERS_ROOT = "/disasters";
  private static final String DISABLED_ACTIONS = DISASTERS_ROOT + "/disabled-actions";
  private static final String ACTIVE_DISASTERS = DISASTERS_ROOT + "/active";
  private static final String DISASTER_STATS = DISASTERS_ROOT + "/stats";

  private static final String MESSAGE_FORMAT = "Cannot perform action %s: %s";
  private static final String DEFAULT_MESSAGE = "Action is currently disabled";

  private final Transcoder<SingularityDisabledAction> disabledActionTranscoder;
  private final Transcoder<SingularityDisasterStats> disasterStatsTranscoder;

  @Inject
  public DisasterManager(CuratorFramework curator, SingularityConfiguration configuration, MetricRegistry metricRegistry,
                         Transcoder<SingularityDisabledAction> disabledActionTranscoder, Transcoder<SingularityDisasterStats> disasterStatsTranscoder) {
    super(curator, configuration, metricRegistry);
    this.disabledActionTranscoder = disabledActionTranscoder;
    this.disasterStatsTranscoder = disasterStatsTranscoder;
  }

  private String getActionPath(SingularityDisabledActionType action) {
    return ZKPaths.makePath(DISABLED_ACTIONS, action.name());
  }

  public boolean isDisabled(SingularityDisabledActionType action) {
    return exists(getActionPath(action));
  }

  public SingularityDisabledAction getDisabledAction(SingularityDisabledActionType action) {
    Optional<SingularityDisabledAction> maybeDisabledAction = getData(getActionPath(action), disabledActionTranscoder);
    return maybeDisabledAction.or(new SingularityDisabledAction(action, String.format(MESSAGE_FORMAT, action, DEFAULT_MESSAGE), Optional.<String>absent(), false));
  }

  public SingularityCreateResult disable(SingularityDisabledActionType action, Optional<String> maybeMessage, Optional<SingularityUser> user, boolean systemGenerated) {
    SingularityDisabledAction disabledAction = new SingularityDisabledAction(
      action,
      String.format(MESSAGE_FORMAT, action, maybeMessage.or(DEFAULT_MESSAGE)),
      user.isPresent() ? Optional.of(user.get().getId()) : Optional.<String>absent(),
      systemGenerated);

    return save(getActionPath(action), disabledAction, disabledActionTranscoder);
  }

  public SingularityDeleteResult enable(SingularityDisabledActionType action) {
    return delete(getActionPath(action));
  }

  public List<SingularityDisabledAction> getDisabledActions() {
    List<String> paths = new ArrayList<>();
    for (String path : getChildren(DISABLED_ACTIONS)) {
      paths.add(ZKPaths.makePath(DISABLED_ACTIONS, path));
    }

    return getAsync(DISABLED_ACTIONS, paths, disabledActionTranscoder);
  }

  public void addDisaster(SingularityDisasterType disaster) {
    create(ZKPaths.makePath(ACTIVE_DISASTERS, disaster.name()));
  }

  public void removeDisaster(SingularityDisasterType disaster) {
    delete(ZKPaths.makePath(ACTIVE_DISASTERS, disaster.name()));
  }

  public List<SingularityDisasterType> getActiveDisasters() {
    List<String> disasterNames = getChildren(ACTIVE_DISASTERS);
    List<SingularityDisasterType> disasters = new ArrayList<>();
    for (String name : disasterNames) {
      disasters.add(SingularityDisasterType.valueOf(name));
    }
    return disasters;
  }

  public void saveDisasterStats(SingularityDisasterStats stats) {
    save(DISASTER_STATS, stats, disasterStatsTranscoder);
  }

  public Optional<SingularityDisasterStats> getDisasterStats() {
    return getData(DISASTER_STATS, disasterStatsTranscoder);
  }
}
