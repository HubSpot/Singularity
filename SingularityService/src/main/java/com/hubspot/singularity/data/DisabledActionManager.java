package com.hubspot.singularity.data;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.Transcoder;

public class DisabledActionManager extends CuratorAsyncManager {
  private static final String DISABLED_ACTIONS_ROOT = "/disabled-actions";

  private static final String MESSAGE_FORMAT = "Cannot perform action %s: %s";
  private static final String DEFAULT_MESSAGE = "Action is currently disabled";

  private final Transcoder<SingularityDisabledAction> disabledActionTranscoder;

  @Inject
  public DisabledActionManager(CuratorFramework curator, SingularityConfiguration configuration, MetricRegistry metricRegistry, Transcoder<SingularityDisabledAction> disabledActionTranscoder) {
    super(curator, configuration, metricRegistry);
    this.disabledActionTranscoder = disabledActionTranscoder;
  }

  private String getActionPath(SingularityDisabledActionType action) {
    return ZKPaths.makePath(DISABLED_ACTIONS_ROOT, action.name());
  }

  public boolean isDisabled(SingularityDisabledActionType action) {
    return exists(getActionPath(action));
  }

  public SingularityDisabledAction getDisabledAction(SingularityDisabledActionType action) {
    Optional<SingularityDisabledAction> maybeDisabledAction = getData(getActionPath(action), disabledActionTranscoder);
    return maybeDisabledAction.or(new SingularityDisabledAction(action, String.format(MESSAGE_FORMAT, action, DEFAULT_MESSAGE), Optional.<String>absent()));
  }

  public SingularityCreateResult disable(SingularityDisabledActionType action, Optional<String> maybeMessage, Optional<SingularityUser> user) {
    SingularityDisabledAction disabledAction = new SingularityDisabledAction(
      action,
      String.format(MESSAGE_FORMAT, action, maybeMessage.or(DEFAULT_MESSAGE)),
      user.isPresent() ? Optional.of(user.get().getId()) : Optional.<String>absent());

    return save(getActionPath(action), disabledAction, disabledActionTranscoder);
  }

  public SingularityDeleteResult enable(SingularityDisabledActionType action) {
    return delete(getActionPath(action));
  }

  public List<SingularityDisabledAction> getDisabledActions() {
    List<String> paths = new ArrayList<>();
    for (String path : getChildren(DISABLED_ACTIONS_ROOT)) {
      paths.add(ZKPaths.makePath(DISABLED_ACTIONS_ROOT, path));
    }

    return getAsync(DISABLED_ACTIONS_ROOT, paths, disabledActionTranscoder);
  }
}
