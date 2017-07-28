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
import SlaveUsage from './components/machines/usage/Utilization';
import Webhooks from './components/webhooks/Webhooks';
import TaskDetail from './components/taskDetail/TaskDetail';
import TaskSearch from './components/taskSearch/TaskSearch';
import DeployDetail from './components/deployDetail/DeployDetail';
import RequestForm from './components/requestForm/RequestForm';
import NewDeployForm from './components/newDeployForm/NewDeployForm';
import TaskInstanceRedirect from './components/requestDetail/TaskInstanceRedirect';
import RequestDetailPage from './components/requestDetail/RequestDetailPage';
import Group from './components/groupDetail/GroupDetail.jsx';
import Disasters from './components/disasters/Disasters';
import TaskLogTailerContainer from './containers/TaskLogTailerContainer';
import RequestLogTailerContainer from './containers/RequestLogTailerContainer';
import CustomLogTailerContainer from './containers/CustomLogTailerContainer';
import { Tail, AggregateTail } from './components/logs/Tail';

const getFilenameFromSplat = (splat) => _.last(splat.split('/'));

const routes = (
  <Route path="/" component={Application}>
    <IndexRoute component={DashboardPage} title="Dashboard" />
    <Route path="status" component={StatusPage} title="Status" />
    <Route path="requests/new" component={RequestForm} title="New Request" />
    <Route path="requests/edit/:requestId" component={RequestForm} title="New or Edit Request" />
    <Route path="requests(/:state)(/:subFilter)(/:searchFilter)" component={RequestsPage} title="Requests" />
    <Route path="group/:groupId" component={Group} title={(params) => `Group ${params.groupId}`} />
    <Route path="request">
      <Route path=":requestId" component={RequestDetailPage} title={(params) => params.requestId} />
      <Route path=":requestId/task-search" component={TaskSearch} title="Task Search" />
      <Route path=":requestId/deploy" component={NewDeployForm} title="New Deploy" />
      <Route path=":requestId/deploy/:deployId" component={DeployDetail} title={(params) => `Deploy ${params.deployId}`} />
      <Route path=":requestId/old-tail/**" component={AggregateTail} title={(params) => `Tail of ${getFilenameFromSplat(params.splat)}`} />
      <Route path=":requestId/tail/**" component={RequestLogTailerContainer} title={(params) => `Tail of ${getFilenameFromSplat(params.splat)}`} />
      <Route path=":requestId/instance/:instanceNo" component={TaskInstanceRedirect} />
      <IndexRoute component={NotFound} title="Not Found" />
    </Route>
    <Route path="tasks(/:state)(/:requestsSubFilter)(/:searchFilter)" component={TasksPage} title="Tasks" />
    <Route path="task">
      <Route path=":taskId(/files**)" component={TaskDetail} title={(params) => params.taskId} />
      <Route path=":taskId/old-tail/**" component={Tail} title={(params) => `Tail of ${getFilenameFromSplat(params.splat)}`} />
      <Route path=":taskId/tail/**" component={TaskLogTailerContainer} title={(params) => `Tail of ${getFilenameFromSplat(params.splat)}`} />
      <IndexRoute component={NotFound} title="Not Found" />
    </Route>
    <Route path="tail/**" component={CustomLogTailerContainer} title="Tailer" />
    <Route path="racks(/:state)" component={Racks} title="Racks" />
    <Route path="slaves(/:state)" component={Slaves} title="Slaves" />
    <Route path="utilization" component={SlaveUsage} title="Utilization" />
    <Route path="webhooks" component={Webhooks} title="Webhooks" />
    <Route path="task-search" component={TaskSearch} title="Task Search" />
    <Route path="disasters" component={Disasters} title="Disasters" />
    <Route path="*" component={NotFound} title="Not Found" />
  </Route>);

const AppRouter = (props) => {
  const syncedHistory = syncHistoryWithStore(props.history, props.store);

  return (
    <Provider store={props.store}>
      <Router history={syncedHistory} routes={routes} />
    </Provider>
  );
};

AppRouter.propTypes = {
  store: React.PropTypes.object.isRequired
};

export default AppRouter;
