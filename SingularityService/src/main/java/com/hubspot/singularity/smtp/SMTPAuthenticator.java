package com.hubspot.singularity.smtp;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

public class SMTPAuthenticator extends Authenticator {
  private final PasswordAuthentication passwordAuthentication;

  public SMTPAuthenticator(String username, String password) {
    this.passwordAuthentication = new PasswordAuthentication(username, password);
  }

  @Override
  protected PasswordAuthentication getPasswordAuthentication() {
    return passwordAuthentication;
  }
}
