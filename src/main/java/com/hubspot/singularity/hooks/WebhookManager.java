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
import com.google.inject.Inject;
import com.hubspot.singularity.data.SingularityTaskUpdate;
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
  
  private final List<String> hooks;
  private final AsyncHttpClient asyncHttpClient;
  private final AsyncCompletionHandler<Response> handler;
  
  @Inject
  public WebhookManager(CuratorFramework curator, ObjectMapper objectMapper) {
    this.curator = curator;
    this.objectMapper = objectMapper;
    
    hooks = loadHooks();
  
    asyncHttpClient = new AsyncHttpClient();
    handler = new AsyncCompletionHandler<Response>(){

        @Override
        public Response onCompleted(Response response) throws Exception{
            return response;
        }

        @Override
        public void onThrowable(Throwable t) {
          LOG.warn("Throwable while processing a webhook", t);
        }
    };
  }
  
  public void notify(SingularityTaskUpdate taskUpdate) {
    for (String hook : hooks) {
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
    return String.format(HOOK_FORMAT_PATH, uri);
  }
  
  public List<String> getWebhooks() {
    return hooks;
  }
  
  private List<String> loadHooks() {
    try {
      return curator.getChildren().forPath(HOOK_ROOT_PATH);
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
      hooks.add(uri);
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
      hooks.remove(uri);
    } catch (NoNodeException nne) {
      LOG.info("Expected webhook, but didn't exist at path for path: " + uri);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
  
  
}
