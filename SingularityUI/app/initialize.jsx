// explicit polyfills for older browsers
import 'core-js/es6';

import React from 'react';
import ReactDOM from 'react-dom';
import { AppContainer } from 'react-hot-loader';
import FormModal from './components/common/modal/FormModal';
import AppRouter from './router';
import configureStore from 'store';
import { FetchUser } from 'actions/api/auth';
import { FetchGroups } from 'actions/api/requestGroups';
import { actions as tailerActions } from 'singularityui-tailer';

// Set up third party configurations
import { loadThirdParty } from 'thirdPartyConfigurations';

import './assets/static/images/favicon.ico';

import './styles/index.scss';
import './styles/index.styl';

function setApiRoot(data) {
  if (data.apiRoot) {
    localStorage.setItem('apiRootOverride', data.apiRoot);
  }
  return location.reload();
}

const HMRContainer = (module.hot)
  ? AppContainer
  : ({ children }) => (children);

document.addEventListener('DOMContentLoaded', () => {
  loadThirdParty();

  if (window.config.apiRoot) {
    // set up Redux store
    const store = configureStore();

    store.dispatch(tailerActions.sandboxSetApiRoot(config.apiRoot));

    // set up user
    window.app = {};
    window.app.setupUser = () => store.dispatch(FetchUser.trigger());
    window.app.setupUser();

    // set up request groups
    store.dispatch(FetchGroups.trigger([404, 500]));

    // set up hot module reloading
    if (module.hot) {
      module.hot.accept('./router', () => {
        const NextAppRouter = require('./router').default;
        return ReactDOM.render(<HMRContainer><NextAppRouter store={store} /></HMRContainer>, document.getElementById('root'));
      });
    }

    // Render the page content
    return ReactDOM.render(<HMRContainer><AppRouter store={store} /></HMRContainer>, document.getElementById('root'), () => {
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
          <code>localStorage.setItem("apiRootOverride", "http://example/singularity/api")</code>
        </p>
      </div>
    </FormModal>, document.getElementById('root')).show();
});
