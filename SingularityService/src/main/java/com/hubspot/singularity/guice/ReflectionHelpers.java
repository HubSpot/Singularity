package com.hubspot.singularity.guice;

import java.lang.annotation.Annotation;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.spi.DefaultElementVisitor;

final class ReflectionHelpers {
  private ReflectionHelpers() {
    throw new AssertionError("Do not instantiate");
  }

  static void scanBindings(final Injector injector, final Class<?> targetClass, final Callback<Binding<?>> callback) {
    for (final Binding<?> binding : injector.getBindings().values()) {
      binding.acceptVisitor(new DefaultElementVisitor<Void>() {
        @Override
        public <U> Void visit(final Binding<U> binding) {
          final Class<?> clazz = binding.getKey().getTypeLiteral().getRawType();
          if (scanClass(clazz, ImmutableSet.<Class<?>>of(targetClass))) {
            callback.call(binding);
          }
          return null;
        }
      });
    }
  }

  private static <T> boolean scanClass(Class<?> scanClass, final Iterable<Class<?>> matchClasses) {
    while (scanClass != null) {
      for (final Class<?> matchClass : matchClasses) {
        if (scanClass == matchClass) {
          return true;
        }
      }

      final Annotation[] annotations = scanClass.getAnnotations();
      for (final Annotation annotation : annotations) {
        for (final Class<?> matchClass : matchClasses) {
          if (annotation.getClass() == matchClass) {
            return true;
          }
          for (final Class<?> interfaceClass : annotation.getClass().getInterfaces()) {
            if (interfaceClass == matchClass) {
              return true;
            }
          }

        }
      }

      final Class<?>[] interfaces = scanClass.getInterfaces();
      for (final Class<?> interfaceClass : interfaces) {
        for (final Class<?> matchClass : matchClasses) {
          if (interfaceClass == matchClass) {
            return true;
          }
        }

        if (scanClass(interfaceClass, matchClasses)) {
          return true;
        }
      }
      scanClass = scanClass.getSuperclass();
    }

    return false;
  }

  interface Callback<T> {
    void call(T clazz);
  }
}
