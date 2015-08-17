package com.hubspot.singularity.runner.base.jackson;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({ METHOD, FIELD, PARAMETER })
@Retention(RUNTIME)
public @interface Obfuscate {
}
