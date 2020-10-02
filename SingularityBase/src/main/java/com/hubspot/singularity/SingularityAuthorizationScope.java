package com.hubspot.singularity;

public enum SingularityAuthorizationScope {
  READ,
  WRITE,
  ADMIN,
  DEPLOY,
  EXEC // run, bounce, kill, pause, scale
}
