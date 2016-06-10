import Router from 'router';

import configureStore from 'store';
import { FetchAction } from 'actions/api/user';

import NavView from 'views/nav';
import GlobalSearchView from 'views/globalSearch';

import Sortable from 'sortable';


class Application {
  initialize() {
    this.globalRefresh = this.globalRefresh.bind(this);
    this.handleBlur = this.handleBlur.bind(this);
    this.handleFocus = this.handleFocus.bind(this);
    this.handleAjaxError = this.handleAjaxError.bind(this);

    // set up Redux store
    this.store = configureStore();

    // set up user
    this.store.dispatch(FetchAction.trigger());

    // set up ajax error handling
    this.unloading = false;
    $(window).on('beforeunload', () => {
      this.unloading = true;
    });
    $(document).on('ajaxError', this.handleAjaxError);
    
    this.page = $('#page')[0];

    // wire up nav
    this.views = {};
    this.views.nav = new NavView;
    this.views.nav.render();
    $('body').prepend(this.views.nav.$el);

    // wire up global search
    this.views.globalSearch = new GlobalSearchView();
    this.views.globalSearch.render();
    $('body').append(this.views.globalSearch.$el);

    // hide loading animation
    $('.page-loader.fixed').hide();

    // set up router
    this.router = new Router(this);
    
    // set up Backbone history
    Backbone.history.start({
      pushState: true,
      root: this.getRootPath()
    });

    // set up global refresh
    this.blurred = false;
    this.setRefreshInterval();
    $(window).on('blur', this.handleBlur);
    $(window).on('focus', this.handleFocus);
  }

  handleBlur() {
    this.blurred = true;
    clearInterval(this.globalRefreshInterval);
  }

  handleFocus() {
    this.blurred = false;
    this.globalRefresh();
    this.setRefreshInterval();
  }

  setRefreshInterval() {
    clearInterval(this.globalRefreshInterval);
    setInterval(this.globalRefresh, this.globalRefreshTime);
  }

  globalRefresh() {
    if (localStorage.getItem('suppressRefresh') === 'true') {
      return;
    }

    if (this.blurred) {
      clearInterval(this.globalRefreshInterval);
    } else {
      return this.currentController.refresh();
    }
  }

  caughtError() {
    this.caughtThisError = true;
  }

  handleAjaxError(e, jqxhr, settings) {
    let error, id, options, selector, serverMessage, url;
    if (this.caughtThisError) {
      this.caughtThisError = false;
      return;
    }
    if (settings.suppressErrors) {
      return;
    }
    if (jqxhr.statusText === 'abort') {
      return;
    }
    if (this.unloading) {
      return;
    }
    if (this.blurred && jqxhr.statusText === 'timeout') {
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
  }

  showPageLoader() {
    return $('#page').html("<div class='page-loader centered cushy'></div>");
  }

  showFixedPageLoader() {
    return $('#page').append("<div class='page-loader page-loader-fixed'></div>");
  }

  hideFixedPageLoader() {
    return $('#page').find('.page-loader-fixed').remove();
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

  getUsername() {
    const state = this.store.getState();

    if (state.api.user.receivedAt && state.api.user.data) {
      return state.api.user.data.user.id;
    } else {
      return '';
    }
  }

  getRootPath() {
    const el = document.createElement('a');
    el.href = config.appRoot || '/';
    return el.pathname;
  }
}

Application.prototype.globalRefreshInterval = void 0;

Application.prototype.globalRefreshTime = 60000;

export default new Application;
