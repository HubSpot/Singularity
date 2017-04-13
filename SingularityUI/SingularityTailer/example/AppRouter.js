import React from 'react';
import { Router, Route, browserHistory } from 'react-router';

import App from './App';
import SandboxTailerPage from './SandboxTailerPage';
import LoadTests from './LoadTests';

const AppRouter = () => (
  <Router history={browserHistory}>
    <Route path="/" component={App}>
      <Route path="/test" component={LoadTests} />
      <Route path="/:taskId/tail/*" component={SandboxTailerPage} />
    </Route>
  </Router>
);

export default AppRouter;
