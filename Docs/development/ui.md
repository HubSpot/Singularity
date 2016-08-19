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

SingularityUI is a single page webapp that relies on SingularityService for its data.

The app is built using Gulp (i.e. compiling ES6, etc), with npm being used to manage its dependencies (e.g. React, Bootstrap).

We recommend you familiarize yourself with the following if you haven't used them before:

* [ES6](http://es6-features.org/#Constants) is the language used for all logic.
* [Stylus](http://learnboost.github.io/stylus/) is used instead of CSS.
* [React](https://facebook.github.io/react/) acts as the front-end framework and templating library.
* [Redux](http://redux.js.org/docs/introduction/) provides state managment.
* [Bootstrap](http://getbootstrap.com/) provides standard responsive components, and [react-bootstrap](https://react-bootstrap.github.io/) provides react versions of said components.

## Set-up

You will need to have the following:

* [nodejs](http://nodejs.org/) with [npm](https://www.npmjs.org/).
* [gulp](http://gulpjs.com/), installable via `npm install --global gulp-cli`.

Below are some commands you might find useful when working on SingularityUI:

```bash
# cd Singularity/SingularityUI
# Install NPM deps
npm install

# Build the app
gulp build

# Serve the app locally at localhost:3334 and rebuild whenever files are changed.
gulp serve
```

When you first start, run `npm install` to download all the dependencies. Once that's done, you're ready to roll!

## Developing locally

So far you have SingularityUI with all its dependencies installed. You're able to run SingularityUI and have it served using `gulp serve`. What we need now is a running SingularityService to pull information from.

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

  # Redirect static assets to local server (assuming it is on port 3334)
  ".*/static/.*": "http://localhost:3334/"

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
* All other requests, including static files, are sent to the gulp server running locally.

### Connecting to the API

So far you have SingularityUI being served by gulp, and SingularityService running somewhere. If you have a proxy like vee running too, please replace the ports/URIs that follow with the ones you're using for the proxy.

Open up SingularityUI in your browser by going to [`http://localhost:3333`](http://localhost:3333).

You'll be prompted to input an API root. This is the service that SingularityUI will interact with for its data. Give it your `http://docker/singularity/api` URI.

You can change the value of this at any point by typing the following in your JS console:

```javascript
localStorage.setItem('apiRootOverride', 'http://docker/singularity/api')
```

And there you go! You should at this point have SingularityUI running in your browser with it connected to SingularityService. Just let gulp watch and compile your files as you work and try it out in your browser.

While we're on the topic of localStorage overrides, another useful one you can use disables the auto-refresh which will stop the page re-rendering so you can properly inspect the DOM:

```javascript
localStorage.setItem('suppressRefresh', true)
```

## Code structure

As mentioned before, SingularityUI uses [React](https://facebook.github.io/react/) and [Redux](http://redux.js.org/docs/introduction/). If you're not familiar with how they do things, please look into them and familiarize yourself with React's lifecycle and the Redux store and dispatch.

What follows is a run-down of how things work in Singularity, using the Webhooks page as an example.

First you request `/singularity/webhooks`. This triggers [our router](../SingularityUI/app/router.jsx) to fire up [`Webhooks`](../SingularityUI/app/components/webhooks/webhooks.jsx).

When the Webhooks component is called, the initial action occurs in the `connect()` function call at the bottom of the page.

First, `connect()` calls `mapStateToProps()`. Though it is called with the redux store and the component's own props, the Webhooks page doesn't have props passed into it. This returns props that are obtained from the redux store, such as API calls.

Then `connect()` calles `mapDispatchToProps()`. This is called with the redux dispatch and the component's own props, and returns props are functions which can perform actions.

`connect()` combines the outputs of `mapStateToProps()` and `mapDispatchToProps()` into one object and passes that in as props to the component the result of `connect()` is called with.

The result of `connect()` is called with the [`rootComponent`](../SingularityUI/app/rootComponent.jsx). `rootComponent` sets up automatically refreshing the page and can display a 404 page if the component sets a `notFound` prop. The passed-in `refresh()` function fetches the page data from the API (in some cases in which initial data needs to not be fetched again, an `initialize()` function is also passed in to perform only initial calls). While the `rootComponent` is waiting for this to finish, the loading animation is displayed.

Finally, once the API call does complete, `rootComponent` takes the props provided by `connect()` and passes them into the Webhooks component itself, which will render the table of webhooks that you see.

Everything else is standard [React](https://facebook.github.io/react/)-structured code. Please refer to the official docs for how to do things like respond to UI events, etc.

To summarize:
* React Router bootstraps everything for the page.
* All API calls necessary for rendering the page are performed in the primary component's `refresh()` or `initialize()` function.
* Use React conventions wherever possible. Try to rely on props, not component state.

### Useful links

There are some libraries/classes in SingularityUI which you should be aware of if you plan on working on it:

* [Initialize](../SingularityUI/app/initialize.jsx) sets up the Redux store, user settings, and the router. It also prompts the user for API root if necessary.
* [Router](../SingularityUI/app/router.jsx) points requests to their respective components.
* [Application](../SingularityUI/app/components/common/Application.jsx) provides the naviagtion header and global search functionality.
* [Base](../SingularityUI/app/actions/api/base.es6) is responsible for API call behavior, including error-handling.
* [Utils](../SingularityUI/app/utils.jsx) contains a bunch of reusable static functions.
* [UITable](../SingularityUI/app/components/common/table/UITable.jsx) is a comprehensive table component that provides sorting, pagination and other utilities. Data is provided by child [Column](../SingularityUI/app/components/common/table/Column.jsx) components.
* [FormModal](../SingularityUI/app/components/common/modal/FormModal.jsx) provides a base for most of Singularity's modals, such as the Run Now and Pause Request modals.
