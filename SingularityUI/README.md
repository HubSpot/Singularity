# SingularityUI

The Singularity UI is a brunch app. To develop locally, first install brunch.

```shell
npm install -g brunch
```

Next, add a local environment file, called `env.coffee` in `Singularity/SingularityUI/app/`, and set a `SINGULARITY_BASE` domain. For example, at HubSpot, we use:

```coffeescript
module.exports =
    env: 'local'
    SINGULARITY_BASE: 'http://example.com'
```

Then npm install `Singularity/SingularityUI/package.json`:

```shell
npm install
```

### Using a remote API while developing locally

If you're using a remote API for your data, run SingularityUI through Brunch on your preferred port. Example with port `4000`:

```shell
brunch watch --server -p 4000
```

Afterwards you will need to specify the API path. Open up [http://localhost:4000/singularity](http://localhost:4000/singularity) and in your JS console type:

```javascript
window.localStorage.singularityApiRoot = "http://example/singularity/api"
```

Your browser will not allow cross-domain requests. To get around this, you can run Chrome or Chrome Canary with web security disabled:

```shell
open -a Google\ Chrome\ Canary --args --disable-web-security
```

And you're set! SingularityUI is now available at [http://localhost:4000/singularity](http://localhost:4000/singularity).

----

Or, instead of running Chrome with web security disabled, you can run a local proxy that forwards requests and makes the browser think that the API and static assets are being served from the same domain.

Any web server should work, but we are using an internal tool called vee with config like so:

```
name: SingularityUI
routes:

  # Redirect static assets to local brunch server
  ".*/static/.*": "http://localhost:4000/"

  # Redirect everything else to our internal QA cluster
  ".*": "https://<QA domain>/"
```

And then you can navigate to: `https://local.<QA domain>/singularity` (assuming you have the necessary host/DNS config to make `local.<QA domain>` point to `127.0.0.1`).

### Developing with SingularityService running locally

No need to jump through any hoops here. Just tell Brunch to watch for changes and auto-compile:

```shell
brunch watch
```

And open up Singularity in your browser as served by SingularityService, eg [http://localhost:7099/singularity](http://localhost:7099/singularity).

Brunch will make sure your static files are always up to date and SingularityService will serve them.
