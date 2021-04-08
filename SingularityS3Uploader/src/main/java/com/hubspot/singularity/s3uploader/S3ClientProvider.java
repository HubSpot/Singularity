package com.hubspot.singularity.s3uploader;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class S3ClientProvider {
  private static final Logger LOG = LoggerFactory.getLogger(S3ClientProvider.class);

  private final Map<String, AmazonS3> clientByKey;
  private final Map<String, AtomicInteger> clientHolds;

  @Inject
  public S3ClientProvider() {
    this.clientByKey = new HashMap<>();
    this.clientHolds = new HashMap<>();
  }

  public synchronized AmazonS3 getClient(BasicAWSCredentials credentials) {
    String key = credsToKey(credentials);
    AmazonS3 s3 = clientByKey.computeIfAbsent(key, k -> new AmazonS3Client(credentials));
    clientHolds.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
    return s3;
  }

  public synchronized void returnClient(BasicAWSCredentials credentials) {
    String key = credsToKey(credentials);
    AtomicInteger holds = clientHolds.get(key);
    if (holds == null) {
      LOG.error("Client returned before being checked out!");
    } else {
      int remaining = holds.decrementAndGet();
      if (remaining == 0) {
        clientHolds.remove(key);
        clientByKey.remove(key);
      }
    }
  }

  public static String credsToKey(BasicAWSCredentials credentials) {
    // BasicAWSCredentials doesn't implement any sort of hash code, use this
    return String.format(
      "%s%s",
      credentials.getAWSAccessKeyId(),
      credentials.getAWSSecretKey()
    );
  }
}
