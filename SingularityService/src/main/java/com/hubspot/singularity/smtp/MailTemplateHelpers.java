package com.hubspot.singularity.smtp;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.SMTPConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.SandboxManager;

/**
 * Helpers that get passed to the Jade renderer. These helpers manipulate information given to the
 * Jade context into different formats.
 */
@Singleton
public class MailTemplateHelpers {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityMailer.class);

  /**
   * For fetching log files from mesos slaves.
   */
  private final SandboxManager sandboxManager;

  private static final String TASK_DATE_PATTERN = "MMM dd HH:mm:ss";
  private static final String TASK_LINK_FORMAT = "%s/task/%s";
  private static final String REQUEST_LINK_FORMAT = "%s/request/%s";
  private static final String LOG_LINK_FORMAT = "%s/task/%s/tail/%s";

  /**
   * Used to generate links, if no String is present, helpers will return empty Strings.
   */
  private final Optional<String> uiBaseUrl;
  private final Optional<SMTPConfiguration> smtpConfiguration;

  @Inject
  public MailTemplateHelpers(SandboxManager sandboxManager, SingularityConfiguration singularityConfiguration) {
    this.uiBaseUrl = singularityConfiguration.getUiConfiguration().getBaseUrl();
    this.sandboxManager = sandboxManager;
    this.smtpConfiguration = singularityConfiguration.getSmtpConfiguration();
  }

  /**
   * Format taskHistory information into a Map for Jade to generate a table from.
   *
   * @param taskHistory task history information.
   * @return map for Jade to pull "date", "update", and "message" information from.
   */
  public List<SingularityMailTaskHistoryUpdate> getJadeTaskHistory(Collection<SingularityTaskHistoryUpdate> taskHistory) {
    List<SingularityMailTaskHistoryUpdate> output = Lists.newArrayListWithCapacity(taskHistory.size());

    for (SingularityTaskHistoryUpdate taskUpdate : taskHistory) {
      output.add(
          new SingularityMailTaskHistoryUpdate(
              DateFormatUtils.formatUTC(taskUpdate.getTimestamp(), TASK_DATE_PATTERN), // date
              WordUtils.capitalize(taskUpdate.getTaskState().getDisplayName()), // update
              taskUpdate.getStatusMessage().or(""))); // message
    }

    return output;
  }

  /**
   * Format task logs into a List of SingularityMailTaskLog for Jade to interpret easily.
   *
   * @param taskId    task to get logs from.
   * @param task      task object to get logs from.
   * @param directory directory to read log from.
   * @return Jade interpretable List of SingularityMailTaskLog.
   */
  public List<SingularityMailTaskLog> getTaskLogs(SingularityTaskId taskId, Optional<SingularityTask> task, Optional<String> directory) {
    // No configuration for SMTP, what did you do to execute this?
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
              filePath, // path
              getFileName(filePath), // file
              getSingularityLogLink(filePath, taskId.getId()), // link
              getTaskLogFile(taskId, filePath, task, directory).or(""))); // log
    }

    return logTails;
  }

  /**
   * Get a log file from a remote mesos slave.
   *
   * @param taskId    id of the task.
   * @param filename  log file name.
   * @param task      required for method to retrieve task logs properly.
   * @param directory directory to read log from.
   * @return string of the log file.
   */
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

    try {
      logChunkObject = sandboxManager.read(slaveHostname, fullPath, Optional.of(0L), Optional.of(logLength));
    } catch (RuntimeException e) {
      LOG.error("Sandboxmanager failed to read {}/{} on slave {}", directory.get(), filename, slaveHostname, e);
      return Optional.absent();
    }

    if (logChunkObject.isPresent()) {
      return Optional.of(logChunkObject.get().getData());
    } else {
      LOG.error("Singularity mailer failed to get {} log for {} task ", filename, taskId.getId());
      return Optional.absent();
    }
  }

  /**
   * Get the file name from the file path.
   *
   * @param path file path string.
   */
  public String getFileName(String path) {
    String[] splitPath = path.split("/");
    return splitPath[splitPath.length - 1];
  }

  /**
   * Get a working link to the SingularityUI page for the given taskId.
   *
   * @param taskId which task to link to.
   * @return link.
   */
  public String getSingularityTaskLink(String taskId) {
    if (!uiBaseUrl.isPresent()) {
      return "";
    }

    return String.format(TASK_LINK_FORMAT, uiBaseUrl.get(), taskId);
  }

  /**
   * Get a working link to the SingularityUI page for the given requestId.
   *
   * @param requestId which request to go to.
   * @return link.
   */
  public String getSingularityRequestLink(String requestId) {
    if (!uiBaseUrl.isPresent()) {
      return "";
    }

    return String.format(REQUEST_LINK_FORMAT, uiBaseUrl.get(), requestId);
  }

  /**
   * Get a working link to the SingularityUI task log tail page.
   *
   * @param logPath path of the log file.
   * @param taskId  under which task should this look to find the log file.
   * @return link.
   */
  public String getSingularityLogLink(String logPath, String taskId) {
    if (!uiBaseUrl.isPresent()) {
      return "";
    }

    return String.format(LOG_LINK_FORMAT, uiBaseUrl.get(), taskId, logPath);
  }

  /**
   * Get a subject line for the task email based on the task history.
   *
   * @param taskId  SingularityTaskId.
   * @param state   detailed task state information.
   * @param type    email purpose.
   * @param history task history.
   * @return subject line string.
   */
  public String getSubjectForTaskHistory(SingularityTaskId taskId, ExtendedTaskState state, SingularityEmailType type, Collection<SingularityTaskHistoryUpdate> history) {
    if (type == SingularityEmailType.TASK_SCHEDULED_OVERDUE_TO_FINISH) {
      return String.format("Task is overdue to finish (%s)", taskId.toString());
    }

    if (!didTaskRun(history)) {
      return String.format("Task never started and was %s (%s)", state.getDisplayName(), taskId.toString());
    }

    return String.format("Task %s (%s)", state.getDisplayName(), taskId.toString());
  }

  /**
   * From a task's history, determine if it ran.
   *
   * @param history task history.
   * @return whether the task ran.
   */
  public boolean didTaskRun(Collection<SingularityTaskHistoryUpdate> history) {
    SingularityTaskHistoryUpdate.SimplifiedTaskState simplifiedTaskState = SingularityTaskHistoryUpdate.getCurrentState(history);

    return simplifiedTaskState == SingularityTaskHistoryUpdate.SimplifiedTaskState.DONE || simplifiedTaskState == SingularityTaskHistoryUpdate.SimplifiedTaskState.RUNNING;
  }
}
