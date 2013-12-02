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

  @Override
  public void run(SingularityConfiguration configuration, Environment environment) throws Exception {
    final Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    
    SMTPAppender appender = buildSMTPAppender(configuration.getSmtpConfiguration(), configuration.getSmtpConfiguration().getSmtpLoggingConfiguration(), root.getLoggerContext());
    
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
    appender.setSMTPPort(port.or(25));
//    
//    if (smtp.getSSL()) {
//      appender.setSMTPPort(port.or(465));
//    } else {
//      appender.setSMTPPort(port.or(25));
//    }

    appender.setUsername(smtp.getUsername());
    appender.setPassword(smtp.getPassword());
//    appender.setSSL(smtpLogging.getSSL());
//    appender.setSTARTTLS(sm.getSTARTTLS());
    appender.setCharsetEncoding(smtpLogging.getCharsetEncoding());
    appender.setLocalhost(smtpLogging.getLocalhost().orNull());
    appender.start();

    return appender;
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {}

}
