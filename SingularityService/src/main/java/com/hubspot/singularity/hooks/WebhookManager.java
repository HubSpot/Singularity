package com.hubspot.singularity.hooks;

import java.util.Collections;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityTaskUpdate;
import com.hubspot.singularity.data.TaskManager;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

public class WebhookManager {

  private final static Logger LOG = LoggerFactory.getLogger(TaskManager.class);

  private static final String HOOK_ROOT_PATH = "/hooks";
  private static final String HOOK_FORMAT_PATH = HOOK_ROOT_PATH + "/%s";
  
  private final CuratorFramework curator;
  private final ObjectMapper objectMapper;
  
  private final AsyncHttpClient asyncHttpClient;
  private final AsyncCompletionHandler<Response> handler;
  
  // TODO watch/cache
  // TODO one async htp client?
  
  @Inject
  public WebhookManager(CuratorFramework curator, ObjectMapper objectMapper) {
    this.curator = curator;
    this.objectMapper = objectMapper;
    
    asyncHttpClient = new AsyncHttpClient();
    handler = new AsyncCompletionHandler<Response>() {

      @Override
      public Response onCompleted(Response response) throws Exception {
        return response;
      }

      @Override
      public void onThrowable(Throwable t) {
        LOG.warn("Throwable while processing a webhook", t);
      }
    };
  }
  
  public void notify(SingularityTaskUpdate taskUpdate) {
    for (String hook : getWebhooks()) {
      try {
        asyncHttpClient.preparePost(hook)
          .setBody(taskUpdate.getTaskData(objectMapper))
          .addHeader("Content-Type", "application/json")
          .execute(handler);
      } catch (Exception e) {
        LOG.warn("Exception while preparing hook: " + hook, e);
      }
    }
  }
  
  private String getHookPath(String uri) {
    return String.format(HOOK_FORMAT_PATH, JavaUtils.urlEncode(uri));
  }
  
  public List<String> getWebhooks() {
    return loadHooks();
  }
  
  private List<String> loadHooks() {
    try {
      final List<String> hooks = curator.getChildren().forPath(HOOK_ROOT_PATH);
      final List<String> decodedHooks = Lists.newArrayListWithCapacity(hooks.size());
      for (String hook : hooks) {
        decodedHooks.add(JavaUtils.urlDecode(hook));
      }
      return decodedHooks;
    } catch (NoNodeException nee) {
      return Collections.emptyList();
    } catch (Exception e) {
      // TODO fix this - throw a curator exception.
      throw Throwables.propagate(e);
    }
  }
  
  public void addHook(String uri) {
    final String path = getHookPath(uri);
    try {
      curator.create().creatingParentsIfNeeded().forPath(path);
    } catch (NodeExistsException nee) {
      LOG.info("Webhook already existed for path: " + uri);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
  
  public void removeHook(String uri) {
    final String path = getHookPath(uri);
    try {
      curator.delete().forPath(path);
    } catch (NoNodeException nne) {
      LOG.info("Expected webhook, but didn't exist at path for path: " + uri);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
  
  
}
