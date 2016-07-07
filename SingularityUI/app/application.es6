import React from 'react';
import ReactDOM from 'react-dom';
import AppRouter from './router';
import configureStore from 'store';
import { FetchUser } from 'actions/api/auth';
import Utils from './utils';

class Application {
  initialize() {
    this.handleAjaxError = this.handleAjaxError.bind(this);

    // set up Redux store
    this.store = configureStore();

    // set up user
    this.store.dispatch(FetchUser.trigger());

    // hide loading animation
    $('.page-loader.fixed').hide();

    // Render the page content
    ReactDOM.render(<AppRouter store={this.store} />, document.getElementById('root'));
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
      this.currentController.refresh();
    }
  }

  caughtError() {
    this.caughtThisError = true;
  }

  handleAjaxError(e, jqxhr, settings) {
    let id;
    let options;
    let selector;
    let serverMessage;

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
    const url = settings.url.replace(config.appRoot, '');
    if (jqxhr.status === 502) {
      window.Messenger().info({
        message: 'Singularity is deploying, your requests cannot be handled. Things should resolve in a few seconds so just hang tight!'
      });
    } else if (jqxhr.status === 401 && config.redirectOnUnauthorizedUrl) {
      window.location.href = config.redirectOnUnauthorizedUrl.replace('{URL}', encodeURIComponent(window.location.href));
    } else if (jqxhr.statusText === 'timeout') {
      window.Messenger().error({
        message: `<p>A <code>${jqxhr.statusText}</code> error occurred while accessing:</p><pre>${url}</pre>`
      });
    } else if (jqxhr.status === 0) {
      window.Messenger().error({
        message: '<p>Could not reach the Singularity API. Please make sure SingularityUI is properly set up.</p><p>If running through locally, this might be your browser blocking cross-domain requests.</p>'
      });
    } else {
      try {
        serverMessage = JSON.parse(jqxhr.responseText).message || jqxhr.responseText;
      } catch (error) {
        if (jqxhr.status === 200) {
          console.error(jqxhr.responseText);
          window.Messenger().error({
            message: `<p>Expected JSON but received ${jqxhr.responseText.startsWith('<!DOCTYPE html>') ? 'html' : 'something else'}. The response has been saved to your js console.</p>`
          });
          throw new Error(`Expected JSON in response but received ${jqxhr.responseText.startsWith('<!DOCTYPE html>') ? 'html' : 'something else'}`);
        }
        serverMessage = jqxhr.responseText;
      }
      serverMessage = _.escape(serverMessage);
      id = `message_${Date.now()}`;
      selector = `#${id}`;
      window.Messenger().error({
        message: `<div id="${id}"><p>An uncaught error occurred with your request. The server said:</p><pre class="copy-text">${serverMessage}</pre><p>The error has been saved to your JS console. <span class='copy-link'>Copy error message</span>.</p></div>`
      });
      console.error(jqxhr);
      options = {
        selector,
        linkText: 'Copy error message',
        copyLink: '.copy-link'
      };
      Utils.makeMeCopy(options);
      throw new Error('AJAX Error');
    }
  }
}

export default new Application;
