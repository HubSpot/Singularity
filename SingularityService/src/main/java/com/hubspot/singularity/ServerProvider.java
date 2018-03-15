package com.hubspot.singularity;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import org.eclipse.jetty.server.Server;

import com.google.inject.Provider;

import io.dropwizard.lifecycle.ServerLifecycleListener;

public class ServerProvider implements Provider<Optional<Server>>, ServerLifecycleListener
{
  private final AtomicReference<Server> serverHolder = new AtomicReference<>();

  @Inject
  public ServerProvider() {
  }

  @Override
  public Optional<Server> get() {
    return Optional.ofNullable(serverHolder.get());
  }

  @Override
  public void serverStarted(Server server) {
    serverHolder.set(server);
  }
}
