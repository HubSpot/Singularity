import React from 'react';
import { Router, Route, hashHistory } from 'react-router';

import App from './App';
import Tailer from './Tailer';
import LoadTests from './LoadTests';
import HttpTailer from './HttpTailer';

const AppRouter = () => (
  <Router history={hashHistory}>
    <Route path="/" component={App}>
      <Route path="/:taskId/tail/*" component={Tailer} />
      <Route path="/test" component={LoadTests} />
      <Route path="/http/*" component={HttpTailer} />
    </Route>
  </Router>
);

export default AppRouter;
