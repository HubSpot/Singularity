# Singularity Webhook Auth

Singularity contains a few options for authentication andautorization. The most reliable of these is webhook auth. The setup for Singularity's webhook auth is based on the [webhook token auth](https://kubernetes.io/docs/admin/authentication/#webhook-token-authentication) provided in kubernetes.

- [Group Only Auth](#group-only-auth)
- [Groups and Scopes Auth](#ggroups-and-scopes-auth)

### Group Only Auth

When webhook auth is configured, Singularity will look for an `Authorization` header on each api call. Singularity will then make a `GET` call to the configured `webhookAuth.authVerificationUrl` with the same `Authorization` header send. The system expects a response which describes the user as JSON in a format like:

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
  authMode: GROUPS
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
- `adminGroups` - If a user is part of these groups they are allowed admin actions in Singularity (actions on agents, view all requests, etc)
- `requiredGroups` - A user must be part of one at least of these groups in order to access Singularity
- `jitaGroups` - Groups that can be allowed access to any SingularityRequest (but not admin actions)
- `defaultReadOnlyGroups` - If read only groups are not set for a SingularityRequest, these groups are used for read access
- `globalReadOnlyGroups` - These groups are allowed read access to requests regardless of what groups are set on the SingularityRequest

***SingularityRequest Fields***

You can configure access to individual SingularityRequests using the `group`, `readWriteGroups`, and `readOnlyGroups` fields.

- `group` - The primary group for this request. A user in this group is allowed read/write access
- `readWriteGroups` - alternate groups that are also allowed read-write access
- `readOnlyGroups` - alternative groups that are only allowed read access

## Groups and Scopes Auth

A newer update to Singularity auth in version 1.3.0 contains some additional options inside the `auth` section of your config yaml:

```
auth:
  enabled: true
  authMode: GROUPS_SCOPES
```

This enables a more granular mode of checking auth, verifying scopes for groups as well as scopes on users. In order to use this mode of auth, you need a slightly different response format in your webhook auth. You can ue one of two options:

```
auth:
  authResponseParser: RAW
```

In this format, the response from your webhook auth url will conform to the shape of the `SingularityUser` object like:

```
{
  "id": "id",
  "name": "name",
  "email": "user@test.com",
  "groups": ["group1", "group2"],
  "scopes": ["SINGULARITY_READONLY"],
  "authenticated": true
}
```

or with `authResponseParser` set to `WRAPPED` you can conform to the shape of the `SingularityUserPermissionsResponse` object:

```
{
  "user": { # present if no error
    "id": "id",
    "name": "name",
    "email": "user@test.com",
    "groups": ["group1", "group2"],
    "scopes": ["SINGULARITY_READONLY"],
    "authenticated": true
  },
  "error": "" # present if auth failed
}
```

This would grant this user read-only privileges for only requests belonging to the groups group1 and group2.

### Scopes

You can customize the strings used to specify scopes. Defaults are shown below:

```
auth:
  scopes:
    admin:
    - SINGULARITY_ADMIN
    write:
    - SINGULARITY_WRITE
    read:
    - SINGULARITY_READONLY
```

### Default and Global Groups

Several other parameters are also available to allow certain permissions globally to all users:

`defaultReadOnlyGroups` - Users in these groups are allowed read access to a request as long as 1) the user has the readonly scopes and 2) no readOnlyGroups are specified on the request json. readOnlyGroups on the request serve to override the defaultReadOnlyGroups
- `globalReadOnlyGroups` - These groups are allowed readonly access to all requests in any group assuming they have the readonly scope. Useful for things like bots performing automation across all things in Singularity
- `globalReadWriteGroups` - Similar to `globalReadOnlyGroups` but for the read/write permissions and scopes
- `jitaGroups` - These groups will be allowed all access, but any action taken that the user would normally not be able to perform will be logged at WARN level. Note, the user still must have the appropriate scope to perform actions


### Admin Actions

All webhook configurations and actions on agents require admin level credentials


### Example Config

```
auth:
  enabled: true
  authMode: GROUPS_SCOPES
  authResponseParser: RAW
  authenticators:
  - WEBHOOK
  jitaGroups:
  - perm-jita-singularity-rw
  - perm-jita-singularity-ro
  defaultReadOnlyGroups:
  - sgy-read
  globalReadOnlyGroups:
  - perm-singularity-ro
  globalReadWriteGroups:
  - perm-singularity-rw
webhookAuth:
  authVerificationUrl: https://something.com/user/permissions
```

## Token Auth

Singularity also supports token based authentication by adding `TOKEN` to the lis of configured authenticators in `auth.authenticators` in your config yaml.

You can create a token as an admin via an api call to:

`POST {appRoot}/auth/token` with a body like:

```
{
  "token": "new-token",
  "user": {
    "id": "id",
    "name": "name",
    "email": "user@test.com",
    "groups": ["group1", "group2"],
    "scopes": ["SINGULARITY_READONLY"],
    "authenticated": true
  }
}
```

You can then utilize this token by including a header of `Authorization: Token new-token` on each request to the Singularity API.

You can clear all tokens for a given user with a call like `DELETE {appRoot}/auth/{user.name}`

