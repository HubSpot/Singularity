import Controller from './Controller';
import TaskView from '../views/task';
import { fetchTask as TaskFetchAction, clear as TaskFetchActionClear} from '../actions/api/task';
import { FetchAction as DeployFetchAction} from '../actions/api/deploy';
import { FetchAction as DeploysFetchAction} from '../actions/api/deploys';
import { FetchAction as TaskCleanupsFetchAction } from '../actions/api/taskCleanups';
import { FetchAction as TaskFilesFetchAction } from '../actions/api/taskFiles';
import { FetchAction as TaskResourceUsageFetchAction } from '../actions/api/taskResourceUsage';
import { FetchAction as TaskS3LogsFetchAction } from '../actions/api/taskS3Logs';

class TaskDetail extends Controller {

  initialize({store, taskId, filePath}) {
    app.showPageLoader()
    this.title(taskId);
    this.store = store;
    this.taskId = taskId;
    this.filePath = filePath;

    this.store.dispatch(TaskFetchActionClear());
    this.store.dispatch(TaskFilesFetchAction.trigger(this.taskId, this.filePath));
    this.refresh().then(() => {
      this.setView(new TaskView(store, this.taskId, this.filePath));
      app.showView(this.view);
    });
  }

  refresh() {
    let promises = [];
    let taskPromise = this.store.dispatch(TaskFetchAction(this.taskId));
    taskPromise.then(() => {
      let task = this.store.getState().api.task[this.taskId].data;
      promises.push(this.store.dispatch(DeployFetchAction.trigger(task.task.taskId.requestId, task.task.taskId.deployId)));
      if (task.isStillRunning) {
        promises.push(this.store.dispatch(TaskResourceUsageFetchAction.trigger(this.taskId)));
      }
    });
    promises.push(taskPromise);
    promises.push(this.store.dispatch(TaskCleanupsFetchAction.trigger()));
    promises.push(this.store.dispatch(DeploysFetchAction.trigger('pending')));
    promises.push(this.store.dispatch(TaskS3LogsFetchAction.trigger(this.taskId)));
    return Promise.all(promises);
  }
}

export default TaskDetail;
