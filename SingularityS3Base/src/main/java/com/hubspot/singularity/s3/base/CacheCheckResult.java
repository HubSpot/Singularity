package com.hubspot.singularity.s3.base;

public enum CacheCheckResult {
  FOUND, DOES_NOT_EXIST, FILE_SIZE_MISMATCH, MD5_MISMATCH
}
