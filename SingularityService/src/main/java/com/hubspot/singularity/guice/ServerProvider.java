package com.hubspot.singularity.guice;

import io.dropwizard.lifecycle.ServerLifecycleListener;
import io.dropwizard.setup.Environment;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jetty.server.Server;
import org.parboiled.common.Preconditions;

public class ServerProvider {

  private final AtomicReference<Server> server = new AtomicReference<>();

  public ServerProvider(Environment environment) {
    environment.lifecycle().addServerLifecycleListener(new ServerLifecycleListener() {

      @Override
      public void serverStarted(Server server) {
        ServerProvider.this.server.set(server);
      }
    });
  }

  public Server get() {
    return Preconditions.checkNotNull(server.get());
  }

}
