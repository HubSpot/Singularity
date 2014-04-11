package com.hubspot.deploy;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

@JsonIgnoreProperties(ignoreUnknown=true)
public class DeployConfig {
  
  public static final String PROCFILE_ITEM_ENV_NAME = "PROCFILE_ITEM";
  
  private final String type;
  private final Map<String, ProcfileItem> procfile;
  private final String buildName;
  private final Optional<String> warmupUrl;
  private final List<String> loadBalancers;
  private final Optional<String> appRoot;
  private final String name;
  private final Map<String, Map<String, String>> env;
  private final Map<String, List<Server>> servers;
  private final List<String> extraLbConfigs;
  private final Boolean singleProcessRunning;
  private final List<String> owners;
  private final Optional<String> rewriteAppRootTo;

  @JsonCreator
  public DeployConfig(@JsonProperty("name") String name, @JsonProperty("buildName") String buildName,
                      @JsonProperty("artifactId") String artifactId, @JsonProperty("appRoot") String appRoot,
                      @JsonProperty("env") Map<String, Map<String, String>> env,
                      @JsonProperty("loadBalancers") List<String> loadBalancers,
                      @JsonProperty("loadBalancer") String loadBalancer, @JsonProperty("warmupUrl") String warmupUrl,
                      @JsonProperty("procfile") Map<String, ProcfileItem> procfile,
                      @JsonProperty("servers") Map<String, List<Server>> servers,
                      @JsonProperty("nginxExtraConfigs") List<String> extraLbConfigs,
                      @JsonProperty("singleProcessRunning") Boolean singleProcessRunning,
                      @JsonProperty("owners") List<String> owners, @JsonProperty("type") String type,
                      @JsonProperty("rewriteAppRootTo") String rewriteAppRootTo) {
    this.type = type;
    this.name = name;

    // this is stupid, Objects.firstNonNull() should be varags...
    if (!Strings.isNullOrEmpty(buildName)) {
      this.buildName = buildName;
    } else if (!Strings.isNullOrEmpty(artifactId)) {
      this.buildName = artifactId;
    } else {
      this.buildName = name;
    }
    
    this.appRoot = Optional.fromNullable(appRoot);
    this.env = Objects.firstNonNull(env, Collections.<String, Map<String, String>>emptyMap());
    this.loadBalancers = Objects.firstNonNull(loadBalancers, Lists.<String> newArrayList());
    if (!Strings.isNullOrEmpty(loadBalancer) && !this.loadBalancers.contains(loadBalancer)) {
      this.loadBalancers.add(loadBalancer);
    }

    if (!Strings.isNullOrEmpty(warmupUrl)) {
      this.warmupUrl = Optional.of(warmupUrl);
    } else if (this.appRoot.isPresent()) {
      this.warmupUrl = Optional.of(String.format("%s/healthcheck", this.appRoot.get()));
    } else {
      this.warmupUrl = Optional.absent();
    }

    this.procfile = Objects.firstNonNull(procfile, Collections.<String, ProcfileItem>emptyMap());

    if (this.procfile.containsKey("web") && (this.procfile.get("web").getPorts() == null || this.procfile.get("web").getPorts() == 0)) {
      final ProcfileItem webItem = this.procfile.get("web");
      this.procfile.put("web", new ProcfileItem(webItem.getCmd(), webItem.getSchedule(), 3, webItem.getUser(), true, webItem.getResources().orNull()));
    }

    this.servers = Objects.firstNonNull(servers, Collections.<String, List<Server>>emptyMap());
    this.extraLbConfigs = Objects.firstNonNull(extraLbConfigs, Collections.<String>emptyList());
    this.singleProcessRunning = Objects.firstNonNull(singleProcessRunning, false);
    this.owners = Objects.firstNonNull(owners, Collections.<String>emptyList());
    this.rewriteAppRootTo = Optional.fromNullable(rewriteAppRootTo);
  }
  
  public Map<String, ProcfileItem> getProcfile() {
    return procfile;
  }

  public String getBuildName() {
    return buildName;
  }

  public Optional<String> getWarmupUrl() {
    return warmupUrl;
  }

  public List<String> getLoadBalancers() {
    return loadBalancers;
  }

  public Optional<String> getAppRoot() {
    return appRoot;
  }

  public String getName() {
    return name;
  }

  public Map<String, Map<String, String>> getEnv() {
    return env;
  }

  public Map<String, List<Server>> getServers() {
    return servers;
  }

  public List<String> getExtraLbConfigs() {
    return extraLbConfigs;
  }

  public Boolean isSingleProcessRunning() {
    return singleProcessRunning;
  }

  public List<String> getOwners() {
    return owners;
  }

  public String getType() {
    return type;
  }

  public Optional<String> getRewriteAppRootTo() { return rewriteAppRootTo; }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("name", name)
        .add("buildName", buildName)
        .add("servers", servers)
        .toString();
  }
}
