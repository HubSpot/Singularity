import Controller from './Controller';
import TaskView from '../views/task';

import {
  FetchTaskHistory,
  FetchDeployForRequest
} from '../actions/api/history';
import { FetchPendingDeploys } from '../actions/api/deploys';
import {
  FetchTaskCleanups,
  FetchTaskStatistics
} from '../actions/api/tasks';
import { FetchTaskFiles } from '../actions/api/sandbox';
import { FetchTaskS3Logs } from '../actions/api/logs';

class TaskDetail extends Controller {

  initialize({store, taskId, filePath}) {
    app.showPageLoader();
    this.title(taskId);
    this.store = store;
    this.taskId = taskId;
    this.filePath = filePath;

    this.store.dispatch(FetchTaskFiles.trigger(this.taskId, this.filePath)).then(
      this.refresh().then(() => {
        this.setView(new TaskView(store, this.taskId, this.filePath));
        app.showView(this.view);
      })
    );
  }

  refresh() {
    const promises = [];
    const taskPromise = this.store.dispatch(FetchTaskHistory.trigger(this.taskId));
    taskPromise.then(() => {
      const task = this.store.getState().api.task[this.taskId].data;
      promises.push(this.store.dispatch(FetchDeployForRequest.trigger(task.task.taskId.requestId, task.task.taskId.deployId)));
      if (task.isStillRunning) {
        promises.push(this.store.dispatch(FetchTaskStatistics.trigger(this.taskId)));
      }
    });
    promises.push(taskPromise);
    promises.push(this.store.dispatch(FetchTaskCleanups.trigger()));
    promises.push(this.store.dispatch(FetchPendingDeploys.trigger()));
    promises.push(this.store.dispatch(FetchTaskS3Logs.trigger(this.taskId)));
    return Promise.all(promises);
  }
}

export default TaskDetail;
