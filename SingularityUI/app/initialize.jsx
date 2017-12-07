import Raven from 'raven-js';

if (window.config.sentryDsn) {
  Raven.config(window.config.sentryDsn).install();
}

// explicit polyfills for older browsers
import 'core-js/es6';

import React from 'react';
import ReactDOM from 'react-dom';
import FormModal from './components/common/modal/FormModal';
import AppRouter from './router';
import configureStore from 'store';
import { FetchUser } from 'actions/api/auth';
import { FetchGroups } from 'actions/api/requestGroups';
import { FetchUtilization } from 'actions/api/utilization';
import { FetchSingularityStatus } from 'actions/api/state';
import { actions as tailerActions } from 'singularityui-tailer';
import { AddStarredRequests } from 'actions/api/users';
import Utils from './utils';
import parseurl from 'parseurl';
import { useRouterHistory } from 'react-router';
import { createHistory } from 'history';

// Set up third party configurations
import  { loadThirdParty } from 'thirdPartyConfigurations';

import './assets/static/images/favicon.ico';
import './assets/static/images/icons/icon-on_demand.svg';
import './assets/static/images/icons/icon-run_once.svg';
import './assets/static/images/icons/icon-scheduled.svg';
import './assets/static/images/icons/icon-service.svg';
import './assets/static/images/icons/icon-worker.svg';

import './styles/index.scss';
import './styles/index.styl';

function setApiRoot(data) {
  if (data.apiRoot) {
    window.localStorage.setItem('apiRootOverride', data.apiRoot);
  }
  return location.reload();
}

const HMRContainer = (module.hot)
  ? require('react-hot-loader').AppContainer
  : ({ children }) => (children);

document.addEventListener(tailerActions.SINGULARITY_TAILER_AJAX_ERROR_EVENT, (event) => {
  if (event.detail.response.status === 401 && window.config.redirectOnUnauthorizedUrl) {
    window.location.href = config.redirectOnUnauthorizedUrl.replace('{URL}', encodeURIComponent(window.location.href));
  }
});

document.addEventListener('DOMContentLoaded', () => {
  loadThirdParty();

  if (window.config.apiRoot) {
    // set up Redux store
    const parsedUrl = parseurl({ url: config.appRoot });
    const history = useRouterHistory(createHistory)({
      basename: parsedUrl.path
    });

    const store = configureStore({}, history);

    store.dispatch(tailerActions.sandboxSetApiRoot(config.apiRoot));
    if (config.generateAuthHeader) {
      store.dispatch(tailerActions.setAuthorizationHeader(Utils.getAuthTokenHeader()));
    }

    // set up user
    let userId;
    window.app = {};
    window.app.setupUser = () => store.dispatch(FetchUser.trigger());
    window.app.setupUser().then(() => {
      if (!store.getState().api.user.data.user) {
        return renderUserIdForm();
      } else {
        if (window.config.sentryDsn) {
          Raven.setUserContext({ email: store.getState().api.user.data.user.email });
        }
        userId = store.getState().api.user.data.user.id
        // Set up starred requests
        maybeImportStarredRequests(store, store.getState().api.user, userId);
      }
    });

    const globalRefresh = () => {
      // set up request groups
      store.dispatch(FetchGroups.trigger([404, 500]));

      // set up cluster utilization
      store.dispatch(FetchUtilization.trigger([404, 500]));

      // set up state
      store.dispatch(FetchSingularityStatus.trigger());
    };

    globalRefresh();
    setInterval(globalRefresh, config.globalRefreshInterval);

    // set up hot module reloading
    if (module.hot) {
      module.hot.accept('./router', () => {
        const NextAppRouter = require('./router').default;
        return ReactDOM.render(<HMRContainer><NextAppRouter history={history} store={store} /></HMRContainer>, document.getElementById('root'));
      });
    }

    // Render the page content
    return ReactDOM.render(<HMRContainer><AppRouter history={history} store={store} /></HMRContainer>, document.getElementById('root'), () => {
      // hide loading animation
      document.getElementById('static-loader').remove();
    });
  }

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
});

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
