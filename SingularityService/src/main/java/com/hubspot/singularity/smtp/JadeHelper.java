package com.hubspot.singularity.smtp;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;

import de.neuland.jade4j.template.TemplateLoader;

public final class JadeHelper {

  private JadeHelper() {
    throw new AssertionError("do not instantiate");
  }

  public static final TemplateLoader JADE_LOADER = new TemplateLoader() {

    @Override
    public Reader getReader(String name) throws IOException {
      return new InputStreamReader(ClassLoader.getSystemResourceAsStream(name), StandardCharsets.UTF_8);
    }

    @Override
    public long getLastModified(String name) throws IOException {
      return -1;
    }
  };

  private static final String TASK_DATE_PATTERN = "MMM dd HH:mm:ss";

  public static List<Map<String, String>> getJadeTaskHistory(Collection<SingularityTaskHistoryUpdate> taskHistory) {
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

}
