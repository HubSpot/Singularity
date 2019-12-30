package com.hubspot.singularity.config;

public class CrashLoopConfiguration {
  private int evaluateCooldownOverMinutes = 1;
  private int cooldownFailureThreshold = 5;

  private int evaluateStartupLoopOverMinutes = 10;
  private int startupFailureThreshold = 5;

  private int evaluateOomsOverMinutes = 30;
  private int oomFailureThreshold = 3;

  private int singleInstanceFailureBucketSizeMinutes = 3;
  private int singleInstanceFailureBuckets = 10;
  private double singleInstanceFailureThreshold = 0.25;

  private int multiInstanceFailureBucketSizeMinutes = 3;
  private int multiInstanceFailureBuckets = 10;
  private double multiInstanceFailureThreshold = 0.6;

  private int slowFailureBucketSizeMinutes = 30;
  private int slowFailureBuckets = 15;
  private double slowFailureThreshold = 0.7;

  public int getEvaluateCooldownOverMinutes() {
    return evaluateCooldownOverMinutes;
  }

  public void setEvaluateCooldownOverMinutes(int evaluateCooldownOverMinutes) {
    this.evaluateCooldownOverMinutes = evaluateCooldownOverMinutes;
  }

  public int getCooldownFailureThreshold() {
    return cooldownFailureThreshold;
  }

  public void setCooldownFailureThreshold(int cooldownFailureThreshold) {
    this.cooldownFailureThreshold = cooldownFailureThreshold;
  }

  public int getEvaluateStartupLoopOverMinutes() {
    return evaluateStartupLoopOverMinutes;
  }

  public void setEvaluateStartupLoopOverMinutes(int evaluateStartupLoopOverMinutes) {
    this.evaluateStartupLoopOverMinutes = evaluateStartupLoopOverMinutes;
  }

  public int getStartupFailureThreshold() {
    return startupFailureThreshold;
  }

  public void setStartupFailureThreshold(int startupFailureThreshold) {
    this.startupFailureThreshold = startupFailureThreshold;
  }

  public int getEvaluateOomsOverMinutes() {
    return evaluateOomsOverMinutes;
  }

  public void setEvaluateOomsOverMinutes(int evaluateOomsOverMinutes) {
    this.evaluateOomsOverMinutes = evaluateOomsOverMinutes;
  }

  public int getOomFailureThreshold() {
    return oomFailureThreshold;
  }

  public void setOomFailureThreshold(int oomFailureThreshold) {
    this.oomFailureThreshold = oomFailureThreshold;
  }

  public int getSingleInstanceFailureBucketSizeMinutes() {
    return singleInstanceFailureBucketSizeMinutes;
  }

  public void setSingleInstanceFailureBucketSizeMinutes(int singleInstanceFailureBucketSizeMinutes) {
    this.singleInstanceFailureBucketSizeMinutes = singleInstanceFailureBucketSizeMinutes;
  }

  public int getSingleInstanceFailureBuckets() {
    return singleInstanceFailureBuckets;
  }

  public void setSingleInstanceFailureBuckets(int singleInstanceFailureBuckets) {
    this.singleInstanceFailureBuckets = singleInstanceFailureBuckets;
  }

  public double getSingleInstanceFailureThreshold() {
    return singleInstanceFailureThreshold;
  }

  public void setSingleInstanceFailureThreshold(double singleInstanceFailureThreshold) {
    this.singleInstanceFailureThreshold = singleInstanceFailureThreshold;
  }

  public int getMultiInstanceFailureBucketSizeMinutes() {
    return multiInstanceFailureBucketSizeMinutes;
  }

  public void setMultiInstanceFailureBucketSizeMinutes(int multiInstanceFailureBucketSizeMinutes) {
    this.multiInstanceFailureBucketSizeMinutes = multiInstanceFailureBucketSizeMinutes;
  }

  public int getMultiInstanceFailureBuckets() {
    return multiInstanceFailureBuckets;
  }

  public void setMultiInstanceFailureBuckets(int multiInstanceFailureBuckets) {
    this.multiInstanceFailureBuckets = multiInstanceFailureBuckets;
  }

  public double getMultiInstanceFailureThreshold() {
    return multiInstanceFailureThreshold;
  }

  public void setMultiInstanceFailureThreshold(double multiInstanceFailureThreshold) {
    this.multiInstanceFailureThreshold = multiInstanceFailureThreshold;
  }

  public int getSlowFailureBucketSizeMinutes() {
    return slowFailureBucketSizeMinutes;
  }

  public void setSlowFailureBucketSizeMinutes(int slowFailureBucketSizeMinutes) {
    this.slowFailureBucketSizeMinutes = slowFailureBucketSizeMinutes;
  }

  public int getSlowFailureBuckets() {
    return slowFailureBuckets;
  }

  public void setSlowFailureBuckets(int slowFailureBuckets) {
    this.slowFailureBuckets = slowFailureBuckets;
  }

  public double getSlowFailureThreshold() {
    return slowFailureThreshold;
  }

  public void setSlowFailureThreshold(double slowFailureThreshold) {
    this.slowFailureThreshold = slowFailureThreshold;
  }
}
