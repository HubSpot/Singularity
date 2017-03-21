package com.hubspot.singularity.smtp;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.mail.Address;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hubspot.singularity.SingularityCloser;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.SMTPConfiguration;

import de.neuland.jade4j.Jade4J;
import de.neuland.jade4j.template.JadeTemplate;

public class SingularityTestMailer {
  
  private static Logger LOG = LoggerFactory.getLogger(SingularityTestMailer.class);

  private Optional<SMTPConfiguration> maybeSmtpConfiguration;
  private Optional<ThreadPoolExecutor> mailSenderExecutorService;

  private SingularityCloser closer;
  
  private JadeTemplate taskFailedTemplate;
  private JadeTemplate requestPausedTemplate;
  private JadeTemplate taskNotRunningWarningTemplate;
  
  private List<String> EMAIL_TO = Lists.newArrayList();
  
  private static boolean TASK_EVER_RAN;

  private static boolean TASK_STATE_LOST;
  private static boolean TASK_STATE_FAILED;
  
  public static void main(String[] args) {
    /*  args[0] = address to send test emails to
     *  args[1] = smtp username
     *  args[2] = smtp password */
    SingularityTestMailer testMailer = new SingularityTestMailer(args[0], args[1], args[2], args[3], Integer.parseInt(args[4]));
    testMailer.sendRequestPausedMail(null, null, Optional.<SingularityTaskHistory>absent());
    testMailer.sendTaskNotRunningWarningEmail(null, (long)10, null, Optional.<SingularityTaskHistory>absent());
    TASK_STATE_FAILED = true;
    TASK_EVER_RAN = true; 
    testMailer.sendTaskFailedMail(null, null, Optional.<SingularityTaskHistory>absent());
    TASK_STATE_FAILED = false;
    
    /*
    TASK_STATE_LOST = true;
    TASK_EVER_RAN = true; 
    testMailer.sendTaskFailedMail(null, null, Optional.<SingularityTaskHistory>absent());
    TASK_EVER_RAN = false; 
    testMailer.sendTaskFailedMail(null, null, Optional.<SingularityTaskHistory>absent());
    */
  }

  public SingularityTestMailer(String EMAIL_TO, String SMTP_USERNAME, String SMTP_PASSWORD, String hostname, int port){
    this.EMAIL_TO.add(EMAIL_TO);
    this.maybeSmtpConfiguration = Optional.of(generateSmtpConfig(SMTP_USERNAME, SMTP_PASSWORD, hostname, port));
    try {
      this.taskFailedTemplate = Jade4J.getTemplate("./src/main/resources/templates/task_failed.jade");
      this.requestPausedTemplate = Jade4J.getTemplate("./src/main/resources/templates/request_paused.jade");
      this.taskNotRunningWarningTemplate = Jade4J.getTemplate("./src/main/resources/templates/task_not_running_warning.jade");
    } catch (IOException e) {
      Throwables.propagate(e);
    }
    
    if (maybeSmtpConfiguration.isPresent()) {
      this.mailSenderExecutorService = Optional.of(new ThreadPoolExecutor(maybeSmtpConfiguration.get().getMailThreads(), maybeSmtpConfiguration.get().getMailMaxThreads(), 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(),
          new ThreadFactoryBuilder().setNameFormat("SingularityMailer-%d").build()));
    } else {
      this.mailSenderExecutorService = Optional.absent();
    }
  }
  
  public SMTPConfiguration generateSmtpConfig(String SMTP_USERNAME, String SMTP_PASSWORD, String hostname, int port){
    SMTPConfiguration smtpConfiguration = new SMTPConfiguration();
    smtpConfiguration.setUsername(SMTP_USERNAME);
    smtpConfiguration.setPassword(SMTP_PASSWORD);
    smtpConfiguration.setHost(hostname);
    smtpConfiguration.setPort(port);
    smtpConfiguration.setMailMaxThreads(6);
    smtpConfiguration.setMailThreads(6);
    return smtpConfiguration;
  }
  
  public void close() {
    if (!mailSenderExecutorService.isPresent()) {
      return;
    }

    closer.shutdown(getClass().getName(), mailSenderExecutorService.get());
  }

  public void sendAbortMail() {
    if (!maybeSmtpConfiguration.isPresent()) {
      LOG.warn("Couldn't send abort mail because no SMTP configuration is present");
      return;
    }

    final List<String> to = maybeSmtpConfiguration.get().getAdmins();
    final String subject = String.format("Singularity on %s is aborting", "somehostname");

    queueMail(to, subject, "");
  }

  private String getEmailLogFormat(List<String> toList, String subject, String body) {
    return String.format("[to: %s, subject: %s, body: %s]", toList, subject, body);
  }

  private Optional<String[]> getTaskLogFile(SingularityTaskId taskId, String filename) {
    String[] mockedFileLines;
    if(filename == "stdout"){
      mockedFileLines = new String[]{
          "Fetching resources into '/usr/share/hubspot/mesos/slaves/201402130038-3265021706-5050-402-3/frameworks/Singularity1/executors/singularity-pagerduty_to_opentsdb-update-1394595612569-1-bushplace-us_east_1a/runs/b0cd589c-7d18-46d9-92f6-9e56722dd25c'",
          "2014-03-12 03:40:13,094 | 25345 | Ready to serve! (PID = 25345)",
          "2014-03-12 03:40:13,101 | 25345 | Registered with Mesos slave for framework Singularity1",
          "2014-03-12 03:40:13,120 | 25345 | Task is: pagerduty_to_opentsdb-update-1394595612569-1-bushplace-us_east_1a"        
      };
    } else {
      mockedFileLines = new String[]{
          "INFO:root:Instanating new PagerDuty connect with subdomain: hubspot and api token: piLWhHeG1yydjJpqwUj2",
          "Traceback (most recent call last):",
          "File /usr/share/hubspot/mesos/slaves/201402130038-3265021706-5050-402-2/frameworks/Singularity1/executors/singularity-pagerduty_to_opentsdb-update-1394667011238-1-sublette-us_east_1b/runs/438235c8-7300-4cac-a674-770958fbe662/app/.deploy_virtualenv/bin/pagerduty_to_opentsdb, line 10, in <module>",
          "load_entry_point('pagerduty-to-opentsdb==1.0hubspot-b48', 'console_sc"
      };
    }
    return Optional.of(mockedFileLines);
  }

  private String populateGenericEmailTemplate(JadeTemplate template, SingularityRequest request, SingularityTaskId taskId, Optional<SingularityTaskHistory> maybeTaskHistory, Map<String, Object> additionalBindings){
    Builder<String, Object> templateSubs = ImmutableMap.<String, Object>builder();
    templateSubs.put("request_id", "pagerduty_to_opentsdb-update");
    templateSubs.put("singularity_task_link", "https://tools.hubteam.com/singularity/task/pagerduty_to_opentsdb-update-1394667011238-1-sublette-us_east_1b");
    templateSubs.put("stdout", getTaskLogFile(taskId, "stdout").or(new String[0]));
    templateSubs.put("stderr", getTaskLogFile(taskId, "stderr").or(new String[0]));
    templateSubs.put("duration_left", "0:10:00.000");
    templateSubs.put("task_id", "pagerduty_to_opentsdb-update-1394667011238-1-sublette-us_east_1b");

    
    templateSubs.put("status", "TASK_STATUS");    
    templateSubs.put("taskStateLost", TASK_STATE_LOST);
    templateSubs.put("taskStateFailed", TASK_STATE_FAILED);
    
    
    List<Map<String, String>> taskUpdates = Lists.newArrayList();
    for (String taskUpdate : Lists.newArrayList("TASK_STARTING", "TASK_RUNNING", "TASK_FAILED")) {
      Map<String, String> formatted = Maps.newHashMap();
      formatted.put("date", "Wed Mar 12 23:30:11 UTC 2014");
      formatted.put("update", taskUpdate);
      taskUpdates.add(formatted);
    }
    
    templateSubs.put("task_updates", taskUpdates);
    templateSubs.put("task_directory", "/usr/share/hubspot/mesos/slaves/201402130038-3265021706-5050-402-2/frameworks/Singularity1/executors/singularity-pagerduty_to_opentsdb-update-1394667011238-1-sublette-us_east_1b/runs/438235c8-7300-4cac-a674-770958fbe662");
    templateSubs.put("slave_hostname", "http://sublette.iad01.hubspot-networks.net/");
    templateSubs.put("taskEverRan", TASK_EVER_RAN);
    
    for (Map.Entry<String, Object> bindingEntry : additionalBindings.entrySet()){
      templateSubs.put(bindingEntry.getKey(), bindingEntry.getValue());
    }
    
    return Jade4J.render(template, templateSubs.build());
  }
  
  public void sendTaskNotRunningWarningEmail(SingularityTaskId taskId, long duration, SingularityRequest request, Optional<SingularityTaskHistory> maybeTaskHistory ) {

    final List<String> to = EMAIL_TO;
    final String subject = String.format("Task %s has not started yet", "pagerduty_to_opentsdb-update-1394667011238-1-sublette-us_east_1b");

    ImmutableMap<String, Object> additionalBindings = ImmutableMap.<String, Object>builder()
        .put("duration_running", DurationFormatUtils.formatDurationHMS(duration))
        .build();
  
    final String body = populateGenericEmailTemplate(this.taskNotRunningWarningTemplate, request, taskId, maybeTaskHistory, additionalBindings);
  
    queueMail(to, subject, body);
  }

  public void sendTaskFailedMail(SingularityTaskId taskId, SingularityRequest request, Optional<SingularityTaskHistory> maybeTaskHistory ) {

    final List<String> to = EMAIL_TO;
    final String subject = "(TASK_FAILED) - Task pagerduty_to_opentsdb-update-1394667011238-1-sublette-us_east_1b failed after running";

    final String adminEmails = "some_admin@domain.com";
    
    ImmutableMap<String, Object> additionalBindings = ImmutableMap.<String, Object>builder()
        .put("adminEmails", adminEmails.toString())
        .build();
    
    final String body = populateGenericEmailTemplate(this.taskFailedTemplate, request, taskId, maybeTaskHistory, additionalBindings);

    queueMail(to, subject, body);
  }
  

  public void sendRequestPausedMail(SingularityTaskId taskId, SingularityRequest request, Optional<SingularityTaskHistory> maybeTaskHistory ) {
    
    final int maxFailures = 1;

    final List<Map<String, String>> requestHistoryFormatted = Lists.newArrayList();
   
    for( String update : Lists.newArrayList("CREATED", "LOST", "KILLED")){
      Map<String, String> requestUpdate = Maps.newHashMap();
      requestUpdate.put("state", update);
      requestUpdate.put("date", "Tue Mar 11 11:56::41 GMT");
      requestUpdate.put("user", "some user");
      requestUpdate.put("request_id", "pagerduty_to_opentsdb-update");
      requestUpdate.put("request_cmd", "Some command");
      requestHistoryFormatted.add(requestUpdate);
    }

    final List<String> to = EMAIL_TO;
    final String subject = "Request pagerduty_to_opentsdb-update is PAUSED";

    ImmutableMap<String, Object> additionalBindings = ImmutableMap.<String, Object>builder()
        .put("num_failures", maxFailures)
        .put("request_history", requestHistoryFormatted)
        .build();

    final String body = populateGenericEmailTemplate(this.requestPausedTemplate, request, taskId, maybeTaskHistory, additionalBindings);

    queueMail(to, subject, body);
  }

  private void queueMail(final List<String> toList, final String subject, final String body) {
    if (toList.isEmpty()) {
      LOG.warn(String.format("Couldn't queue email %s because no to address is present", getEmailLogFormat(toList, subject, body)));
      return;
    }

    if (!mailSenderExecutorService.isPresent()) {
      LOG.warn(String.format("Couldn't queue email %s because no SMTP configuration is present", getEmailLogFormat(toList, subject, body)));
      return;
    }

    final Runnable cmd = new Runnable() {

      @Override
      public void run() {
        sendMail(toList, subject, body);
      }
    };

    LOG.debug(String.format("Queuing an email to %s (subject: %s)", toList, subject));

    mailSenderExecutorService.get().submit(cmd);
  }

  private Session createSession(SMTPConfiguration smtpConfiguration, boolean useAuth) {
    Properties properties = System.getProperties();

    properties.setProperty("mail.smtp.host", smtpConfiguration.getHost());

    if (smtpConfiguration.getPort().isPresent()) {
      properties.setProperty("mail.smtp.port", Integer.toString(smtpConfiguration.getPort().get()));
    }

    if (useAuth) {
      properties.setProperty("mail.smtp.auth", "true");
      return Session.getInstance(properties, new SMTPAuthenticator(smtpConfiguration.getUsername().get(), smtpConfiguration.getPassword().get()));
    } else {
      return Session.getInstance(properties);
    }
  }

  public void sendMail(List<String> toList, String subject, String body) {
    if (!maybeSmtpConfiguration.isPresent()) {
      LOG.warn(String.format("Couldn't send email %s because no SMTP configuration is present", getEmailLogFormat(toList, subject, body)));
    }

    final SMTPConfiguration smtpConfiguration = maybeSmtpConfiguration.get();

    boolean useAuth = false;

    if (smtpConfiguration.getUsername().isPresent() && smtpConfiguration.getPassword().isPresent()) {
      useAuth = true;
    } else if (smtpConfiguration.getUsername().isPresent() || smtpConfiguration.getPassword().isPresent()) {
      LOG.warn(String.format("Not using smtp authentication because username (%s) or password (%s) was not present", smtpConfiguration.getUsername().isPresent(), smtpConfiguration.getPassword().isPresent()));
    }

    try {
      final Session session = createSession(maybeSmtpConfiguration.get(), useAuth);

      MimeMessage message = new MimeMessage(session);

      message.setFrom(new InternetAddress(smtpConfiguration.getFrom()));

      if (maybeSmtpConfiguration.get().isIncludeAdminsOnAllMails()) {
        Address[] ccArray = getAddresses(maybeSmtpConfiguration.get().getAdmins());
        if (ccArray.length > 0) {
          LOG.trace(String.format("Adding admins %s to mail %s", Arrays.toString(ccArray), subject));
          message.addRecipients(RecipientType.CC, ccArray);
        }
      }

      message.setSubject(subject);
      message.setContent(body, "text/html; charset=utf-8");

      Address[] toArray = getAddresses(toList);

      message.addRecipients(RecipientType.TO, toArray);

      LOG.trace(String.format("Sending a message to %s - %s", Arrays.toString(toArray), message));

      Transport.send(message);
    } catch (Throwable t) {
      LOG.warn(String.format("Unable to send message %s", getEmailLogFormat(toList, subject, body)), t);
    }
  }

  private Address[] getAddresses(List<String> toList) {
    List<InternetAddress> addresses = Lists.newArrayListWithCapacity(toList.size());

    for (String to : toList) {
      try {
        addresses.add(new InternetAddress(to));
      } catch (Throwable t) {
        LOG.warn(String.format("Invalid address %s - ignoring", to), t);
      }
    }

    return addresses.toArray(new InternetAddress[addresses.size()]);
  }
}


