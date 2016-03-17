# SingularityUI

This document is intended for people who want to work on SingularityUI independently of the other Singularity modules.

If you're here just looking for information on how to build SingularityUI, please note that the Maven build process is configured to automatically build and package the UI into SingularityService.

The compiled static files are placed in `../SingularityService/target/generated-resources/static/`.

## Contents

* [Developer overview](#developer-overview)
* [Set-up](#set-up)
* [Developing locally](#developing-locally)
  * [Cross-domain restrictions](#cross-domain-restrictions)
  * [Connecting to the API](#connecting-to-the-api)
* [Code structure](#code-structure)
  * [Useful links](#useful-links)

## Developer overview

SingularityUI is a static app that relies on SingularityService for its data.

The app is built using Brunch (i.e. compiling CoffeeScript, etc), with Bower being used to manage its dependencies (e.g. jQuery, Backbone).

We recommend you familiarise yourself with the following if you haven't used them before:

* [CoffeeScript](http://coffeescript.org/) is the language used for all logic.
* [Stylus](http://learnboost.github.io/stylus/) is used instead of CSS.
* [Handlebars](http://handlebarsjs.com/) is the templating library.
* [Backbone](http://backbonejs.org/) acts as the front-end framework.

## Set-up

You will need to have the following:

* [nodejs](http://nodejs.org/) with [npm](https://www.npmjs.org/).
* [Brunch](http://brunch.io) & [Bower](http://bower.io), installable via `npm install -g brunch bower`.

Below are some commands you might find useful when working on SingularityUI:

```bash
# cd Singularity/SingularityUI
# Install NPM deps, Bower deps, and do one production build
npm install

# Install just the Bower dependencies
bower install

# Remove dependencies (reinstall them using 'npm install')
rm -rf node_modules bower_components

# Build SingularityUI. '--production' (optional) optimises the output files
brunch build [--production]

# Watch the project and build it when there are changes
brunch watch

# Same as above, but also start an HTTP server that serves the static files. '-P <number>' (optional) specifies what port it runs on
brunch watch --server [-P 3333]
# NOTE - As of February 3, 2016, this has changed - it used to be lowercase p to specify port number, now it's capital P. 
```

When you first start, run `npm install` to download all the dependencies. Once that's done, you're ready to roll!

## Developing locally

So far you have SingularityUI with all its dependencies installed. You're able to run SingularityUI and have it served using `brunch watch --server`. What we need now is a running SingularityService to pull information from.

If you don't have one already (e.g. your team might be running one you can use), you can easily run your own via [Docker](developing-with-docker.md). If running via docker, it is helpful to add the host that docker is running on to your `/etc/hosts` file as `docker` so we can reference it by hostname. If using `boot2docker` this is your `boot2docker ip`. We will reference the hostname as `docker` in the examples below.


Once the cluster is up and running, the API's root is available at [`http://docker/singularity/api`](http://docker/singularity/api) by default.

You might also be running SingularityService locally without a VM. This works too!

-----

From this point onwards we're assuming you have a running SingularityService you can use. We'll be using `http://docker/singularity/api` as a placeholder for the root URL of the API you're using.

### Cross-domain restrictions

**If you're using the docker-compose setup for your API you can go ahead and skip this section.**

If you're using a different API you should be aware that SingularityService has an `enableCorsFilter` option in its server config. If enabled, cross-domain restrictions won't apply. The docker service has this enabled by default which is why we're not worried about it.

Your browser will not allow cross-domain requests if you're using a server without the CORS filter enabled. To get around this we'll use an open-source HubSpot mini-proxy called [vee](https://github.com/HubSpot/vee).

If you're able to configure your own nginx or Apache server to be used as a proxy, feel free to do it that way if you wish. Otherwise, read on!

Go ahead and install vee by running `npm install -g vee`.

Afterwards you will need to create a `.vee` file which will act as the configuration. Place this inside of `/SingularityUI`. Here is an example `.vee` file you can use:

```yaml
name: SingularityUI

routes:

  # Redirect static assets to local brunch server (assuming it is on port 3333)
  ".*/static/.*": "http://localhost:3333/"

  # Redirect any API calls to the QA Singularity service (the slash after the domain is necessary)
  ".*/api/.*": "http://docker/"
  ".*/login/.*": "http://docker/"

  # All else to the index.html (for all other Backbone routes)
  ".*": "http://localhost:3333/singularity/"

# Uncomment to debug the above routes
# debug: true
```

Once you have the file up and running, go ahead and run vee (from the dir `.vee` is in):

```bash
# Run vee on ports 80 and 443
sudo vee

# Run vee on specific ports
vee -p 4001 -s 4002
```

Assuming you used the second command, you can now access SingularityUI by going to [`http://localhost:4001/singularity`](http://localhost:4001/singularity).

If you're confused as to what's going on here, all your requests are being processed by vee so that:

* Requests to `localhost:4001/singularity/api` are sent to the server at `docker`.
* All other requests, including static files, are sent to the Brunch server running locally.

### Connecting to the API

So far you have SingularityUI being served by Brunch, and SingularityService running somewhere. If you have a proxy like vee running too, please replace the ports/URIs that follow with the ones you're using for the proxy.

Open up SingularityUI in your browser by going to [`http://localhost:3333`](http://localhost:3333).

You'll be prompted to input an API root. This is the service that SingularityUI will interact with for its data. Give it your `http://docker/singularity/api` URI.

You can change the value of this at any point by typing the following in your JS console:

```javascript
localStorage.setItem('apiRootOverride', 'http://docker/singularity/api')
```

And there you go! You should at this point have SingularityUI running in your browser with it connected to SingularityService. Just let Brunch watch and compile your files as you work and try it out in your browser.

While we're on the topic of localStorage overrides, another useful one you can use disables the auto-refresh which will stop the page re-rendering so you can properly inspect the DOM:

```javascript
localStorage.setItem('suppressRefresh', true)
```

## Code structure

As mentioned before, SingularityUI uses [Backbone](http://backbonejs.org/). If you're not familiar with how it does things, please look into it and familiarise yourself with Views, Models, Collections, and the event-based interaction between them all.

What follows is a run-down of how things work in Singularity, using the Slaves page as an example.

First you request `/singularity/slaves`. This triggers [our router](../SingularityUI/app/router.coffee) to fire up [`SlavesController`](../SingularityUI/app/controllers/Slaves.coffee).

The controller bootstraps the things we need for the requested page. First, it creates 3 collections--one for each API endpoint we're going to hit.

Afterwards, it creates 3 instances of [`SimpleSubview`](../SingularityUI/app/views/simpleSubview.coffee) and gives each one a template to render and a collection to use for data.

`SimpleSubview` is a reusable class that renders its template in response to change events from the collection you gave it. For the slaves page, when one of the collections receives a response from the service `SimpleSubview` renders the required table and nothing more, therefore giving it ownership over one component of the page, with the collection telling it when to render it.

The controller also creates a [`SlavesView`](../SingularityUI/app/views/slaves.coffee) and assigns it as its primary view using `Controller.setView(View)`. This view is special because it represents the entire page being rendered. We feed it with references to our sub-views so that it can embed them into itself.

Finally, we tell the app to render this main view of ours and to start all of the collection fetches, which will eventually trigger the subview renders when completed.

Everything else is standard [Backbone](http://backbonejs.org/)-structured code. Please refer to the official docs for how to do things like respond to UI events, etc.

To summarise:
* A controller bootstraps everything (collections, models, views) for a page.
* If there is more than one collection/model involved, we split the view up into subviews in order to keep things modular and easy to change/render. A primary view glues everything together.
* If there is one/no collection/model being used, we just use the primary view for everything.
* Use Backbone conventions wherever possible. Try to rely on events, not callbacks.

### Useful links

There are some libraries/classes in SingularityUI which you should be aware of if you plan on working on it:

* [Application](../SingularityUI/app/application.coffee) is responsible for a lot of global behaviour, including error-handling.
* [Router](../SingularityUI/app/router.coffee) points requests to their respective controllers.
* [Utils](../SingularityUI/app/utils.coffee) contains a bunch of reusable static functions.
* The base classes. The various components extend the following:
  * [Model](../SingularityUI/app/models/model.coffee)
  * [Collection](../SingularityUI/app/collections/collection.coffee) & [PaginableCollection](../SingularityUI/app/collections/PaginableCollection.coffee)
  * [View](../SingularityUI/app/views/view.coffee)
  * [Controller](../SingularityUI/app/controllers/Controller.coffee)
* Reusable components:
  * [SimpleSubview](../SingularityUI/app/views/simpleSubview.coffee)
  * [ExpandableTableSubview](../SingularityUI/app/views/expandableTableSubview.coffee)
