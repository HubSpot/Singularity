package com.hubspot.singularity.smtp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.helpers.MesosUtils;
import com.hubspot.mesos.json.MesosFileChunkObject;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.SingularityDisasterDataPoint;
import com.hubspot.singularity.SingularityEmailType;
import com.hubspot.singularity.SingularityMailDisasterDataPoint;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskMetadata;
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
    this.uiBaseUrl = singularityConfiguration.getSmtpConfigurationOptional().isPresent() ?
        singularityConfiguration.getSmtpConfiguration().getUiBaseUrl().or(singularityConfiguration.getUiConfiguration().getBaseUrl()) :
        singularityConfiguration.getUiConfiguration().getBaseUrl();
    this.sandboxManager = sandboxManager;
    this.smtpConfiguration = singularityConfiguration.getSmtpConfigurationOptional();
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

  public List<SingularityMailDisasterDataPoint> getJadeDisasterStats(Collection<SingularityDisasterDataPoint> stats) {
    List<SingularityMailDisasterDataPoint> mailStats = new ArrayList<>();
    for (SingularityDisasterDataPoint stat : stats) {
      mailStats.add(new SingularityMailDisasterDataPoint(humanizeTimestamp(stat.getTimestamp()), stat));
    }
    return mailStats;
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

  private Optional<Pattern> getLogErrorRegex(final Optional<SingularityTask> task) {
    Optional<String> maybeRegex;
    SMTPConfiguration configuration = smtpConfiguration.get();
    if (task.isPresent() && task.get().getTaskRequest().getRequest().getTaskLogErrorRegex().isPresent()
    && !task.get().getTaskRequest().getRequest().getTaskLogErrorRegex().get().equals("")) {
      maybeRegex = task.get().getTaskRequest().getRequest().getTaskLogErrorRegex();
    } else {
      maybeRegex = configuration.getTaskLogErrorRegex();
    }
    if (!maybeRegex.isPresent()) {
      LOG.trace("No task log error regex provided.");
      return Optional.absent();
    }
    String regex = maybeRegex.get();

    Boolean caseSensitive;
    if (task.isPresent() && task.get().getTaskRequest().getRequest().getTaskLogErrorRegexCaseSensitive().isPresent()) {
      caseSensitive = task.get().getTaskRequest().getRequest().getTaskLogErrorRegexCaseSensitive().get();
    } else if (configuration.getTaskLogErrorRegexCaseSensitive().isPresent()) {
      caseSensitive = configuration.getTaskLogErrorRegexCaseSensitive().get();
    } else {
      caseSensitive = true;
    }

    try {
      if (caseSensitive) {
        return Optional.of(Pattern.compile(regex));
      } else {
        return Optional.of(Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
      }
    } catch (PatternSyntaxException e) {
      LOG.error("Invalid task log error regex supplied: \"{}\". Received exception: {}", regex, e);
      return Optional.absent();
    }
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

    final String slaveHostname = task.get().getHostname();

    final String fullPath = String.format("%s/%s", directory.get(), filename);

    final Long logLength = (long) smtpConfiguration.get().getTaskLogLength();

    final Optional<MesosFileChunkObject> logChunkObject;

    LOG.trace("Getting offset (maybe) for task {} file {}", taskId.getId(), fullPath);
    Optional<Long> maybeOffset = getMaybeTaskLogReadOffset(slaveHostname, fullPath, logLength, task);

    if (!maybeOffset.isPresent()) {
      LOG.trace("Failed to find logs or error finding logs for task {} file {}", taskId.getId(), fullPath);
      return Optional.absent();
    }
    try {
      logChunkObject = sandboxManager.read(slaveHostname, fullPath, maybeOffset, Optional.of(logLength));
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

  // Searches through the file, looking for first error
  private Optional<Long> getMaybeTaskLogReadOffset(final String slaveHostname, final String fullPath, final Long logLength, Optional<SingularityTask> task) {
    long offset = 0;
    long maxOffset = smtpConfiguration.get().getMaxTaskLogSearchOffset();
    Optional<Pattern> maybePattern = getLogErrorRegex(task);
    Pattern pattern;
    if (maybePattern.isPresent()) {
      pattern = maybePattern.get();
    } else {
      LOG.trace("Could not get regex pattern. Reading from bottom of file instead.");
      return getMaybeTaskLogEndOfFileOffset(slaveHostname, fullPath, logLength);
    }
    long length = logLength + pattern.toString().length(); // Get extra so that we can be sure to find the error
    Optional<MesosFileChunkObject> logChunkObject;
    Optional<MesosFileChunkObject> previous = Optional.absent();

    while (offset <= maxOffset) {
      try {
        logChunkObject = sandboxManager.read(slaveHostname, fullPath, Optional.of(offset), Optional.of(length));
      } catch (RuntimeException e) {
        LOG.error("Sandboxmanager failed to read {} on slave {}", fullPath, slaveHostname, e);
        return Optional.absent();
      }
      if (logChunkObject.isPresent()) {
        if (logChunkObject.get().getData().equals("")) { // Passed end of file
          if (previous.isPresent()) { // If there was any log, get the bottom bytes of it
            long end = previous.get().getOffset() + previous.get().getData().length();
            return Optional.of(end - logLength);
          }
          return Optional.absent();
        }
        Matcher matcher = pattern.matcher(logChunkObject.get().getData());
        if (matcher.find()) {
          return Optional.of(offset + matcher.start());
        } else {
          offset += logLength;
        }
      } else { // Couldn't read anything
        LOG.error("Failed to read log file {}", fullPath);
        return Optional.absent();
      }
      previous = logChunkObject;
    }
    LOG.trace("Searched through the first {} bytes of file {} and didn't find an error. Tailing bottom of file instead", maxOffset, fullPath);
    return getMaybeTaskLogEndOfFileOffset(slaveHostname, fullPath, logLength);
  }

  private Optional<Long> getMaybeTaskLogEndOfFileOffset(final String slaveHostname, final String fullPath, final long logLength) {
    Optional<MesosFileChunkObject> logChunkObject;
    try {
      logChunkObject = sandboxManager.read(slaveHostname, fullPath, Optional.<Long>absent(), Optional.<Long>absent());
    } catch (RuntimeException e) {
      LOG.error("Sandboxmanager failed to read {} on slave {}", fullPath, slaveHostname, e);
      return Optional.absent();
    }
    if (logChunkObject.isPresent()) {
      return Optional.of(Math.max(0, logChunkObject.get().getOffset() - logLength));
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
