package com.hubspot.singularity.runner.base.validators;

import java.io.File;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.hubspot.singularity.runner.base.constraints.DirectoryExists;

public class DirectoryExistsValidator implements ConstraintValidator<DirectoryExists, String> {
  @Override
  public void initialize(DirectoryExists constraintAnnotation) {

  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) {
      return false;
    }

    final File file = new File(value);

    return file.exists() && file.isDirectory();
  }
}
