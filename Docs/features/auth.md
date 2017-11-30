## Singularity Webhook Auth

Singularity contains a few options for authentication andautorization. The most reliable of these is webhook auth. The setup for Singularity's webhook auth is based on the [webhook token auth](https://kubernetes.io/docs/admin/authentication/#webhook-token-authentication) provided in kubernetes.

### Webhook Authentication Flow

When webhook atuh is configured, Singularity will look for an `Authorization` header on each api call. Singularity will then make a `GET` call to the configured `webhookAuth.authVerificationUrl` with the same `Authorization` header send. The system expects a response which describes the user as JSON in a format like:

```json
{
	"user": {
		"id": "required-user-id",
		"name": "Optional User Name",
		"email": "useremail@example.com",
		"groups": [
			"engineering",
			"singularity-admin"
		],
		"authenticated": true
	},
	"error": "Optional exception message"
}
```

The authenticating system should return `"authenticated": true` for the user if the user was successfully authenticated, as well as any groups the user is part of (for later use with authorization). If there was an exception/error while authenticating, Singularity can display that to the user if it is returned in the `error` field.

### Configuring Webhook Authentication and Authorization Groups

To enable webhook auth in Singularity, there are two main sections of the configuration yaml to update. An example is shown below with explanations 

```yaml
auth:
  enabled: true
  authenticators:
    - WEBHOOK
  adminGroups:
  - singularity-admin
  requiredGroups:
  - engineering
  jitaGroups:
  - engineering-jita
  defaultReadOnlyGroups:
  - engineering-ro
  globalReadOnlyGroups:
  - engineering-leaders-ro

 webhookAuth:
  authVerificationUrl: https://my-auth-domain.com/singularity
```

***Verification URL***

The `webhookAuth.authVerificationUrl` will be sent a `GET` with the `Authorization` header provided as described in the section above

***Auth Config***

- `enabled` - defaults to `false`. If set to `true`, auth will be enforced
- `authenticators` - a list of authentication types to use. For webhook auth, this is simply `- WEBHOOK`
- `adminGroups` - If a user is part of these groups they are allowed admin actions in Singularity (actions on slaves, view all requests, etc)
- `requiredGroups` - A user must be part of one at least of these groups in order to access Singularity
- `jitaGroups` - Groups that can be allowed access to any SingularityRequest (but not admin actions)
- `defaultReadOnlyGroups` - If read only groups are not set for a SingularityRequest, these groups are used for read access
- `globalReadOnlyGroups` - These groups are allowed read access to requests regardless of what groups are set on the SingularityRequest

***SingularityRequest Fields***

You can configure access to individual SingularityRequests using the `group`, `readWriteGroups`, and `readOnlyGroups` fields.

- `group` - The primary group for this request. A user in this group is allowed read/write access
- `readWriteGroups` - alternate groups that are also allowed read-write access
- `readOnlyGroups` - alternative groups that are only allowed read access
