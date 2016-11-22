// explicit polyfills for older browsers
import 'core-js/es6';

import React from 'react';
import ReactDOM from 'react-dom';
import FormModal from './components/common/modal/FormModal';
import AppRouter from './router';
import configureStore from 'store';
import { FetchUser } from 'actions/api/auth';
import { FetchGroups } from 'actions/api/requestGroups';
import { AddStarredRequests } from 'actions/api/users';
import Utils from './utils';

// Set up third party configurations
import 'thirdPartyConfigurations';

function setApiRoot(data) {
  if (data.apiRoot) {
    window.localStorage.setItem('apiRootOverride', data.apiRoot);
  }
  return location.reload();
}

function renderApiRootForm() {
  return ReactDOM.render(
    <FormModal
      name="Set API Root"
      action="Set API Root"
      onConfirm={(data) => setApiRoot(data)}
      buttonStyle="primary"
      mustFill={true}
      formElements={[
        {
          name: 'apiRoot',
          type: FormModal.INPUT_TYPES.STRING,
          label: 'API Root URL',
          isRequired: true
        }
      ]}>
      <div id="api-prompt-message">
        <p>
          Hi there! I see you are running the Singularity UI locally.
          You must be trying to use a <strong>remote API</strong>.
        </p>
        <p>
          You need to specify an <strong>API root</strong> so SingularityUI knows where to get its data,
          e.g. <code>http://example/singularity/api</code>.
        </p>
        <p>
          This can be changed at any time in the JS console with <br />
          <code>localStorage.setItem("apiRootOverride", "http://example/singularity/api")</code>
        </p>
      </div>
    </FormModal>, document.getElementById('root')
  ).show();
}

function setUserIdLocal(data) {
  if (data.userId) {
    window.localStorage.setItem('singularityUserId', data.userId);
  }
  return location.reload();
}

function renderUserIdForm() {
  return ReactDOM.render(
    <FormModal
      name="Set User ID"
      action="Set User ID"
      onConfirm={(data) => setUserIdLocal(data)}
      buttonStyle="primary"
      mustFill={true}
      formElements={[
        {
          name: 'userId',
          type: FormModal.INPUT_TYPES.STRING,
          label: 'User ID',
          isRequired: true
        }
      ]}>
      <div id="api-prompt-message">
        <p>
          Hi there! You must be new to Singularity.
          Please set a <strong>User ID</strong>.
        </p>
      </div>
    </FormModal>, document.getElementById('root')
  ).show();
}

function maybeImportStarredRequests(store, userState, userId) {
  const apiStarredRequests = Utils.maybe(userState.data, ['settings', 'starredRequestIds']);
  const locallyStarredRequests = window.localStorage.hasOwnProperty('starredRequests')
    ? JSON.parse(window.localStorage.getItem('starredRequests'))
    : [];
  if (apiStarredRequests && _.isEmpty(locallyStarredRequests)) {
    window.localStorage.removeItem('starredRequests');
    return;
  }

  if (!_.isEmpty(locallyStarredRequests)) {
    store.dispatch(AddStarredRequests.trigger(locallyStarredRequests)).then((response) => {
      if (response.statusCode >= 300 || response.statusCode < 200) return;
      window.localStorage.removeItem('starredRequests');
    });
  }
}

document.addEventListener('DOMContentLoaded', () => {
  if (window.config.apiRoot) {
    // set up Redux store
    const store = configureStore();

    // set up user
    let userId;
    window.app = {};
    window.app.setupUser = () => store.dispatch(FetchUser.trigger());
    window.app.setupUser().then(() => {
      if (!store.getState().api.user.data.user) {
        return renderUserIdForm();
      } else {
        userId = store.getState().api.user.data.user.id
        // Set up starred requests
        maybeImportStarredRequests(store, store.getState().api.user, userId);
      }
    });

    // set up request groups
    store.dispatch(FetchGroups.trigger([404, 500]));

    // Render the page content
    return ReactDOM.render(<AppRouter store={store} />, document.getElementById('root'), () => {
      // hide loading animation
      document.getElementById('static-loader').remove();
    });
  }

  return renderApiRootForm();
});
