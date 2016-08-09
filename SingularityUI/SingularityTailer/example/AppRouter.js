import React from 'react';
import { Router, Route, hashHistory } from 'react-router';

import App from './App';
import SandboxTailerPage from './SandboxTailerPage';
import HttpTailerPage from './HttpTailerPage';
import LoadTests from './LoadTests';

const AppRouter = () => (
  <Router history={hashHistory}>
    <Route path="/" component={App}>
      <Route path="/:taskId/tail/*" component={SandboxTailerPage} />
      <Route path="/http/*" component={HttpTailerPage} />
      <Route path="/test" component={LoadTests} />
    </Route>
  </Router>
);

export default AppRouter;
