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

import React from 'react';
import ReactDOM from 'react-dom';
import FormModal from './components/common/modal/FormModal';

// Set up third party configurations
import 'thirdPartyConfigurations';

function setApiRoot(data) {
  if (data.apiRoot) {
    localStorage.setItem('apiRootOverride', data.apiRoot);
  }
  window.location = window.location.href;
  return window.location;
}

document.addEventListener('DOMContentLoaded', () => {
  if (window.config.apiRoot) {
    return window.app.initialize();
  }
  return ReactDOM.render(
    <FormModal
      action="Set API Root"
      onConfirm={(data) => setApiRoot(data)}
      buttonStyle="primary"
      mustFill={true}
      formElements={[
        {
          name: 'apiRoot',
          type: FormModal.INPUT_TYPES.URL,
          label: 'API Root URL',
          isRequired: true
        }
      ]}>
      <div id="api-root-prompt-message">
        <p>
          Hi there! I see you're running the Singularity UI locally.
          You must be trying to use a <strong>remote API</strong>.
        </p>
        <p>
          You need to specify an <strong>API root</strong> so SingularityUI knows where to get its data,
          e.g. <code>http://example/singularity/api</code>.
        </p>
        <p>
          This can be changed at any time in the JS console with <br />
          <code>localStorage.setItem(&quot;apiRootOverride&quot;, &quot;http://example/singularity/api&quot;)</code>
        </p>
      </div>
    </FormModal>, document.getElementById('root')).show();
});
