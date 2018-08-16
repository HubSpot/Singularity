package com.hubspot.singularity.config;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Optional;
import com.hubspot.singularity.MachineLoadMetric;

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

  private long rxEventBufferSize = 10000;
  private int statusUpdateConcurrencyLimit = 500;
  private int maxStatusUpdateQueueSize = 5000;
  private int offersConcurrencyLimit = 100;
  private MachineLoadMetric scoreUsingSystemLoad = MachineLoadMetric.LOAD_5;
  private double allocatedResourceWeight = 0.5;
  private double inUseResourceWeight = 0.5;
  private double cpuWeight = 0.4;
  private double memWeight = 0.4;
  private double diskWeight = 0.2;
  private boolean omitOverloadedHosts = false;
  private boolean omitForMissingUsageData = false;
  private double load5OverloadedThreshold = 1.0;
  private double load1OverloadedThreshold = 1.5;

  private double recheckMetricsLoad1Threshold = 0.75;
  private double recheckMetricsLoad5Threshold = 0.8;

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

  public void setRxEventBufferSize(long rxEventBufferSize) {
    this.rxEventBufferSize = rxEventBufferSize;
  }

  public int getStatusUpdateConcurrencyLimit() {
    return statusUpdateConcurrencyLimit;
  }

  public void setStatusUpdateConcurrencyLimit(int statusUpdateConcurrencyLimit) {
    this.statusUpdateConcurrencyLimit = statusUpdateConcurrencyLimit;
  }

  public int getMaxStatusUpdateQueueSize() {
    return maxStatusUpdateQueueSize;
  }

  public void setMaxStatusUpdateQueueSize(int maxStatusUpdateQueueSize) {
    this.maxStatusUpdateQueueSize = maxStatusUpdateQueueSize;
  }

  public int getOffersConcurrencyLimit() {
    return offersConcurrencyLimit;
  }

  public void setOffersConcurrencyLimit(int offersConcurrencyLimit) {
    this.offersConcurrencyLimit = offersConcurrencyLimit;
  }

  public MachineLoadMetric getScoreUsingSystemLoad() {
    return scoreUsingSystemLoad;
  }

  public void setScoreUsingSystemLoad(MachineLoadMetric scoreUsingSystemLoad) {
    this.scoreUsingSystemLoad = scoreUsingSystemLoad;
  }

  public boolean isOmitOverloadedHosts() {
    return omitOverloadedHosts;
  }

  public void setOmitOverloadedHosts(boolean omitOverloadedHosts) {
    this.omitOverloadedHosts = omitOverloadedHosts;
  }

  public boolean isOmitForMissingUsageData() {
    return omitForMissingUsageData;
  }

  public void setOmitForMissingUsageData(boolean omitForMissingUsageData) {
    this.omitForMissingUsageData = omitForMissingUsageData;
  }

  public double getLoad5OverloadedThreshold() {
    return load5OverloadedThreshold;
  }

  public void setLoad5OverloadedThreshold(double load5OverloadedThreshold) {
    this.load5OverloadedThreshold = load5OverloadedThreshold;
  }

  public double getLoad1OverloadedThreshold() {
    return load1OverloadedThreshold;
  }

  public void setLoad1OverloadedThreshold(double load1OverloadedThreshold) {
    this.load1OverloadedThreshold = load1OverloadedThreshold;
  }

  public double getAllocatedResourceWeight() {
    return allocatedResourceWeight;
  }

  public void setAllocatedResourceWeight(double allocatedResourceWeight) {
    this.allocatedResourceWeight = allocatedResourceWeight;
  }

  public double getInUseResourceWeight() {
    return inUseResourceWeight;
  }

  public void setInUseResourceWeight(double inUseResourceWeight) {
    this.inUseResourceWeight = inUseResourceWeight;
  }

  public double getCpuWeight() {
    return cpuWeight;
  }

  public void setCpuWeight(double cpuWeight) {
    this.cpuWeight = cpuWeight;
  }

  public double getMemWeight() {
    return memWeight;
  }

  public void setMemWeight(double memWeight) {
    this.memWeight = memWeight;
  }

  public double getDiskWeight() {
    return diskWeight;
  }

  public void setDiskWeight(double diskWeight) {
    this.diskWeight = diskWeight;
  }

  public double getRecheckMetricsLoad1Threshold() {
    return recheckMetricsLoad1Threshold;
  }

  public void setRecheckMetricsLoad1Threshold(double recheckMetricsLoad1Threshold) {
    this.recheckMetricsLoad1Threshold = recheckMetricsLoad1Threshold;
  }

  public double getRecheckMetricsLoad5Threshold() {
    return recheckMetricsLoad5Threshold;
  }

  public void setRecheckMetricsLoad5Threshold(double recheckMetricsLoad5Threshold) {
    this.recheckMetricsLoad5Threshold = recheckMetricsLoad5Threshold;
  }
}
