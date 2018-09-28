## Singularity Webhooks

Singularity provides webhooks for changes to the three core types of objects in Singularity: requests, deploys, and tasks.

Webhooks are managed via the [API](api.html) and a separate webhook should be added separately in order to receive updates about all three object types.

### Adding a Webhook

In order to create a new Webhook, post the json for the [SingularityWebhook](api.html) to the [webhook endpoint](api.html).

For example, to receive request updates, send the following json: (create a file webhook.json with your own requestb.in for testing!)

```json
{
    "id": "test-webhook",
    "uri": "http://requestb.in/1lw85s51",
    "type": "REQUEST"
}
```

Then post that to the webhooks endpoint:

```sh
curl -i -X POST -H "Content-Type: application/json" -d@webhook.json \
http://singularityhostname/singularity/api/webhooks
```

This webhook will start to receive events anytime a [Request](api.html) is created, modified, or deleted.

### Webhook Types

#### Request

Request webhooks are sent every time a request is created, deleted, or updated - as well as when its state changes (because it is paused or enters cooldown.)

Request webhooks are in the format of [SingularityRequestHistory](api.html) objects. 

#### Deploy

Deploy webhooks are sent when deploys are started and finished (fail or succeed.)

Deploy webhooks are in the format of [SingularityDeployUpdate](api.html) objects.

#### Task

Task webhooks are sent when tasks are launched by Singularity, killed with by Singularity users, and on all task updates recieved from Mesos.

Task webhooks are in the format of [SingularityTaskWebhook](https://github.com/HubSpot/Singularity/blob/master/SingularityBase/src/main/java/com/hubspot/singularity/SingularityTaskWebhook.java) objects.

### Webhook placeholders

Singularity supports placeholders in webhook URIs which will be replaced with their respective values before the URI is actually called.

The following URI placeholder values are supported for each kind of webhook:
- **Request**:
  - `$REQUEST_ID`
- **Deploy**:
  - `$REQUEST_ID`
  - `$DEPLOY_ID`
- **Task**
  - `$REQUEST_ID`
  - `$DEPLOY_ID`
  - `$TASK_ID`

You could, for example, send along the relevant Request ID with a Task-level webhook by creating the following webhook URI in Singularity:
```
https://my-automation-service.net/webhooks/task?requestId=$REQUEST_ID
```

### Webhook notes

- Webhooks are only considered successful if the response code to the webhook is between 200 and 299 inclusive.
- Webhooks will be delivered every 10 seconds (checkWebhooksEveryMillis) 
- Webhooks will be deleted if they fail to deliver and there are more than 50 in the queue (maxQueuedUpdatesPerWebhook)
- Webhooks will be deleted if they fail to deliver after 7 days (deleteUndeliverableWebhooksAfterHours)
- For debugging purposes, queued webhook updates can be retrieved from the [API](api.html)
