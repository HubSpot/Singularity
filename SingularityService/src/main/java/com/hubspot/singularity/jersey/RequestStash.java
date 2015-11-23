package com.hubspot.singularity.jersey;

import com.google.common.base.Optional;

public enum RequestStash {
  INSTANCE;

  private final ThreadLocal<String> url;

  RequestStash() {
    this.url = new ThreadLocal<>();
  }

  public void setUrl(String url) {
    this.url.set(url);
  }

  public Optional<String> getUrl() {
    return Optional.fromNullable(url.get());
  }

  public void clearUrl() {
    this.url.remove();
  }
}
