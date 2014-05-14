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

Run Chrome or Chrome Canary with web security disabled (to allow cross-domain requests):

```shell
open -a Google\ Chrome\ Canary --args --disable-web-security
```

Run brunch on your preferred port. Example with port `4000`:

```shell
brunch watch --server -p 4000
```

Finally, navigate to: `[http://localhost:4000/singularity](http://localhost:4000/singularity)`.

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
