let Application;
let GlobalSearchView;
let NavView;
let Sortable;
let User;
const bind = (fn, me) => function(){ return fn.apply(me, arguments); };

import Router from 'router';

User = require('models/User');

NavView = require('views/nav');

GlobalSearchView = require('views/globalSearch')["default"];

Sortable = require('sortable');

Application = ((() => {
  class Application {
    constructor() {
      this.getUsername = bind(this.getUsername, this);
      this.globalRefresh = bind(this.globalRefresh, this);
    }

    initialize() {
      let $body, el;
      this.setupGlobalErrorHandling();
      this.setupUser();
      this.$page = $('#page');
      this.page = this.$page[0];
      $body = $('body');
      this.views.nav = new NavView;
      this.views.nav.render();
      $body.prepend(this.views.nav.$el);
      this.views.globalSearch = new GlobalSearchView();
      this.views.globalSearch.render();
      $body.append(this.views.globalSearch.$el);
      $('.page-loader.fixed').hide();
      this.router = new Router(this);
      el = document.createElement('a');
      el.href = config.appRoot || '/';
      Backbone.history.start({
        pushState: true,
        root: el.pathname
      });
      this.setRefreshInterval();
      $(window).on('blur', ((_this => () => {
        _this.blurred = true;
        return clearInterval(_this.globalRefreshInterval);
      }))(this));
      return $(window).on('focus', ((_this => () => {
        _this.blurred = false;
        _this.globalRefresh();
        return _this.setRefreshInterval();
      }))(this));
    }

    setRefreshInterval() {
      clearInterval(this.globalRefreshInterval);
      return setInterval(this.globalRefresh, this.globalRefreshTime);
    }

    globalRefresh() {
      if (localStorage.getItem('suppressRefresh') === 'true') {
        return;
      }
      if (this.blurred) {
        clearInterval(this.globalRefreshInterval);
        return;
      }
      return this.currentController.refresh();
    }

    caughtError() {
      return this.caughtThisError = true;
    }

    setupGlobalErrorHandling() {
      let unloading;
      unloading = false;
      $(window).on('beforeunload', () => {
        unloading = true;
      });
      return $(document).on('ajaxError', ((_this => (e, jqxhr, settings) => {
        let error, id, options, selector, serverMessage, url;
        if (_this.caughtThisError) {
          _this.caughtThisError = false;
          return;
        }
        if (settings.suppressErrors) {
          return;
        }
        if (jqxhr.statusText === 'abort') {
          return;
        }
        if (unloading) {
          return;
        }
        if (_this.blurred && jqxhr.statusText === 'timeout') {
          return;
        }
        url = settings.url.replace(config.appRoot, '');
        if (jqxhr.status === 502) {
          return Messenger().info({
            message: "Singularity is deploying, your requests cannot be handled. Things should resolve in a few seconds so just hang tight!"
          });
        } else if (jqxhr.status === 401 && config.redirectOnUnauthorizedUrl) {
          return window.location.href = config.redirectOnUnauthorizedUrl.replace('{URL}', encodeURIComponent(window.location.href));
        } else if (jqxhr.statusText === 'timeout') {
          return Messenger().error({
            message: `<p>A <code>${jqxhr.statusText}</code> error occurred while accessing:</p><pre>${url}</pre>`
          });
        } else if (jqxhr.status === 0) {
          return Messenger().error({
            message: "<p>Could not reach the Singularity API. Please make sure SingularityUI is properly set up.</p><p>If running through locally, this might be your browser blocking cross-domain requests.</p>"
          });
        } else {
          try {
            serverMessage = JSON.parse(jqxhr.responseText).message || jqxhr.responseText;
          } catch (error) {
            if (jqxhr.status === 200) {
              console.error(jqxhr.responseText);
              Messenger().error({
                message: `<p>Expected JSON but received ${jqxhr.responseText.startsWith('<!DOCTYPE html>') ? 'html' : 'something else'}. The response has been saved to your js console.</p>`
              });
              throw new Error(`Expected JSON in response but received ${jqxhr.responseText.startsWith('<!DOCTYPE html>') ? 'html' : 'something else'}`);
            }
            serverMessage = jqxhr.responseText;
          }
          serverMessage = _.escape(serverMessage);
          id = `message_${Date.now()}`;
          selector = `#${id}`;
          Messenger().error({
            message: `<div id="${id}"><p>An uncaught error occurred with your request. The server said:</p><pre class="copy-text">${serverMessage}</pre><p>The error has been saved to your JS console. <span class='copy-link'>Copy error message</span>.</p></div>`
          });
          console.error(jqxhr);
          options = {
            selector,
            linkText: 'Copy error message',
            copyLink: '.copy-link'
          };
          utils.makeMeCopy(options);
          throw new Error("AJAX Error");
        }
      }))(this));
    }

    showPageLoader() {
      return this.$page.html("<div class='page-loader centered cushy'></div>");
    }

    showFixedPageLoader() {
      return this.$page.append("<div class='page-loader page-loader-fixed'></div>");
    }

    hideFixedPageLoader() {
      return this.$page.find('.page-loader-fixed').remove();
    }

    bootstrapController(controller) {
      return this.currentController = controller;
    }

    showView(view) {
      window.dispatchEvent(new Event('viewChange'));
      if (this.views.current) {
        this.views.current.remove();
      }
      $(window).scrollTop(0);
      this.views.current = view;
      view.render();
      if (this.page.children.length) {
        this.page.replaceChild(view.el, this.page.children[0]);
        return Sortable.init();
      } else {
        return this.page.appendChild(view.el);
      }
    }

    setupUser() {
      this.user = new User;
      return this.user.fetch();
    }

    getUsername() {
      if (this.user.get('authenticated')) {
        return this.user.get('user').id;
      } else {
        return '';
      }
    }
  }

  Application.prototype.views = {};

  Application.prototype.globalRefreshInterval = void 0;

  Application.prototype.globalRefreshTime = 60000;

  Application.prototype.blurred = false;

  return Application;
}))();

export default new Application;
