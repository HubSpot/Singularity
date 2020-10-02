package com.hubspot.singularity.config;

import java.util.Collections;
import java.util.Set;
import org.hibernate.validator.constraints.NotEmpty;

public class ScopesConfiguration {
  @NotEmpty
  private Set<String> admin = Collections.singleton("SINGULARITY_ADMIN");

  @NotEmpty
  private Set<String> write = Collections.singleton("SINGULARITY_WRITE");

  @NotEmpty
  private Set<String> read = Collections.singleton("SINGULARITY_READONLY");

  @NotEmpty
  private Set<String> exec = Collections.singleton("SINGULARITY_EXEC");

  // only enforced if not empty, otherwise will check for write
  private Set<String> deploy = Collections.emptySet();

  public Set<String> getAdmin() {
    return admin;
  }

  public void setAdmin(Set<String> admin) {
    this.admin = admin;
  }

  public Set<String> getWrite() {
    return write;
  }

  public void setWrite(Set<String> write) {
    this.write = write;
  }

  public Set<String> getRead() {
    return read;
  }

  public void setRead(Set<String> read) {
    this.read = read;
  }

  public Set<String> getDeploy() {
    return deploy;
  }

  public void setDeploy(Set<String> deploy) {
    this.deploy = deploy;
  }

  public Set<String> getExec() {
    return exec;
  }

  public void setExec(Set<String> exec) {
    this.exec = exec;
  }
}
