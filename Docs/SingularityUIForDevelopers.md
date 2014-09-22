# Build and Run SingularityUI

The Maven build process is configured to automatically build and package the UI into SingularityService. To manually build SingularityUI on its own, you'll need [nodejs](http://nodejs.org/) with [npm](https://www.npmjs.org/). Once these are installed, `cd` into the `SingularityUI` folder and run:

```bash
npm install
```

All the dependencies will be installed and the UI will be built in "production" mode. The compiled static files will be placed in [`../SingularityService/src/main/resources/`](../SingularityService/src/main/resources/).

# Developing SingularityUI

If you want to locally develop on SingularityUI independently of the other Singularity modules, read on.

## Set-up

SingularityUI uses [Brunch](http://brunch.io) to build itself (compile CoffeeScript, etc) and [Bower](http://bower.io) to manage project dependencies. You will want to install these globally for easier access:

```bash
sudo npm install -g brunch bower
```

Some commands you should be aware of:

```bash
# Download dependencies for Brunch & Bower, then compile everything
npm install

# Install dependencies for Bower (these are the application dependencies, e.g. Backbone & jQuery, not things like CoffeeScript)
bower install

# Build the project to development mode by default (with source maps), or minified mode if given --production
brunch build [--production]

# Watch the app files and auto-build when changes happen. If given --server it hosts an HTTP server for you, and -p can be used to specify a port for said server.
brunch watch [--server [-p 3333]]
```

So to start off, `npm install` to install the dependencies, then `brunch watch --server` to get it running. You can now acceess SingularityUI at [localhost:3333/singularity](http://localhost:3333/singularity) by default.

From here you'll need to hook up to an API. You can either use SingularityService running locally, or a remote version like your development cluster. Open up [SingularityUI](http://localhost:3333/singularity) and you'll be prompted for an API root. This is something like `http://example/singularity/api`. To change this in the future, you can use your JS console:

```javascript
localStorage.set('apiRootOverride', 'http://example/singularity/api')
```

Another useful localStorage override you can use is used to disabled the auto-refresh which will stop the page re-rendering so you can properly inspect the DOM:

```javascript
localStorage.setItem('suppressRefresh', true)
```

## Cross-domain restrictions

Your browser will not allow cross-domain requests if you're using a remote server. You have a few options to get around this:

### Cheat using hosts

Open up your hosts file:

* OS X `/private/etc/hosts`
* Linux `/etc/hosts`
* Windows `C:\windows\system32\drivers\etc\hosts`

Let's say you're trying to use `http://apiroot.example/singularity/api` as your API root. You would add a new line to your hosts file saying:

```
127.0.0.1 local.apiroot.example
```

So if you have SingularityUI running on localhost:3333, just point your browser to `http://local.apiroot.example:3333/singularity/api` and you've fooled your browser into having local UI and remote APIs on the same domain.

### Run Chrome with security disabled

Running Google Chome with `--args --disable-web-security` will stop it from preventing your API calls. Be careful not to use your browser normally in this mode as it can cause securiy issues.

```bash
# OS X example
open -a Google\ Chrome\ Canary --args --disable-web-security

# Windows example
```

And you're set! Just open up singularity and your browser won't prevent your API calls.

### Use a local proxy

If you have a local web server running, such as nginx or Apache, then you can add configuration to serve both local UI assets and remote API calls from a single local domain. 

However, if you are not familiar with rolling your own nginx/Apache/other config, you can use [vee](https://github.com/hubspot/vee) (a small, dedicated proxy app). Here is an example `.vee` config:

```yaml
name: SingularityUI

routes:
  # Redirect static assets to local brunch server
  ".*/static/.*": "http://localhost:4000/"

  # Redirect any API calls to the your desired Singularity instance
  ".*/api/.*": "https://<singularity domain/"

  # All else to the index.html
  ".*": "http://localhost:4000/singularity/v2/"

# Uncomment to debug the above routes
# debug: true
```
