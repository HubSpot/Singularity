package com.hubspot.singularity.config;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Optional;

@JsonIgnoreProperties( ignoreUnknown = true )
public class MesosConfiguration {

  @NotNull
  private String master;
  @NotNull
  private String frameworkName;
  @NotNull
  private String frameworkId;

  private double frameworkFailoverTimeout = 0.0;

  private int defaultCpus = 1;

  private int defaultMemory = 64;

  private int defaultDisk = 0;

  private boolean checkpoint = true;

  private Optional<String> frameworkRole = Optional.absent();

  @NotNull
  private String frameworkUser = "root";

  @NotNull
  private String rackIdAttributeKey = "rackid";

  @NotNull
  private String defaultRackId = "DEFAULT";

  private int slaveHttpPort = 5051;

  private Optional<Integer> slaveHttpsPort = Optional.absent();

  private int maxNumInstancesPerRequest = 25;
  private int maxNumCpusPerInstance = 50;
  private int maxNumCpusPerRequest = 900;
  private int maxMemoryMbPerInstance = 24000;
  private int maxMemoryMbPerRequest = 450000;
  private int maxDiskMbPerInstance = 60000;
  private int maxDiskMbPerRequest = 3000000;

  private Optional<String> credentialPrincipal = Optional.absent();
  private Optional<String> credentialSecret = Optional.absent();

  private long rxEventBufferSize = 10000;

  public int getMaxNumInstancesPerRequest() {
    return maxNumInstancesPerRequest;
  }

  public void setMaxNumInstancesPerRequest(int maxNumInstancesPerRequest) {
    this.maxNumInstancesPerRequest = maxNumInstancesPerRequest;
  }

  public int getMaxNumCpusPerInstance() {
    return maxNumCpusPerInstance;
  }

  public void setMaxNumCpusPerInstance(int maxNumCpusPerInstance) {
    this.maxNumCpusPerInstance = maxNumCpusPerInstance;
  }

  public int getMaxNumCpusPerRequest() {
    return maxNumCpusPerRequest;
  }

  public void setMaxNumCpusPerRequest(int maxNumCpusPerRequest) {
    this.maxNumCpusPerRequest = maxNumCpusPerRequest;
  }

  public int getMaxMemoryMbPerInstance() {
    return maxMemoryMbPerInstance;
  }

  public void setMaxMemoryMbPerInstance(int maxMemoryMbPerInstance) {
    this.maxMemoryMbPerInstance = maxMemoryMbPerInstance;
  }

  public int getMaxMemoryMbPerRequest() {
    return maxMemoryMbPerRequest;
  }

  public void setMaxDiskMbPerInstance(int maxDiskMbPerInstance) {
    this.maxDiskMbPerInstance = maxDiskMbPerInstance;
  }

  public int getMaxDiskMbPerInstance() {
    return maxDiskMbPerInstance;
  }

  public void setMaxDiskMbPerRequest(int maxDiskMbPerRequest) {
    this.maxDiskMbPerRequest = maxDiskMbPerRequest;
  }

  public int getMaxDiskMbPerRequest() {
    return maxDiskMbPerRequest;
  }

  public void setMaxMemoryMbPerRequest(int maxMemoryMbPerRequest) {
    this.maxMemoryMbPerRequest = maxMemoryMbPerRequest;
  }

  public String getRackIdAttributeKey() {
    return rackIdAttributeKey;
  }

  public String getDefaultRackId() {
    return defaultRackId;
  }

  public void setDefaultRackId(String defaultRackId) {
    this.defaultRackId = defaultRackId;
  }

  public void setRackIdAttributeKey(String rackIdAttributeKey) {
    this.rackIdAttributeKey = rackIdAttributeKey;
  }

  public String getMaster() {
    return master;
  }

  public String getFrameworkId() {
    return frameworkId;
  }

  public void setFrameworkId(String frameworkId) {
    this.frameworkId = frameworkId;
  }

  public String getFrameworkName() {
    return frameworkName;
  }

  public void setFrameworkName(String frameworkName) {
    this.frameworkName = frameworkName;
  }

  public double getFrameworkFailoverTimeout() {
    return frameworkFailoverTimeout;
  }

  public void setFrameworkFailoverTimeout(double frameworkFailoverTimeout) {
    this.frameworkFailoverTimeout = frameworkFailoverTimeout;
  }

  public Optional<String> getFrameworkRole() {
    return frameworkRole;
  }

  public void setFrameworkRole(Optional<String> frameworkRole) {
    this.frameworkRole = frameworkRole;
  }

  public void setMaster(String master) {
    this.master = master;
  }

  public boolean isCheckpoint() {
    return checkpoint;
  }

  public void setCheckpoint(boolean checkpoint) {
    this.checkpoint = checkpoint;
  }

  public int getDefaultCpus() {
    return defaultCpus;
  }

  public void setDefaultCpus(int defaultCpus) {
    this.defaultCpus = defaultCpus;
  }

  public int getDefaultMemory() {
    return defaultMemory;
  }

  public void setDefaultMemory(int defaultMemory) {
    this.defaultMemory = defaultMemory;
  }

  public int getSlaveHttpPort() {
    return slaveHttpPort;
  }

  public void setSlaveHttpPort(int slaveHttpPort) {
    this.slaveHttpPort = slaveHttpPort;
  }

  public Optional<Integer> getSlaveHttpsPort() {
    return slaveHttpsPort;
  }

  public void setSlaveHttpsPort(Optional<Integer> slaveHttpsPort) {
    this.slaveHttpsPort = slaveHttpsPort;
  }

  public Optional<String> getCredentialPrincipal() {
    return credentialPrincipal;
  }

  public void setCredentialPrincipal(Optional<String> credentialPrincipal) {
    this.credentialPrincipal = credentialPrincipal;
  }

  public Optional<String> getCredentialSecret() {
    return credentialSecret;
  }

  public void setCredentialSecret(Optional<String> credentialSecret) {
    this.credentialSecret = credentialSecret;
  }

  public int getDefaultDisk() {
    return defaultDisk;
  }

  public void setDefaultDisk(int defaultDisk) {
    this.defaultDisk = defaultDisk;
  }

  public String getFrameworkUser() {
    return frameworkUser;
  }

  public void setFrameworkUser(String frameworkUser) {
    this.frameworkUser = frameworkUser;
  }

  public long getRxEventBufferSize() {
    return rxEventBufferSize;
  }

  public MesosConfiguration setRxEventBufferSize(long rxEventBufferSize) {
    this.rxEventBufferSize = rxEventBufferSize;
    return this;
  }
}
