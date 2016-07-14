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

// Set up third party configurations
import 'thirdPartyConfigurations';

import React from 'react';
import ReactDOM from 'react-dom';

import ApiRootOverride from './components/common/ApiRootOverride';

document.addEventListener('DOMContentLoaded', () => {
  if (window.config.apiRoot) {
    return window.app.initialize();
  }
  // In the event that the apiRoot isn't set (running locally)
  // prompt the user for it and refresh
  return ReactDOM.render(<ApiRootOverride />, document.getElementById('page'));
});
