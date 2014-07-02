# SingularityUI

The Singularity UI is a brunch app. To develop locally, first install brunch.

```shell
npm install -g brunch
```

Then npm install the dependencies:

```shell
npm install
```

## Using a local API

If you have SingularityService running locally, it will serve all the static files itself. Tell Brunch to watch for changes and auto-compile:

```shell
brunch watch
```

And open up Singularity in your browser, e.g. [http://localhost:7099/singularity](http://localhost:7099/singularity) with default config.

## Using a remote API

If you're using a remote API for your data, run SingularityUI through Brunch on your preferred port. Example with port `4000`:

```shell
brunch watch --server -p 4000
```

Open up SingularityUI in your browser by going to [http://localhost:4000/singularity](http://localhost:4000/singularity). It will prompt you for an API root. Give it the base URL of the API you want to use, e.g. `http://example/singularity/api`.

Later on if you want to change the API root manually, open up your browser's JS console and type:

```javascript
localStorage.set("apiRootOverride", "http://example/singularity/api")
```

----

Your browser will not allow cross-domain requests. You have 2 options to get around this:

### Run Chrome with security disabled

```shell
open -a Google\ Chrome\ Canary --args --disable-web-security
```

And you're set! SingularityUI is now available at [http://localhost:4000/singularity](http://localhost:4000/singularity) and it will use whatever API you specified.

### Use a proxy

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
