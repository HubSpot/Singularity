package com.hubspot.singularity.data;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.UUID;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.Transcoder;

public class AuthTokenManager extends CuratorManager {
  private static final Logger LOG = LoggerFactory.getLogger(AuthTokenManager.class);

  private static final String TOKEN_ROOT = "/tokens";
  private static final String TOKEN_PATH = TOKEN_ROOT + "/%s";

  private final Transcoder<SingularityUser> userTranscoder;

  @Inject
  public AuthTokenManager(CuratorFramework curator,
                          SingularityConfiguration configuration,
                          MetricRegistry metricRegistry,
                          Transcoder<SingularityUser> userTranscoder) {
    super(curator, configuration, metricRegistry);
    this.userTranscoder = userTranscoder;
  }

  public String generateToken(SingularityUser userData) throws NoSuchAlgorithmException, InvalidKeySpecException {
    String newToken = UUID.randomUUID().toString();
    String hashed = generateTokenHash(newToken);
    saveToken(hashed, userData);
    return newToken;
  }

  private void saveToken(String hashed, SingularityUser userData) {
    save(getTokenPath(hashed), userData, userTranscoder);
  }

  public SingularityUser getUserIfValidToken(String token) {
    for (String hashed : getChildren(TOKEN_ROOT)) {
      try {
        if (validateToken(token, hashed)) {
          Optional<SingularityUser> maybeUser = getData(getTokenPath(hashed), userTranscoder);
          if (maybeUser.isPresent()) {
            return maybeUser.get();
          }
        }
      } catch (Throwable t) {
        LOG.error("Unable to validate token", t);
      }
    }
    LOG.debug("No matching token found");
    return null;
  }

  private String getTokenPath(String hashed) {
    return String.format(TOKEN_PATH, hashed);
  }

  // Implementation of PBKDF2WithHmacSHA1
  private static boolean validateToken(String originalToken, String storedToken) throws NoSuchAlgorithmException, InvalidKeySpecException {
    String[] parts = storedToken.split(":");
    int iterations = Integer.parseInt(parts[0]);
    byte[] salt = fromHex(parts[1]);
    byte[] hash = fromHex(parts[2]);

    PBEKeySpec spec = new PBEKeySpec(originalToken.toCharArray(), salt, iterations, hash.length * 8);
    SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
    byte[] testHash = skf.generateSecret(spec).getEncoded();

    int diff = hash.length ^ testHash.length;
    for (int i = 0; i < hash.length && i < testHash.length; i++) {
      diff |= hash[i] ^ testHash[i];
    }
    return diff == 0;
  }

  private static byte[] fromHex(String hex) throws NoSuchAlgorithmException {
    byte[] bytes = new byte[hex.length() / 2];
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
    }
    return bytes;
  }

  private static String generateTokenHash(String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
    int iterations = 1000;
    char[] chars = password.toCharArray();
    byte[] salt = getSalt();

    PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, 64 * 8);
    SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
    byte[] hash = skf.generateSecret(spec).getEncoded();
    return iterations + ":" + toHex(salt) + ":" + toHex(hash);
  }

  private static byte[] getSalt() throws NoSuchAlgorithmException {
    SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
    byte[] salt = new byte[16];
    sr.nextBytes(salt);
    return salt;
  }

  private static String toHex(byte[] array) throws NoSuchAlgorithmException {
    BigInteger bi = new BigInteger(1, array);
    String hex = bi.toString(16);
    int paddingLength = (array.length * 2) - hex.length();
    if (paddingLength > 0) {
      return String.format("%0" + paddingLength + "d", 0) + hex;
    } else {
      return hex;
    }
  }

}
