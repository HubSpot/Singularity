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
  private static final String DISABLED_ACTIONS_PATH = DISASTERS_ROOT + "/disabled-actions";
  private static final String ACTIVE_DISASTERS_PATH = DISASTERS_ROOT + "/active";
  private static final String DISASTER_STATS_PATH = DISASTERS_ROOT + "/stats";
  private static final String DISABLE_AUTOMATED_PATH = DISASTERS_ROOT + "/disabled";

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
    return ZKPaths.makePath(DISABLED_ACTIONS_PATH, action.name());
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
    for (String path : getChildren(DISABLED_ACTIONS_PATH)) {
      paths.add(ZKPaths.makePath(DISABLED_ACTIONS_PATH, path));
    }

    return getAsync(DISABLED_ACTIONS_PATH, paths, disabledActionTranscoder);
  }

  public void addDisaster(SingularityDisasterType disaster) {
    create(ZKPaths.makePath(ACTIVE_DISASTERS_PATH, disaster.name()));
  }

  public void removeDisaster(SingularityDisasterType disaster) {
    delete(ZKPaths.makePath(ACTIVE_DISASTERS_PATH, disaster.name()));
  }

  public List<SingularityDisasterType> getActiveDisasters() {
    List<String> disasterNames = getChildren(ACTIVE_DISASTERS_PATH);
    List<SingularityDisasterType> disasters = new ArrayList<>();
    for (String name : disasterNames) {
      disasters.add(SingularityDisasterType.valueOf(name));
    }
    return disasters;
  }

  public void saveDisasterStats(SingularityDisasterStats stats) {
    save(DISASTER_STATS_PATH, stats, disasterStatsTranscoder);
  }

  public Optional<SingularityDisasterStats> getDisasterStats() {
    return getData(DISASTER_STATS_PATH, disasterStatsTranscoder);
  }

  public void updateActiveDisasters(List<SingularityDisasterType> previouslyActiveDisasters, List<SingularityDisasterType> newActiveDisasters) {
    for (SingularityDisasterType disaster : previouslyActiveDisasters) {
      if (!newActiveDisasters.contains(disaster)) {
        removeDisaster(disaster);
      }
    }

    for (SingularityDisasterType disaster : newActiveDisasters) {
      addDisaster(disaster);
    }


  }

  public void addDisabledActionsForDisasters(List<SingularityDisasterType> newActiveDisasters) {
    String message = String.format("Active disasters detected: (%s)", newActiveDisasters);
    for (SingularityDisabledActionType action : configuration.getDisasterDetection().getDisableActionsOnDisaster()) {
      disable(action, Optional.of(message), Optional.<SingularityUser>absent(), true);
    }
  }

  public void clearSystemGeneratedDisabledActions() {
    for (SingularityDisabledAction disabledAction : getDisabledActions()) {
      if (disabledAction.isSystemGenerated()) {
        enable(disabledAction.getType());
      }
    }
  }

  public void disableAutomatedDisabledActions() {
    create(DISABLE_AUTOMATED_PATH);
  }

  public void enableAutomatedDisabledActions() {
    delete(DISABLE_AUTOMATED_PATH);
  }

  public boolean isAutomatedDisabledActionsDisabled() {
    return exists(DISABLE_AUTOMATED_PATH);
  }
}
