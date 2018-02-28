## Singularity Webhooks

Singularity provides webhooks for changes to the three core types of objects in Singularity: requests, deploys, and tasks.

Webhooks are managed via the [API](apidocs/api-webhooks.md) and a separate webhook should be added separately in order to receive updates about all three object types.

### Adding a Webhook

In order to create a new Webhook, post the json for the [SingularityWebhook](apidocs/models.md#model-SingularityWebhook) to the [webhook endpoint](apidocs/api-webhooks.md#post-apiwebhooks).

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

This webhook will start to receive events anytime a [Request](apidocs/models.md#model-SingularityRequest) is created, modified, or deleted.

### Webhook Types

#### Request

Request webhooks are sent every time a request is created, deleted, or updated - as well as when its state changes (because it is paused or enters cooldown.)

Request webhooks are in the format of [SingularityRequestHistory](apidocs/models.md#model-singularityrequesthistory) objects. 

#### Deploy

Deploy webhooks are sent when deploys are started and finished (fail or succeed.)

Deploy webhooks are in the format of [SingularityDeployUpdate](apidocs/models.md#model-SingularityDeployUpdate) objects.

#### Task

Task webhooks are sent when tasks are launched by Singularity, killed with by Singularity users, and on all task updates recieved from Mesos.

Task webhooks are in the format of [SingularityTaskWebhook](https://github.com/HubSpot/Singularity/blob/master/SingularityBase/src/main/java/com/hubspot/singularity/SingularityTaskWebhook.java) objects.

### Webhook notes

- Webhooks are only considered successful if the response code to the webhook is between 200 and 299 inclusive.
- Webhooks will be delivered every 10 seconds (checkWebhooksEveryMillis) 
- Webhooks will be deleted if they fail to deliver and there are more than 50 in the queue (maxQueuedUpdatesPerWebhook)
- Webhooks will be deleted if they fail to deliver after 7 days (deleteUndeliverableWebhooksAfterHours)
- For debugging purposes, queued webhook updates can be retrieved from the [API](apidocs/api-webhooks.md#get-apiwebhooksrequestwebhookid)
