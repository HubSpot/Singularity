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
import DeployDetail from './components/deployDetail/DeployDetail';
import RequestForm from './components/requestForm/RequestForm';
import NewDeployForm from './components/newDeployForm/NewDeployForm';
import { Tail, AggregateTail } from './components/logs/Tail';
import RequestDetailPage from './components/requestDetail/RequestDetailPage';
import Group from './components/groupDetail/GroupDetail.jsx';
import DisabledActions from './components/disabledActions/DisabledActions';

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
          <Route path="requests/new" component={RequestForm} />
          <Route path="requests/edit/:requestId" component={RequestForm} />
          <Route path="requests(/:state)(/:subFilter)(/:searchFilter)" component={RequestsPage} />
          <Route path="group/:groupId" component={Group} />
          <Route path="request">
            <Route path=":requestId" component={RequestDetailPage} />
            <Route path=":requestId/task-search" component={TaskSearch} />
            <Route path=":requestId/deploy" component={NewDeployForm} />
            <Route path=":requestId/deploy/:deployId" component={DeployDetail} store={props.store} />
            <Route path=":requestId/tail/**" component={AggregateTail} />
          </Route>
          <Route path="tasks(/:state)(/:requestsSubFilter)(/:searchFilter)" component={TasksPage} />
          <Route path="task">
            <Route path=":taskId(/files/**)" component={TaskDetail} store={props.store} />
            <Route path=":taskId/tail/**" component={Tail} />
          </Route>
          <Route path="racks(/:state)" component={Racks} />
          <Route path="slaves(/:state)" component={Slaves} />
          <Route path="webhooks" component={Webhooks} />
          <Route path="task-search" component={TaskSearch} />
          <Route path="disabled-actions" component={DisabledActions} />
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
