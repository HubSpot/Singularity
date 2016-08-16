import React from 'react';
import ReactDOM from 'react-dom';
import FormModal from './components/common/modal/FormModal';
import AppRouter from './router';
import configureStore from 'store';
import { FetchUser, FetchUserSettings, UpdateUserSettings } from 'actions/api/user';
import { FetchGroups } from 'actions/api/requestGroups';
import Utils from './utils';

// Set up third party configurations
import 'thirdPartyConfigurations';

function setApiRoot(data) {
  if (data.apiRoot) {
    localStorage.setItem('apiRootOverride', data.apiRoot);
  }
  return location.reload();
}

function maybeImportStars(store, fetchUserSettingsApiResponse, userId) {
  if (fetchUserSettingsApiResponse.statusCode !== 200) return;
  const locallyStarredRequests = window.localStorage.hasOwnProperty('starredRequests')
    ? JSON.parse(window.localStorage.getItem('starredRequests'))
    : [];
  const apiStarredRequests = Utils.maybe(fetchUserSettingsApiResponse.data, 'starredRequestIds') || [];
  if (_.isEmpty(_.difference(locallyStarredRequests, apiStarredRequests))) return;
  const newSettings = Utils.changeUserSetting(
    fetchUserSettingsApiResponse.data,
    'starredRequestIds',
    _.union(locallyStarredRequests, apiStarredRequests)
  );
  store.dispatch(UpdateUserSettings.trigger(userId, newSettings));
}

document.addEventListener('DOMContentLoaded', () => {
  if (window.config.apiRoot) {
    // set up Redux store
    const store = configureStore();

    // set up user
    window.app = {};
    window.app.setupUser = () => store.dispatch(FetchUser.trigger()).then(response => {
      const userId = Utils.maybe(response.data, ['user', 'id']);
      if (response.statusCode === 200 && userId) {
        store.dispatch(FetchUserSettings.trigger(userId)).then(
          fetchUserSettingsApiResponse => maybeImportStars(store, fetchUserSettingsApiResponse, userId)
        );
      }
    });
    window.app.setupUser();

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
      name="Set API Root"
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
