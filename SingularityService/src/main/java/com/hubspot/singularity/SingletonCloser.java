package com.hubspot.singularity;

import java.io.Closeable;
import java.lang.reflect.Field;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Binding;
import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.internal.BindingImpl;
import com.google.inject.spi.DefaultBindingScopingVisitor;
import com.google.inject.spi.DefaultBindingTargetVisitor;
import com.google.inject.spi.InstanceBinding;
import com.google.inject.spi.LinkedKeyBinding;

public class SingletonCloser {
  static final Logger LOG = LoggerFactory.getLogger(SingletonCloser.class);

  public static void closeAllSingletonClosables(Injector injector) {
    int itemsClosed = 0;
    LOG.info("interrogating injector for any singleton instances of closable");

    if (injector == null || injector.getAllBindings() == null) {
      return;
    }

    for (Map.Entry<Key<?>, Binding<?>> bindingEntry : injector.getAllBindings().entrySet()) {
      final Key<?> key = bindingEntry.getKey();

      if (Closeable.class.isAssignableFrom(key.getTypeLiteral().getRawType())) {
        @SuppressWarnings("unchecked")
        final Binding<Closeable> binding = (Binding<Closeable>) bindingEntry.getValue();

        if (isInitialized(binding)) {
          try (Closeable closeable = binding.getProvider().get()) {
            LOG.debug(String.format("Closing %s instance", closeable.getClass().getName()));
            itemsClosed += 1;
          } catch (UnsupportedOperationException e) {
            String msg = String.format("Cannot close resource %s, as it threw an UnsupportedOperationException", key);
            LOG.debug(msg, e);
          } catch (Throwable t) {
            String msg = String.format("Error closing resource: %s", key);
            LOG.error(msg, t);
          }
        }
      }
    }

    LOG.info(String.format("%d closable instances were autoclosed", itemsClosed));
  }

  private static boolean isInitialized(Binding<?> binding) {
    try {
      return isEagerSingleton(binding) || isInstanceBinding(binding) || instanceIsPopulated(binding.getProvider());
    } catch (ConfigurationException ce) {
      return false;
    }
  }

  private static boolean isEagerSingleton(Binding<?> binding) {
    return binding.acceptScopingVisitor(new DefaultBindingScopingVisitor<Boolean>() {

      @Override
      public Boolean visitEagerSingleton() {
        return true;
      }

      @Override
      protected Boolean visitOther() {
        return false;
      }
    });
  }

  private static boolean isInstanceBinding(Binding<?> binding) {
    LOG.info("{}", binding.getClass());
    return binding.acceptTargetVisitor(new DefaultBindingTargetVisitor<Object, Boolean>() {

      @Override
      public Boolean visit(InstanceBinding<?> instanceBinding) {
        return true;
      }

      @Override
      public Boolean visit(LinkedKeyBinding<?> linkedKeyBinding) {
        return false;
      }

      @Override
      protected Boolean visitOther(Binding<?> binding) {
        return false;
      }
    });
  }

  private static boolean instanceIsPopulated(Provider<?> provider) {
    try {
      Object binding = getField(provider, "val$binding");
      Object factory = getField(binding, BindingImpl.class, "internalFactory");
      Object scopedProvider = getField(factory, "provider");

      return getField(scopedProvider, "instance") != null;
    } catch (Exception e) {
      return false;
    }
  }

  private static Object getField(Object source, String name) throws Exception {
    return getField(source, source.getClass(), name);
  }

  private static Object getField(Object source, Class<?> type, String name) throws Exception {
    Field field = type.getDeclaredField(name);
    field.setAccessible(true);

    return field.get(source);
  }
}
