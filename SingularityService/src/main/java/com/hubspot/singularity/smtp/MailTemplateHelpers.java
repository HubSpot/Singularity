package com.hubspot.singularity.smtp;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Helpers that get passed to the Jade renderer. These helpers manipulate information given to the
 * Jade context into different formats.
 */
public class MailTemplateHelpers {
  private static final String TASK_DATE_PATTERN = "MMM dd HH:mm:ss";
  private static final String TASK_LINK_FORMAT = "%s/task/%s";
  private static final String REQUEST_LINK_FORMAT = "%s/request/%s";
  private static final String LOG_LINK_FORMAT = "%s/task/%s/tail/%s";

  /**
   * Used to generate links, if no String is present, helpers will return empty Strings.
   */
  private final Optional<String> uiHostnameAndPath;

  public MailTemplateHelpers(Optional<String> uiHostnameAndPath) {
    this.uiHostnameAndPath = uiHostnameAndPath;
  }

  /**
   * Format taskHistory information into a Map for Jade to generate a table from.
   * @param taskHistory task history information.
   * @return map for Jade to pull "date", "update", and "message" information from.
   */
  public List<Map<String, String>> getJadeTaskHistory(Collection<SingularityTaskHistoryUpdate> taskHistory) {
    List<Map<String, String>> output = Lists.newArrayListWithCapacity(taskHistory.size());

    for (SingularityTaskHistoryUpdate taskUpdate : taskHistory) {
      output.add(
          ImmutableMap.<String, String> builder()
              .put("date", DateFormatUtils.formatUTC(taskUpdate.getTimestamp(), TASK_DATE_PATTERN))
              .put("update", WordUtils.capitalize(taskUpdate.getTaskState().getDisplayName()))
              .put("message", taskUpdate.getStatusMessage().or(""))
              .build());
    }

    return output;
  }

  /**
   * Get the file name from the file path.
   * @param path file path string.
   */
  public String getFileName(String path) {
    String[] splitPath = path.split("/");
    return splitPath[splitPath.length - 1];
  }

  /**
   * Get a working link to the SingularityUI page for the given taskId.
   * @param taskId which task to link to.
   * @return link.
   */
  public String getSingularityTaskLink(String taskId) {
    if (!uiHostnameAndPath.isPresent()) {
      return "";
    }

    return String.format(TASK_LINK_FORMAT, uiHostnameAndPath.get(), taskId);
  }

  /**
   * Get a working link to the SingularityUI page for the given requestId.
   * @param requestId which request to go to.
   * @return link.
   */
  public String getSingularityRequestLink(String requestId) {
    if (!uiHostnameAndPath.isPresent()) {
      return "";
    }

    return String.format(REQUEST_LINK_FORMAT, uiHostnameAndPath.get(), requestId);
  }

  /**
   * Get a working link to the SingularityUI task log tail page.
   * @param logPath path of the log file.
   * @param taskId under which task should this look to find the log file.
   * @return link.
   */
  public String getSingularityLogLink(String logPath, String taskId) {
    if (!uiHostnameAndPath.isPresent()) {
      return "";
    }

    return String.format(LOG_LINK_FORMAT, uiHostnameAndPath.get(), taskId, logPath);
  }
}
