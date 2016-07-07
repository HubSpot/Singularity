import React from 'react';
import { Provider } from 'react-redux';
import { Router, Route, IndexRoute, useRouterHistory } from 'react-router';
import { createHistory } from 'history';
import { syncHistoryWithStore } from 'react-router-redux';

import Application from './components/common/Application';
import NotFound from './components/common/NotFound';
import StatusPage from './components/status/StatusPage';

const AppRouter = (props) => {
  let history = useRouterHistory(createHistory)({
    basename: config.appRoot
  });
  history = syncHistoryWithStore(history, props.store);

  return (
    <Provider store={props.store}>
      <Router history={history}>
        <Route path="/" component={Application}>
          <IndexRoute component={NotFound} />
          <Route path="status" component={StatusPage} />
          <Route path="*" component={NotFound} />
        </Route>
      </Router>
    </Provider>
  );
};

AppRouter.propTypes = {
  store: React.PropTypes.object.isRequired
};

export default AppRouter;
