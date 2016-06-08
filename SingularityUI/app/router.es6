import DashboardController from 'controllers/Dashboard'
import StatusController from 'controllers/Status'
import RequestFormNewController from 'controllers/RequestFormNew'
import RequestFormEditController from 'controllers/RequestFormEdit'
import NewDeployController from 'controllers/NewDeploy'
import RequestDetailController from 'controllers/RequestDetail'
import RequestsController from 'controllers/Requests'
import TasksTableController from 'controllers/TasksTable'
import TaskDetailController from 'controllers/TaskDetail'
import RacksController from 'controllers/Racks'
import SlavesController from 'controllers/Slaves'
import NotFoundController from 'controllers/NotFound'
import DeployDetailController from 'controllers/DeployDetail'
import LogViewerController from 'controllers/LogViewer'
import TaskSearchController from 'controllers/TaskSearch'
import WebhooksController from 'controllers/Webhooks'
import Utils from './utils'

const hasProp = {}.hasOwnProperty;

class Router extends Backbone.Router {
  constructor(app) {
    super();
    this.app = app;
  }

  dashboard() {
    return this.app.bootstrapController(new DashboardController);
  }

  status() {
    return this.app.bootstrapController(new StatusController);
  }

  newRequest() {
    return this.app.bootstrapController(new RequestFormNewController);
  }

  editRequest(requestId) {
    if (requestId == null) {
      requestId = '';
    }
    return this.app.bootstrapController(new RequestFormEditController({
      requestId
    }));
  }

  requestsTable(state, subFilter, searchFilter) {
    if (state == null) {
      state = 'all';
    }
    if (subFilter == null) {
      subFilter = 'all';
    }
    if (searchFilter == null) {
      searchFilter = '';
    }
    return this.app.bootstrapController(new RequestsController({
      state,
      subFilter,
      searchFilter
    }));
  }

  requestDetail(requestId) {
    return this.app.bootstrapController(new RequestDetailController({
      requestId
    }));
  }

  taskSearch(requestId) {
    return this.app.bootstrapController(new TaskSearchController({
      requestId
    }));
  }

  newDeploy(requestId) {
    return this.app.bootstrapController(new NewDeployController({
      requestId
    }));
  }

  tasksTable(state, requestsSubFilter, searchFilter) {
    if (state == null) {
      state = 'active';
    }
    if (requestsSubFilter == null) {
      requestsSubFilter = 'all';
    }
    if (searchFilter == null) {
      searchFilter = '';
    }
    return this.app.bootstrapController(new TasksTableController({
      state,
      requestsSubFilter,
      searchFilter
    }));
  }

  taskDetail(taskId) {
    return this.app.bootstrapController(new TaskDetailController({
      taskId,
      filePath: null
    }));
  }

  taskFileBrowser(taskId, filePath) {
    if (filePath == null) {
      filePath = "";
    }
    return this.app.bootstrapController(new TaskDetailController({
      taskId,
      filePath
    }));
  }

  tail(taskId, path) {
    let initialOffset, params, requestId, search, splits;
    if (path == null) {
      path = '';
    }
    initialOffset = parseInt(window.location.hash.substr(1), 10) || null;
    splits = taskId.split('-');
    requestId = splits.slice(0, splits.length - 5).join('-');
    params = Utils.getQueryParams();
    search = params.search || '';
    path = path.replace(taskId, '$TASK_ID');
    return this.app.bootstrapController(new LogViewerController({
      requestId,
      path,
      initialOffset,
      taskIds: [taskId],
      search,
      viewMode: 'split'
    }));
  }

  racks(state) {
    if (state == null) {
      state = 'all';
    }
    return this.app.bootstrapController(new RacksController({
      state
    }));
  }

  slaves(state) {
    if (state == null) {
      state = 'all';
    }
    return this.app.bootstrapController(new SlavesController({
      state
    }));
  }

  notFound() {
    return this.app.bootstrapController(new NotFoundController);
  }

  deployDetail(requestId, deployId) {
    return this.app.bootstrapController(new DeployDetailController({
      requestId,
      deployId
    }));
  }

  aggregateTail(requestId, path) {
    let initialOffset, params, search, taskIds, viewMode;
    if (path == null) {
      path = '';
    }
    initialOffset = parseInt(window.location.hash.substr(1), 10) || null;
    params = Utils.getQueryParams();
    if (params.taskIds) {
      taskIds = params.taskIds.split(',');
    } else {
      taskIds = [];
    }
    viewMode = params.viewMode || 'split';
    search = params.search || '';
    return this.app.bootstrapController(new LogViewerController({
      requestId,
      path,
      initialOffset,
      taskIds,
      viewMode,
      search
    }));
  }

  webhooks() {
    return this.app.bootstrapController(new WebhooksController);
  }
}

Router.prototype.routes = {
  '(/)': 'dashboard',
  'status(/)': 'status',
  'requests/new(/)': 'newRequest',
  'requests/edit/:requestId': 'editRequest',
  'requests/:state/:subFilter/:searchFilter(/)': 'requestsTable',
  'requests/:state/:subFilter(/)': 'requestsTable',
  'requests/:state(/)': 'requestsTable',
  'requests(/)': 'requestsTable',
  'request/:requestId(/)': 'requestDetail',
  'request/:requestId/deploy/:deployId(/)': 'deployDetail',
  'request/:requestId/tail/*path': 'aggregateTail',
  'request/:requestId/taskSearch': 'taskSearch',
  'request/:requestId/deploy(/)': 'newDeploy',
  'tasks/:state/:requestsSubFilter/:searchFilter(/)': 'tasksTable',
  'tasks/:state/:requestsSubFilter(/)': 'tasksTable',
  'tasks/:state(/)': 'tasksTable',
  'tasks(/)': 'tasksTable',
  'task/:taskId(/)': 'taskDetail',
  'task/:taskId/files(/)*path': 'taskFileBrowser',
  'task/:taskId/tail/*path': 'tail',
  'taskSearch': 'taskSearch',
  'racks(/)': 'racks',
  'racks/:state(/)': 'racks',
  'slaves/:state(/)': 'slaves',
  'slaves(/)': 'slaves',
  'webhooks(/)': 'webhooks',
  '*anything': 'notFound'
};

export default Router;
