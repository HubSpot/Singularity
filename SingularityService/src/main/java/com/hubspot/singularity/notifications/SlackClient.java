package com.hubspot.singularity.notifications;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.singularity.config.SlackConfiguration;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;

import io.dropwizard.lifecycle.Managed;

public class SlackClient implements Managed {

  private static final Logger LOG = LoggerFactory.getLogger(SlackClient.class);
  private final Optional<SlackSession> maybeSession;

  @Inject
  public SlackClient(
      SlackConfiguration conf
  ) {
    Optional<SlackSession> maybeSession = Optional.empty();
    try {
      String slackToken = conf.getSlackApiToken();
      SlackSession session = SlackSessionFactory.createWebSocketSlackSession(slackToken);
      session.connect();
      maybeSession = Optional.of(session);
    } catch (IOException e) {
      LOG.error("Could not connect to slack!", e);
    }
    this.maybeSession = maybeSession;
  }

  public void sendMessage(String channelName, String message) {
    if (maybeSession.isPresent()) {
      SlackChannel channel = maybeSession.get().findChannelByName(channelName);
      maybeSession.get().sendMessage(channel, message);
    } else {
      LOG.warn("Unable to send {} to {} because Slack session was unable to connect", message, channelName);
    }
  }

  /**
   * Sends a message, using %s placeholders for args
   */
  public void sendMessage(String channelName, String messageFormat, Object ... args) {
    sendMessage(channelName, String.format(messageFormat, args));
  }

  public void broadcastMessage(List<String> channels, String message) {
    channels.forEach(channel -> sendMessage(channel, message));
  }

  /**
   * Broadcasts a message, using %s placeholders for args
   */
  public void broadcastMessage(List<String> channels, String format, Object ... args) {
    broadcastMessage(channels, String.format(format, args));
  }

  @Override
  public void start() throws Exception {
    if (maybeSession.isPresent()) {
      maybeSession.get().connect();
    }
  }

  @Override
  public void stop() throws Exception {
    if (maybeSession.isPresent()) {
      maybeSession.get().connect();
    }
  }
}
