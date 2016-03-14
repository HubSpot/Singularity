package com.hubspot.singularity.smtp;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.mesos.json.MesosFileChunkObject;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.SingularityEmailType;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskMetadata;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.SMTPConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.SandboxManager;

@Singleton
public class MailTemplateHelpers {

  private static final Logger LOG = LoggerFactory.getLogger(MailTemplateHelpers.class);

  private static final String TASK_LINK_FORMAT = "%s/task/%s";
  private static final String REQUEST_LINK_FORMAT = "%s/request/%s";
  private static final String LOG_LINK_FORMAT = "%s/task/%s/tail/%s";
  private static final String DEFAULT_TIMESTAMP_FORMAT = "MMM dd h:mm:ss a zzz";

  private final SandboxManager sandboxManager;

  private final Optional<String> uiBaseUrl;
  private final Optional<SMTPConfiguration> smtpConfiguration;
  private final Optional<String> taskDatePattern;
  private final Optional<TimeZone> timeZone;

  @Inject
  public MailTemplateHelpers(SandboxManager sandboxManager, SingularityConfiguration singularityConfiguration) {
    this.uiBaseUrl = singularityConfiguration.getUiConfiguration().getBaseUrl();
    this.sandboxManager = sandboxManager;
    this.smtpConfiguration = singularityConfiguration.getSmtpConfiguration();
    if (this.smtpConfiguration.isPresent()) {
      this.taskDatePattern = Optional.of(this.smtpConfiguration.get().getMailerDatePattern());
      this.timeZone = Optional.of(this.smtpConfiguration.get().getMailerTimeZone());
    } else {
      this.taskDatePattern = Optional.absent();
      this.timeZone = Optional.absent();
    }
  }

  public String humanizeTimestamp(long timestamp) {
    if (taskDatePattern.isPresent() && timeZone.isPresent()) {
      return DateFormatUtils.format(timestamp, taskDatePattern.get(), timeZone.get());
    } else if (taskDatePattern.isPresent()) {
      return DateFormatUtils.formatUTC(timestamp, taskDatePattern.get());
    } else if (timeZone.isPresent()) {
      return DateFormatUtils.format(timestamp, DEFAULT_TIMESTAMP_FORMAT, timeZone.get());
    } else {
      return DateFormatUtils.format(timestamp, DEFAULT_TIMESTAMP_FORMAT);
    }
  }

  public List<SingularityMailTaskMetadata> getJadeTaskMetadata(Collection<SingularityTaskMetadata> taskMetadata) {
    List<SingularityMailTaskMetadata> output = Lists.newArrayListWithCapacity(taskMetadata.size());

    for (SingularityTaskMetadata metadataElement : taskMetadata) {
      output.add(
          new SingularityMailTaskMetadata(
              humanizeTimestamp(metadataElement.getTimestamp()),
              metadataElement.getType(),
              metadataElement.getTitle(),
              metadataElement.getUser().or(""),
              metadataElement.getMessage().or(""),
              metadataElement.getLevel().toString()));
    }

    return output;
  }

  public List<SingularityMailTaskHistoryUpdate> getJadeTaskHistory(Collection<SingularityTaskHistoryUpdate> taskHistory) {
    List<SingularityMailTaskHistoryUpdate> output = Lists.newArrayListWithCapacity(taskHistory.size());

    for (SingularityTaskHistoryUpdate taskUpdate : taskHistory) {
      output.add(
          new SingularityMailTaskHistoryUpdate(
              humanizeTimestamp(taskUpdate.getTimestamp()),
              WordUtils.capitalize(taskUpdate.getTaskState().getDisplayName()),
              taskUpdate.getStatusMessage().or("")));
    }

    return output;
  }

  public List<SingularityMailTaskLog> getTaskLogs(SingularityTaskId taskId, Optional<SingularityTask> task, Optional<String> directory) {
    if (!smtpConfiguration.isPresent()) {
      LOG.warn("Tried to getTaskLogs for sending email without SMTP configuration set.");
      return Collections.emptyList();
    }

    List<String> taskEmailTailFiles = smtpConfiguration.get().getTaskEmailTailFiles();
    List<SingularityMailTaskLog> logTails = Lists.newArrayListWithCapacity(taskEmailTailFiles.size());

    for (String filePath : taskEmailTailFiles) {
      // To enable support for tailing the service.log file, replace instances of $MESOS_TASK_ID.
      filePath = filePath.replaceAll("\\$MESOS_TASK_ID", MesosUtils.getSafeTaskIdForDirectory(taskId.getId()));

      logTails.add(
          new SingularityMailTaskLog(
              filePath,
              getFileName(filePath),
              getSingularityLogLink(filePath, taskId.getId()),
              getTaskLogFile(taskId, filePath, task, directory).or("")));
    }

    return logTails;
  }

  private Optional<String> getTaskLogFile(final SingularityTaskId taskId, final String filename, final Optional<SingularityTask> task, final Optional<String> directory) {
    if (!smtpConfiguration.isPresent()) {
      LOG.warn("Tried to get a task log file without SMTP configuration set up.");
      return Optional.absent();
    }
    if (!task.isPresent() || !directory.isPresent()) {
      LOG.warn("Couldn't retrieve {} for {} because task ({}) or directory ({}) wasn't present", filename, taskId, task.isPresent(), directory.isPresent());
      return Optional.absent();
    }

    final String slaveHostname = task.get().getOffer().getHostname();

    final String fullPath = String.format("%s/%s", directory.get(), filename);

    final Long logLength = (long) smtpConfiguration.get().getTaskLogLength();

    final Optional<MesosFileChunkObject> logChunkObject;

    Optional<Long> maybeOffset = getMaybeOffset(slaveHostname, fullPath, filename, directory);

    if (!maybeOffset.isPresent()) {
      return Optional.absent();
    }
    try {
      logChunkObject = sandboxManager.read(slaveHostname, fullPath, Optional.of(Math.min(0, maybeOffset.get() - logLength)), Optional.of(logLength));
    } catch (RuntimeException e) {
      LOG.error("Sandboxmanager failed to read {}/{} on slave {}", directory.get(), filename, slaveHostname, e);
      return Optional.absent();
    }

    if (logChunkObject.isPresent()) {
      return Optional.of(logChunkObject.get().getData());
    } else {
      LOG.error("Failed to get {} log for {}", filename, taskId.getId());
      return Optional.absent();
    }
  }

  private Optional<Long> getMaybeOffset(String slaveHostname, String fullPath, String filename, Optional<String> directory) {
    Optional<MesosFileChunkObject> logChunkObject;
    try {
      logChunkObject = sandboxManager.read(slaveHostname, fullPath, Optional.<Long>absent(), Optional.<Long>absent());
    } catch (RuntimeException e) {
      LOG.error("Sandboxmanager failed to read {}/{} on slave {}", directory.get(), filename, slaveHostname, e);
      return Optional.absent();
    }
    if (logChunkObject.isPresent()) {
      return Optional.of(logChunkObject.get().getOffset());
    } else {
      LOG.error("Failed to get offset for log file {}", fullPath);
      return Optional.absent();
    }

  }

  public String getFileName(String path) {
    String[] splitPath = path.split("/");
    return splitPath[splitPath.length - 1];
  }

  public String getSingularityTaskLink(String taskId) {
    if (!uiBaseUrl.isPresent()) {
      return "";
    }

    return String.format(TASK_LINK_FORMAT, uiBaseUrl.get(), taskId);
  }

  public String getSingularityRequestLink(String requestId) {
    if (!uiBaseUrl.isPresent()) {
      return "";
    }

    return String.format(REQUEST_LINK_FORMAT, uiBaseUrl.get(), requestId);
  }

  public String getSingularityLogLink(String logPath, String taskId) {
    if (!uiBaseUrl.isPresent()) {
      return "";
    }

    return String.format(LOG_LINK_FORMAT, uiBaseUrl.get(), taskId, logPath);
  }

  public String getSubjectForTaskHistory(SingularityTaskId taskId, ExtendedTaskState state, SingularityEmailType type, Collection<SingularityTaskHistoryUpdate> history) {
    if (type == SingularityEmailType.TASK_SCHEDULED_OVERDUE_TO_FINISH) {
      return String.format("Task is overdue to finish (%s)", taskId.toString());
    }

    if (!didTaskRun(history)) {
      return String.format("Task never started and was %s (%s)", state.getDisplayName(), taskId.toString());
    }

    return String.format("Task %s (%s)", state.getDisplayName(), taskId.toString());
  }

  public boolean didTaskRun(Collection<SingularityTaskHistoryUpdate> history) {
    SingularityTaskHistoryUpdate.SimplifiedTaskState simplifiedTaskState = SingularityTaskHistoryUpdate.getCurrentState(history);

    return simplifiedTaskState == SingularityTaskHistoryUpdate.SimplifiedTaskState.DONE || simplifiedTaskState == SingularityTaskHistoryUpdate.SimplifiedTaskState.RUNNING;
  }
}
