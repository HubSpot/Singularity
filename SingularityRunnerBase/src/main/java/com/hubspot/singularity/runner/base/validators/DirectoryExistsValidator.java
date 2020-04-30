package com.hubspot.singularity.runner.base.validators;

import com.hubspot.singularity.runner.base.constraints.DirectoryExists;
import java.io.File;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class DirectoryExistsValidator
  implements ConstraintValidator<DirectoryExists, String> {

  @Override
  public void initialize(DirectoryExists constraintAnnotation) {}

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) {
      return false;
    }

    final File file = new File(value);

    return file.exists() && file.isDirectory();
  }
}
