import React from 'react';
import { Provider } from 'react-redux';
import { Router, Route, IndexRoute, useRouterHistory } from 'react-router';
import { createHistory } from 'history';
import { syncHistoryWithStore } from 'react-router-redux';

import Application from './components/common/Application';
import NotFound from './components/common/NotFound';
import DashboardPage from './components/dashboard/DashboardPage';
import StatusPage from './components/status/StatusPage';
import RequestsPage from './components/requests/RequestsPage';
import TasksPage from './components/tasks/TasksPage';
import Racks from './components/machines/Racks';
import Slaves from './components/machines/Slaves';
import Webhooks from './components/webhooks/Webhooks';
import TaskDetail from './components/taskDetail/TaskDetail';
import TaskSearch from './components/taskSearch/TaskSearch';

const AppRouter = (props) => {
  let history = useRouterHistory(createHistory)({
    basename: config.appRoot
  });
  history = syncHistoryWithStore(history, props.store);

  return (
    <Provider store={props.store}>
      <Router history={history}>
        <Route path="/" component={Application}>
          <IndexRoute component={DashboardPage} />
          <Route path="status" component={StatusPage} />
          <Route path="requests(/:state)(/:subFilter)(/:searchFilter)" component={RequestsPage} />
          <Route path="request/:requestId/taskSearch" component={TaskSearch} />
          <Route path="tasks(/:state)(/:requestsSubFilter)(/:searchFilter)" component={TasksPage} />
          <Route path="task/:taskId" component={TaskDetail} store={props.store} />
          <Route path="racks" component={Racks} />
          <Route path="slaves" component={Slaves} />
          <Route path="webhooks" component={Webhooks} />
          <Route path="taskSearch" component={TaskSearch} />
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
