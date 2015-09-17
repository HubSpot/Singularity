package com.hubspot.singularity.data.zkmigrations;

import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;

import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.ScheduleType;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.data.transcoders.Transcoder;

@Singleton
public class ScheduleMigration extends ZkDataMigration  {

  private static final Logger LOG = LoggerFactory.getLogger(ScheduleMigration.class);

  private final RequestManager requestManager;
  private final SingularityValidator validator;
  private final CuratorFramework curator;
  private final Transcoder<SingularityRequestWithState> requestTranscoder;

  @Inject
  public ScheduleMigration(CuratorFramework curator, RequestManager requestManager, SingularityValidator validator, Transcoder<SingularityRequestWithState> requestTranscoder) {
    super(8);

    this.curator = curator;
    this.requestManager = requestManager;
    this.validator = validator;
    this.requestTranscoder = requestTranscoder;
  }

  @Override
  public void applyMigration() {
    LOG.info("Starting migration to fix certain CRON schedules");

    final long start = System.currentTimeMillis();
    int num = 0;

    for (SingularityRequestWithState requestWithState : requestManager.getRequests()) {

      if (requestWithState.getRequest().isScheduled()) {
        Optional<String> schedule = requestWithState.getRequest().getSchedule();
        Optional<String> quartzSchedule = requestWithState.getRequest().getQuartzSchedule();
        Optional<ScheduleType> scheduleType = requestWithState.getRequest().getScheduleType();

        if (scheduleType.isPresent() && scheduleType.get() != ScheduleType.CRON) {
          LOG.info("Skipping {}, it had schedule type: {}", requestWithState.getRequest().getId(), scheduleType.get());
          continue;
        }

        if (quartzSchedule.isPresent() && schedule.isPresent() && quartzSchedule.get().equals(schedule.get())) {
          LOG.info("Skipping {}, assuming it was quartz - it had quartz schedule == schedule {}", requestWithState.getRequest().getId(), schedule.get());
          continue;
        }

        if (!schedule.isPresent()) {
          LOG.info("Skipping {}, it had no schedule", requestWithState.getRequest().getId());
          continue;
        }

        String actualSchedule = schedule.get();

        String newQuartzSchedule = null;

        try {
          newQuartzSchedule = validator.getQuartzScheduleFromCronSchedule(actualSchedule);
        } catch (WebApplicationException e) {
          LOG.error("Failed to convert {} ({}) due to {}", requestWithState.getRequest().getId(), actualSchedule, e.getResponse().getEntity());
          continue;
        }

        if (quartzSchedule.isPresent() && quartzSchedule.get().equals(newQuartzSchedule)) {
          LOG.info("Skipping {}, migration had no effect {}", requestWithState.getRequest().getId(), newQuartzSchedule);
          continue;
        }

        SingularityRequest newRequest = requestWithState.getRequest().toBuilder().setQuartzSchedule(Optional.of(newQuartzSchedule)).build();

        try {
          LOG.info("Saving new schedule (quartz {} - from {}) for {}", newQuartzSchedule, actualSchedule, newRequest.getId());
          curator.setData().forPath("/requests/all/" + newRequest.getId(),
            requestTranscoder.toBytes(new SingularityRequestWithState(newRequest, requestWithState.getState(), requestWithState.getTimestamp())));
          num++;
        } catch (Throwable t) {
          LOG.error("Failed to write {}", newRequest.getId(), t);
          throw Throwables.propagate(t);
        }
      }
    }

    LOG.info("Applied {} in {}", num, JavaUtils.duration(start));
  }

}
