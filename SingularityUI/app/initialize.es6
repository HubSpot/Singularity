// Object.assign polyfill
require('es6-object-assign').polyfill();

// Promise polyfill
window.Promise = require('promise-polyfill');

// Set up the only app globals
window.utils = require('utils').default;

import app from 'application';

window.app = app;

import Messenger from 'messenger'; // eslint-disable-line no-unused-vars

import 'bootstrap';

import vex from 'vex.dialog';

import apiRootPromptTemplate from './templates/vex/apiRootPrompt';

// Set up third party configurations
import 'thirdPartyConfigurations';
// Set up the Handlebars helpers
import 'handlebarsHelpers';

// Initialize the app on DOMContentReady
$(() => {
  if (window.config.apiRoot) {
    return window.app.initialize();
  }
  // In the event that the apiRoot isn't set (running locally)
  // prompt the user for it and refresh
  return vex.dialog.prompt({
    message: apiRootPromptTemplate(),
    callback: value => {
      if (value) {
        localStorage.setItem('apiRootOverride', value);
      }
      window.location = window.location.href;
      return window.location;
    }
  });
});
