package com.hubspot.singularity;

public enum SingularityAction {
  BOUNCE_REQUEST(true), SCALE_REQUEST(true), REMOVE_REQUEST(true), CREATE_REQUEST(true), UPDATE_REQUEST(true), VIEW_REQUEST(false), PAUSE_REQUEST(false),
  KILL_TASK(true), BOUNCE_TASK(true), RUN_SHELL_COMMAND(true), ADD_METADATA(true),
  DEPLOY(true), CANCEL_DEPLOY(true),
  ADD_WEBHOOK(true), REMOVE_WEBHOOK(true), VIEW_WEBHOOKS(false),
  TASK_RECONCILIATION(true), STARTUP_TASK_RECONCILIATION(true),
  RUN_HEALTH_CHECKS(true),
  ADD_DISASTER(false), REMOVE_DISASTER(false), DISABLE_ACTION(false), ENABLE_ACTION(false), VIEW_DISASTERS(false),
  FREEZE_SLAVE(true), ACTIVATE_SLAVE(true), DECOMMISSION_SLAVE(true), VIEW_SLAVES(false),
  FREEZE_RACK(true), ACTIVATE_RACK(true), DECOMMISSION_RACK(true), VIEW_RACKS(false),
  SEND_EMAIL(true),
  PROCESS_OFFERS(true), CACHE_OFFERS(true), EXPENSIVE_API_CALLS(true),
  RUN_CLEANUP_POLLER(true), RUN_DEPLOY_POLLER(true), RUN_SCHEDULER_POLLER(true), RUN_EXPIRING_ACTION_POLLER(true);

  private final boolean canDisable;

  SingularityAction(boolean canDisable) {
    this.canDisable = canDisable;
  }

  public boolean isCanDisable() {
    return canDisable;
  }
}
