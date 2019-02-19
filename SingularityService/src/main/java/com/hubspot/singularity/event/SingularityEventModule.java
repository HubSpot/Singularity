package com.hubspot.singularity.event;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.hubspot.singularity.data.WebhookManager;

public class SingularityEventModule implements Module {

  @Override
  public void configure(final Binder binder) {
    Multibinder<SingularityEventListener> eventListeners = Multibinder.newSetBinder(binder, SingularityEventListener.class);
    eventListeners.addBinding().to(WebhookManager.class).in(Scopes.SINGLETON);
    binder.bind(SingularityEventListener.class).to(SingularityEventController.class).in(Scopes.SINGLETON);
    }
}
