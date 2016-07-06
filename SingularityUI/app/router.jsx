import React from 'react';
import { Provider } from 'react-redux';
import { Router, Route, IndexRoute, useRouterHistory } from 'react-router';
import { createHistory } from 'history';
import { syncHistoryWithStore } from 'react-router-redux';

import Navigation from './components/common/Navigation';
import NotFound from './components/common/NotFound';

const getRouter = (store) => {
  let history = useRouterHistory(createHistory)({
    basename: config.appRoot
  });
  history = syncHistoryWithStore(history, store);

  return (
    <Provider store={store}>
      <Router history={history}>
        <Route path="/" component={Navigation}>
          <IndexRoute component={Navigation} />
          <Route path="*" component={NotFound} />
        </Route>
      </Router>
    </Provider>
  );
};

export default getRouter;
