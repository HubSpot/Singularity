import React from 'react';
import { Router, Route, hashHistory } from 'react-router';

import App from './App';
import Tailer from './Tailer';

const AppRouter = () => (
  <Router history={hashHistory}>
    <Route path="/" component={App}>
      <Route path="/:taskId/tail/*" component={Tailer} />
    </Route>
  </Router>
);

export default AppRouter;
