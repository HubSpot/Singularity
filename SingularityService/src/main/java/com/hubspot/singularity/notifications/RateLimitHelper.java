package com.hubspot.singularity.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityNotificationType;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.config.SMTPConfiguration;
import com.hubspot.singularity.data.MetadataManager;

@Singleton
public class RateLimitHelper {

  private static final Logger LOG = LoggerFactory.getLogger(RateLimitHelper.class);
  private final MetadataManager metadataManager;
  private final Optional<SMTPConfiguration> maybeEmailConf;

  @Inject
  public RateLimitHelper(
      MetadataManager metadataManager,
      Optional<SMTPConfiguration> maybeEmailConf
  ) {
    this.metadataManager = metadataManager;
    this.maybeEmailConf = maybeEmailConf;
  }

  public RateLimitStatus getCurrentRateLimitForMail(SingularityRequest request, SingularityNotificationType emailType) {
    if (!maybeEmailConf.isPresent()) {
      LOG.debug("Rate limiting disabled because no email configuration present");
      return RateLimitStatus.RATE_LIMITING_DISABLED;
    }
    SMTPConfiguration conf = maybeEmailConf.get();
    if (conf.getRateLimitAfterNotifications() < 1) {
      LOG.trace("Mail rate limit cooldown disabled");
      return RateLimitStatus.RATE_LIMITING_DISABLED;
    }

    final String requestId = request.getId();
    final String emailTypeName = emailType.name();

    final long now = System.currentTimeMillis();

    final Optional<String> cooldownMarker = metadataManager.getMailCooldownMarker(requestId, emailTypeName);

    if (cooldownMarker.isPresent()) {
      final long cooldownLeft = conf.getRateLimitCooldownMillis() - (now - Long.parseLong(cooldownMarker.get()));

      if (cooldownLeft > 0) {
        LOG.debug("Not sending {} for {} - mail cooldown has {} time left out of {}", emailTypeName, requestId, cooldownLeft, conf.getRateLimitCooldownMillis());
        return RateLimitStatus.RATE_LIMITED;
      }

      metadataManager.removeMailCooldown(requestId, emailTypeName);
    }

    return RateLimitStatus.NOT_RATE_LIMITED;
  }

  public RateLimitResult checkRateLimitForMail(SingularityRequest request, SingularityNotificationType emailType) {
    if (!maybeEmailConf.isPresent()) {
      LOG.debug("Rate limiting disabled because no email configuration present");
      return RateLimitResult.SEND_MAIL;
    }
    SMTPConfiguration conf = maybeEmailConf.get();
    RateLimitStatus currentStatus = getCurrentRateLimitForMail(request, emailType);

    switch (currentStatus) {
      case RATE_LIMITED:
        return RateLimitResult.DONT_SEND_MAIL_IN_COOLDOWN;
      case RATE_LIMITING_DISABLED:
        return RateLimitResult.SEND_MAIL;
      case NOT_RATE_LIMITED:
        break;
    }

    final String requestId = request.getId();
    final String emailTypeName = emailType.name();
    final long now = System.currentTimeMillis();

    metadataManager.saveMailRecord(request, emailType);

    int numInPeriod = 0;

    for (String recentMailRecord : metadataManager.getMailRecords(request.getId(), emailType.name())) {
      if (now - Long.parseLong(recentMailRecord) < conf.getRateLimitPeriodMillis()) {
        numInPeriod++;
      }
    }

    if (numInPeriod > conf.getRateLimitAfterNotifications()) {
      LOG.info("{} for {} sent at least {} times in {}, not sending this mail again for at least {}", emailTypeName, requestId, numInPeriod, conf.getRateLimitAfterNotifications(), conf.getRateLimitCooldownMillis());
      metadataManager.cooldownMail(requestId, emailTypeName);
      return RateLimitResult.SEND_COOLDOWN_STARTED_MAIL;
    }

    return RateLimitResult.SEND_MAIL;
  }
}
