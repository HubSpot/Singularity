package com.hubspot.singularity.notifications;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityDisastersData;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.api.SingularityPauseRequest;
import com.hubspot.singularity.api.SingularityScaleRequest;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.smtp.SmtpMailNotifier;

@Singleton
public class SingularityIntercom implements SingularityNotifier {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityIntercom.class);

  private final Set<SingularityNotifier> notifiers;

  @Inject
  public SingularityIntercom(
      Provider<SmtpMailNotifier> mailer,
      Provider<SlackNotifier> slacker,
      SingularityConfiguration conf
  ) {
    notifiers = ImmutableSet.of(
        conf.getSmtpConfigurationOptional().isPresent() ? mailer.get() : new NoopNotifier(SmtpMailNotifier.class),
        conf.getSlackConfigurationOptional().isPresent() ? slacker.get() : new NoopNotifier(SlackNotifier.class)
    );
  }

  @Override
  public void sendTaskOverdueNotification(Optional<SingularityTask> task, SingularityTaskId taskId, SingularityRequest request, long runTime, long expectedRuntime) {
    for (SingularityNotifier notifier : notifiers) {
      if (notifier.shouldNotify(request)) {
        catchLogAndIgnoreThrowables(() -> notifier.sendTaskOverdueNotification(task, taskId, request, runTime, expectedRuntime));
      }
    }
  }

  @Override
  public void sendTaskFinishedNotification(SingularityTaskHistory taskHistory, SingularityRequest request) {
    for (SingularityNotifier notifier : notifiers) {
      if (notifier.shouldNotify(request)) {
        catchLogAndIgnoreThrowables(() -> notifier.sendTaskFinishedNotification(taskHistory, request));
      }
    }
  }

  @Override
  public void sendRequestPausedNotification(SingularityRequest request, Optional<SingularityPauseRequest> pauseRequest, Optional<String> user) {
    for (SingularityNotifier notifier : notifiers) {
      if (notifier.shouldNotify(request)) {
        catchLogAndIgnoreThrowables(() -> notifier.sendRequestPausedNotification(request, pauseRequest, user));
      }
    }
  }

  @Override
  public void sendRequestUnpausedNotification(SingularityRequest request, Optional<String> user, Optional<String> message) {
    for (SingularityNotifier notifier : notifiers) {
      if (notifier.shouldNotify(request)) {
        catchLogAndIgnoreThrowables(() -> notifier.sendRequestUnpausedNotification(request, user, message));
      }
    }
  }

  @Override
  public void sendRequestScaledNotification(SingularityRequest request, Optional<SingularityScaleRequest> newScaleRequest, Optional<Integer> formerInstances, Optional<String> user) {
    for (SingularityNotifier notifier : notifiers) {
      if (notifier.shouldNotify(request)) {
        catchLogAndIgnoreThrowables(() -> notifier.sendRequestScaledNotification(request, newScaleRequest, formerInstances, user));
      }
    }
  }

  @Override
  public void sendRequestRemovedNotification(SingularityRequest request, Optional<String> user, Optional<String> message) {
    for (SingularityNotifier notifier : notifiers) {
      if (notifier.shouldNotify(request)) {
        catchLogAndIgnoreThrowables(() -> sendRequestRemovedNotification(request, user, message));
      }
    }
  }

  @Override
  public void sendRequestInCooldownNotification(SingularityRequest request) {
    for (SingularityNotifier notifier : notifiers) {
      if (notifier.shouldNotify(request)) {
        catchLogAndIgnoreThrowables(() -> notifier.sendRequestInCooldownNotification(request));
      }
    }
  }

  @Override
  public void sendDisasterNotification(SingularityDisastersData disastersData) {
    notifiers.forEach(
        notifier -> catchLogAndIgnoreThrowables(() -> sendDisasterNotification(disastersData))
    );
  }

  @Override
  public boolean shouldNotify(
      SingularityRequest request
  ) {
    return notifiers
        .stream()
        .anyMatch(notifier -> notifier.shouldNotify(request));
  }

  private void catchLogAndIgnoreThrowables(Runnable runnable) {
    try {
      runnable.run();
    } catch (Throwable t) {
      LOG.error("Hit error when trying to send notification", t);
    }
  }


}
