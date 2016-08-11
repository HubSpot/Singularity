import React from 'react';
import ReactDOM from 'react-dom';
import FormModal from './components/common/modal/FormModal';
import AppRouter from './router';
import configureStore from 'store';
import { FetchUser } from 'actions/api/auth';
import { FetchGroups } from 'actions/api/requestGroups';

// Set up third party configurations
import 'thirdPartyConfigurations';

function setApiRoot(data) {
  if (data.apiRoot) {
    localStorage.setItem('apiRootOverride', data.apiRoot);
  }
  return location.reload();
}

document.addEventListener('DOMContentLoaded', () => {
  if (window.config.apiRoot) {
    // set up Redux store
    const store = configureStore();

    // set up user
    store.dispatch(FetchUser.trigger());

    // set up request groups
    store.dispatch(FetchGroups.trigger());

    // Render the page content
    return ReactDOM.render(<AppRouter store={store} />, document.getElementById('root'), () => {
      // hide loading animation
      document.getElementById('static-loader').remove();
    });
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
