package com.hubspot.singularity.smtp;

import io.dropwizard.ConfiguredBundle;
import io.dropwizard.logging.DropwizardLayout;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.net.SMTPAppender;

import com.google.common.base.Optional;
import com.hubspot.singularity.config.SMTPConfiguration;
import com.hubspot.singularity.config.SMTPLoggingConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;

public class SMTPAppenderBundle implements ConfiguredBundle<SingularityConfiguration> {
  
  private final static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(SMTPAppenderBundle.class);
  
  @Override
  public void run(SingularityConfiguration configuration, Environment environment) throws Exception {
    Optional<SMTPConfiguration> smtp = configuration.getSmtpConfiguration();
    
    if (!smtp.isPresent()) {
      LOG.info("SMTPAppenderBundle is installed, but there is no SMTP configuration (smtp: in yml)");
      return;
    }
    
    SMTPLoggingConfiguration smtpLoggingConfiguration = smtp.get().getSmtpLoggingConfiguration();

    if (!smtpLoggingConfiguration.isEnabled()) {
      LOG.info("SMTPAppenderBundle is installed, but it is not enabled - enable using smtp: logging: enabled: true");
      return;
    }
    
    final Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    
    SMTPAppender appender = buildSMTPAppender(smtp.get(), smtpLoggingConfiguration, root.getLoggerContext());
    
    root.addAppender(appender);
  }
  
  private SMTPAppender buildSMTPAppender(SMTPConfiguration smtp, SMTPLoggingConfiguration smtpLogging, LoggerContext loggerContext) {
    final DropwizardLayout formatter = new DropwizardLayout(loggerContext, smtpLogging.getTimeZone());
   
    if (smtpLogging.getLogFormat().isPresent()) {
      formatter.setPattern(smtpLogging.getLogFormat().get());
    }
    
    formatter.start();

    final SMTPAppender appender = new SMTPAppender();
    appender.setContext(loggerContext);
    appender.setLayout(formatter);
    
    final ThresholdFilter filter = new ThresholdFilter();
    filter.setLevel(smtpLogging.getThreshold().toString());
    filter.start();
    appender.addFilter(filter);
    
    appender.setFrom(smtp.getFrom());
    
    for (String to : smtp.getAdmins()) {
      appender.addTo(to);
    }
    
    appender.setSubject(smtpLogging.getSubject());
    appender.setSMTPHost(smtp.getHost());

    Optional<Integer> port = smtp.getPort();
   
    if (smtp.isSsl()) {
      appender.setSMTPPort(port.or(465));
    } else {
      appender.setSMTPPort(port.or(25));
    }
    
    if (smtp.getUsername().isPresent()) {
      appender.setUsername(smtp.getUsername().get());
    }
    if (smtp.getPassword().isPresent()) {
      appender.setPassword(smtp.getPassword().get());
    }
    
    appender.setSSL(smtp.isSsl());
    appender.setSTARTTLS(smtp.isStartTLS());
    appender.setCharsetEncoding(smtpLogging.getCharsetEncoding());
    appender.setLocalhost(smtpLogging.getLocalhost().orNull());
    appender.start();

    return appender;
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {}

}
