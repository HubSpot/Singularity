package com.hubspot.singularity.config;

public class ApiPaths {
  public static final String API_BASE_PATH = "/api";

  public static final String AUTH_RESOURCE_PATH = API_BASE_PATH + "/auth";
  public static final String DEPLOY_RESOURCE_PATH = API_BASE_PATH + "/deploys";
  public static final String DISASTERS_RESOURCE_PATH = API_BASE_PATH + "/disasters";
  public static final String HISTORY_RESOURCE_PATH = API_BASE_PATH + "/history";
  public static final String INACTIVE_SLAVES_RESOURCE_PATH = API_BASE_PATH + "/inactive";
  public static final String METRICS_RESOURCE_PATH = API_BASE_PATH + "/metrics"; // Not implemented in proxy
  public static final String PRIORITY_RESOURCE_PATH = API_BASE_PATH + "/priority";
  public static final String RACK_RESOURCE_PATH = API_BASE_PATH + "/racks";
  public static final String REQUEST_GROUP_RESOURCE_PATH = API_BASE_PATH + "/groups";
  public static final String REQUEST_RESOURCE_PATH = API_BASE_PATH + "/requests";
  public static final String S3_LOG_RESOURCE_PATH = API_BASE_PATH + "/logs";
  public static final String SANDBOX_RESOURCE_PATH = API_BASE_PATH + "/sandbox";
  public static final String SLAVE_RESOURCE_PATH = API_BASE_PATH + "/slaves";
  public static final String STATE_RESOURCE_PATH = API_BASE_PATH + "/state";
  public static final String TASK_RESOURCE_PATH = API_BASE_PATH + "/tasks";
  public static final String TEST_RESOURCE_PATH = API_BASE_PATH + "/test"; // Not implemented in proxy
  public static final String TASK_TRACKER_RESOURCE_PATH = API_BASE_PATH + "/track";
  public static final String USAGE_RESOURCE_PATH = API_BASE_PATH + "/usage";
  public static final String USER_RESOURCE_PATH = API_BASE_PATH + "/users";
  public static final String WEBHOOK_RESOURCE_PATH = API_BASE_PATH + "/webhooks";

}
