import React from 'react';
import { Router, Route, hashHistory } from 'react-router';

import App from './App';
import Tailer from './Tailer';
import LoadTests from './LoadTests';

const AppRouter = () => (
  <Router history={hashHistory}>
    <Route path="/" component={App}>
      <Route path="/:taskId/tail/*" component={Tailer} />
      <Route path="/test" component={LoadTests} />
    </Route>
  </Router>
);

export default AppRouter;
