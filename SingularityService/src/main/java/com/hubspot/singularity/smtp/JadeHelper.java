package com.hubspot.singularity.smtp;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang.time.DateFormatUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;

import de.neuland.jade4j.template.TemplateLoader;

public class JadeHelper {

  public static TemplateLoader JADE_LOADER = new TemplateLoader() {
    
    @Override
    public Reader getReader(String name) throws IOException {
      return new InputStreamReader(ClassLoader.getSystemResourceAsStream(name));
    }
    
    @Override
    public long getLastModified(String name) throws IOException {
      return -1;
    }
  };

  private static final String TASK_DATE_PATTERN = "MMM dd HH:mm:ss";
  
  public List<Map<String, String>> getJadeTaskHistory(Collection<SingularityTaskHistoryUpdate> taskHistory) {
    List<Map<String, String>> output = Lists.newArrayListWithCapacity(taskHistory.size());
    
    for (SingularityTaskHistoryUpdate taskUpdate : Ordering.natural().sortedCopy(taskHistory)) {
      output.add(
          ImmutableMap.<String, String> builder()
          .put("date", DateFormatUtils.formatUTC(taskUpdate.getTimestamp(), TASK_DATE_PATTERN))
          .put("update", WordUtils.capitalize(taskUpdate.getTaskState().getDisplayName()))
          .put("message", taskUpdate.getStatusMessage().or(""))
          .build());
    }

    return output;
  }

  public Map<String, String> getJadeRequestHistory(SingularityRequestHistory requestHistory) {
    Date createdFormatted = new Date(requestHistory.getCreatedAt());

    Map<String, String> formatted = Maps.newHashMap();
    formatted.put("state", requestHistory.getState().name());
    formatted.put("date", createdFormatted.toString());
    formatted.put("user", requestHistory.getUser().orNull());
    formatted.put("request_id", requestHistory.getRequest().getId());

    return formatted;
  }

}
