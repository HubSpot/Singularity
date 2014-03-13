package com.hubspot.singularity.smtp;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;

public class JadeHelper {

  public List<Map<String, String>> getJadeTaskHistory(SingularityTaskHistory taskHistory) {
    List<Map<String, String>> output = Lists.newArrayList();

    for (SingularityTaskHistoryUpdate taskUpdate : taskHistory.getTaskUpdates()) {
      Map<String, String> formatted = Maps.newHashMap();
      Date date = new Date(taskUpdate.getTimestamp());
      formatted.put("date", date.toString());
      formatted.put("update", taskUpdate.getStatusUpdate());
      output.add(formatted);
    }

    return output;
  }

  // TODO send the deploy?
  public Map<String, String> getJadeRequestHistory(SingularityRequestHistory requestHistory) {
    Date createdFormatted = new Date(requestHistory.getCreatedAt());

    Map<String, String> formatted = Maps.newHashMap();
    formatted.put("state", requestHistory.getState());
    formatted.put("date", createdFormatted.toString());
    formatted.put("user", requestHistory.getUser().orNull());
    formatted.put("request_id", requestHistory.getRequest().getId());
//    formatted.put("request_cmd", request.getCommand());

    return formatted;
  }

  
  
}
