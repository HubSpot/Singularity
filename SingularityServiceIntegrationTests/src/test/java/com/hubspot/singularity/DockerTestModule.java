package com.hubspot.singularity;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.inject.AbstractModule;
import com.hubspot.singularity.client.SingularityClientModule;

public class DockerTestModule extends AbstractModule {
  private static final Pattern DOCKER_HOST_PATTERN = Pattern.compile("tcp://(.*?):(\\d+)");

  public static Optional<String> getDockerAddress() {
    final String dockerHost = System.getenv("DOCKER_HOST");

    if (Strings.isNullOrEmpty(dockerHost)) {
      return Optional.absent();
    }

    final Matcher m = DOCKER_HOST_PATTERN.matcher(dockerHost);

    if (m.matches()) {
      return Optional.of(m.group(1));
    } else {
      return Optional.absent();
    }
  }

  @Override
  protected void configure() {
    final int singularityPort = Integer.parseInt(System.getProperty("singularity.port"));

    install(new SingularityClientModule(Arrays.asList(String.format("%s:%d", getDockerAddress().or("localhost"), singularityPort))));
  }
}
