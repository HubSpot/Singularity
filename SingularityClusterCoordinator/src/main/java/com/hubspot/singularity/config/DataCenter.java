package com.hubspot.singularity.config;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

public class DataCenter {
  @NotNull
  private String name;
  @NotEmpty
  private List<String> hosts;
  @NotNull
  private String contextPath;
  // http or https
  private String scheme = "http";
}
